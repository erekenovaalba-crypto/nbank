package comparison;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Класс 1: читает правила сравнения из model-comparison.properties.
//
// Формат строки:
//   ИмяКлассаЗапроса=ИмяКлассаОтвета:поле=поле,поле=поле,поле!=поле
//
// Пример:
//   CreateUserRequest=CreateUserResponse:username=username,role=role,password!=password
//
//   поле=поле   значения должны совпасть (имена полей в запросе и ответе могут отличаться)
//   поле        то же самое, если поле называется одинаково в обоих классах
//   поле!=поле  значения должны отличаться (например, пароль в ответе не должен быть открытым)
public final class ModelComparisonConfig {
    private static final String CONFIG_FILE = "model-comparison.properties";
    private static final String NOT_EQUAL = "!=";
    private static final String EQUAL = "=";

    // Одно сравнение полей: поле запроса -> поле ответа
    public record FieldMapping(String requestField, String responseField, boolean mustMatch) {}

    // Правило для пары классов: запрос -> ответ
    public record Rule(String requestClass, String responseClass, List<FieldMapping> fields) {}

    // ключ: простое имя класса запроса
    private static final Map<String, Rule> RULES = load();

    private ModelComparisonConfig() {}

    public static Rule getRule(Class<?> requestClass, Class<?> responseClass) {
        Rule rule = RULES.get(requestClass.getSimpleName());
        if (rule == null) {
            throw new IllegalStateException("В " + CONFIG_FILE + " нет правила для класса "
                    + requestClass.getSimpleName());
        }
        if (!rule.responseClass().equals(responseClass.getSimpleName())) {
            throw new IllegalStateException("В " + CONFIG_FILE + " для " + requestClass.getSimpleName()
                    + " указан ответ " + rule.responseClass() + ", а передан " + responseClass.getSimpleName());
        }
        return rule;
    }

    private static Map<String, Rule> load() {
        try (InputStream stream = ModelComparisonConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                throw new IllegalStateException("Не найден файл " + CONFIG_FILE
                        + " (он должен лежать в resources)");
            }
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            Map<String, Rule> rules = new HashMap<>();
            for (String requestClass : properties.stringPropertyNames()) {
                rules.put(requestClass.trim(), parseRule(requestClass.trim(), properties.getProperty(requestClass)));
            }
            return rules;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать " + CONFIG_FILE, e);
        }
    }

    private static Rule parseRule(String requestClass, String value) {
        int colon = value.indexOf(':');
        if (colon < 0) {
            throw new IllegalStateException("Неверный формат правила для " + requestClass
                    + ": ожидается ИмяОтвета:поле=поле,..., а указано " + value);
        }
        String responseClass = value.substring(0, colon).trim();
        List<FieldMapping> fields = Arrays.stream(value.substring(colon + 1).split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(ModelComparisonConfig::parseField)
                .toList();
        return new Rule(requestClass, responseClass, fields);
    }

    private static FieldMapping parseField(String token) {
        // "!=" проверяем первым, иначе он будет принят за обычное "="
        if (token.contains(NOT_EQUAL)) {
            String[] parts = token.split(NOT_EQUAL, 2);
            return new FieldMapping(parts[0].trim(), parts[1].trim(), false);
        }
        if (token.contains(EQUAL)) {
            String[] parts = token.split(EQUAL, 2);
            return new FieldMapping(parts[0].trim(), parts[1].trim(), true);
        }
        return new FieldMapping(token, token, true);
    }
}
