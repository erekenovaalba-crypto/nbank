package comparison;

import comparison.ModelComparisonConfig.FieldMapping;
import comparison.ModelComparisonConfig.Rule;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Класс 2: сравнивает объекты запроса и ответа по полям и правилам из конфига.
// Возвращает список расхождений, пустой список означает, что модели соответствуют правилам.
public final class ModelComparator {
    private ModelComparator() {}

    public static List<String> compare(Object request, Object response) {
        Rule rule = ModelComparisonConfig.getRule(request.getClass(), response.getClass());
        List<String> mismatches = new ArrayList<>();

        for (FieldMapping mapping : rule.fields()) {
            Object requestValue = readField(request, mapping.requestField());
            Object responseValue = readField(response, mapping.responseField());
            boolean equal = areEqual(requestValue, responseValue);
            String description = request.getClass().getSimpleName() + "." + mapping.requestField()
                    + " -> " + response.getClass().getSimpleName() + "." + mapping.responseField();

            if (mapping.mustMatch() && !equal) {
                mismatches.add(description + ": ожидалось <" + requestValue + ">, получено <" + responseValue + ">");
            }
            // значение не выводим: для проверок "не должно совпадать" это обычно пароль или другая чувствительная часть
            if (!mapping.mustMatch() && equal) {
                mismatches.add(description + ": значения должны отличаться, но совпадают");
            }
        }
        return mismatches;
    }

    // числа сравниваем по значению, чтобы 5000 и 5000.00 считались равными
    private static boolean areEqual(Object expected, Object actual) {
        if (expected instanceof Number expectedNumber && actual instanceof Number actualNumber) {
            return new BigDecimal(expectedNumber.toString())
                    .compareTo(new BigDecimal(actualNumber.toString())) == 0;
        }
        return Objects.equals(expected, actual);
    }

    // читаем поле напрямую, без геттеров; ищем и в родительских классах
    private static Object readField(Object object, String fieldName) {
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                // поле может быть в родительском классе
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Не удалось прочитать поле " + fieldName, e);
            }
        }
        throw new IllegalArgumentException("В классе " + object.getClass().getSimpleName()
                + " нет поля " + fieldName + " (проверьте model-comparison.properties)");
    }
}
