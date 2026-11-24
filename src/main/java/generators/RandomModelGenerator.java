package generators;

//import com.github.curiousoddman.rgxgen.RgxGen;
import org.apache.commons.lang3.RandomStringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Создаёт объект модели и заполняет его поля случайными значениями по типу поля.
// Если над полем стоит @GeneratingRule, значение генерируется по её регулярному выражению.
public final class RandomModelGenerator {
    private static final int MAX_DEPTH = 5;
    private static final int STRING_LENGTH = 10;
    private static final int MAX_LIST_SIZE = 3;

    private RandomModelGenerator() {}

    public static <T> T generate(Class<T> modelClass) {
        return generate(modelClass, 0);
    }

    private static <T> T generate(Class<T> modelClass, int depth) {
        T instance = newInstance(modelClass);
        // идём по классу и его родителям, чтобы заполнить унаследованные поля
        for (Class<?> type = modelClass; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isFinal(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }
                setField(instance, field, generateFieldValue(field, depth));
            }
        }
        return instance;
    }

    private static Object generateFieldValue(Field field, int depth) {
        GeneratingRule rule = field.getAnnotation(GeneratingRule.class);
        if (rule != null) {
          //  String generated = RgxGen.parse(rule.regex()).generate();
          //  return convert(generated, field);
        }
        return randomValue(field.getType(), field.getGenericType(), depth);
    }

    // значение по типу данных
    private static Object randomValue(Class<?> type, Type genericType, int depth) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (type == String.class) {
            return RandomStringUtils.randomAlphabetic(STRING_LENGTH);
        }
        if (type == int.class || type == Integer.class) {
            return random.nextInt(1, 1000);
        }
        if (type == long.class || type == Long.class) {
            return random.nextLong(1, 1000);
        }
        if (type == double.class || type == Double.class) {
            return random.nextLong(1, 100_000) / 100.0;
        }
        if (type == float.class || type == Float.class) {
            return random.nextInt(1, 100_000) / 100.0f;
        }
        if (type == BigDecimal.class) {
            return BigDecimal.valueOf(random.nextLong(1, 100_000), 2);
        }
        if (type == boolean.class || type == Boolean.class) {
            return random.nextBoolean();
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants[random.nextInt(constants.length)];
        }
        if (List.class.isAssignableFrom(type)) {
            return randomList(genericType, depth);
        }
        // вложенная модель
        if (depth >= MAX_DEPTH) {
            return null;
        }
        return generate(type, depth + 1);
    }

    private static List<Object> randomList(Type genericType, int depth) {
        if (!(genericType instanceof ParameterizedType parameterizedType)
                || !(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> elementType)) {
            throw new IllegalArgumentException("Не удалось определить тип элементов списка: " + genericType);
        }
        int size = ThreadLocalRandom.current().nextInt(1, MAX_LIST_SIZE + 1);
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(randomValue(elementType, elementType, depth));
        }
        return list;
    }

    // строка, сгенерированная по регулярному выражению, приводится к типу поля
    private static Object convert(String value, Field field) {
        Class<?> type = field.getType();
        if (type == String.class) {
            return value;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.valueOf(value);
        }
        if (type == long.class || type == Long.class) {
            return Long.valueOf(value);
        }
        if (type == double.class || type == Double.class) {
            return Double.valueOf(value);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(value);
        }
        if (type.isEnum()) {
            return enumValue(type, value);
        }
        throw new IllegalArgumentException("@GeneratingRule не поддерживается для поля "
                + field.getName() + " типа " + type.getSimpleName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String value) {
        return Enum.valueOf((Class<? extends Enum>) type, value);
    }

    private static <T> T newInstance(Class<T> modelClass) {
        try {
            var constructor = modelClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("У класса " + modelClass.getName()
                    + " должен быть конструктор без аргументов (например, @NoArgsConstructor)", e);
        }
    }

    private static void setField(Object instance, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Не удалось записать поле " + field.getName(), e);
        }
    }
}