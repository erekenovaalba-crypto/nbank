package models.comparison;

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
//   КлассЗапроса=КлассОтвета:поле=поле,поле;ДругойКлассОтвета:поле=поле
//
//   поле=поле       значения должны совпасть (имена в запросе и ответе могут отличаться)
//   поле            то же самое, если поле называется одинаково
//   поле!=поле      значения должны отличаться (например, пароль в ответе не должен быть открытым)
//   customer.name   вложенное поле через точку
//   Класс#вариант   несколько наборов правил для одной пары классов,
//                   выбирается так: assertThatModels(a, b).usingRule("вариант").match()
//   ;               разделяет правила для разных классов ответа
public final class ModelComparisonConfig {
    private static final String CONFIG_FILE = "model-comparison.properties";
    private static final String NOT_EQUAL = "!=";
    private static final String EQUAL = "=";
    private static final String KEY_SEPARATOR = "->";
    private static final String RESPONSE_SEPARATOR = ";";
    private static final String FIELDS_SEPARATOR = ",";
    private static final char CLASS_FIELDS_SEPARATOR = ':';
    private static final String VARIANT_MARK = "#";

    // Одно сравнение полей: поле запроса -> поле ответа
    public record FieldMapping(String requestField, String responseField, boolean mustMatch) {}

    // Правило для пары классов: запрос -> ответ
    public record Rule(String requestClass, String responseClass, List<FieldMapping> fields) {}

    // ключ: "КлассЗапроса->КлассОтвета" или "КлассЗапроса->КлассОтвета#вариант"
    private static final Map<String, Rule> RULES = load();

    private ModelComparisonConfig() {}

    public static Rule getRule(Class<?> requestClass, Class<?> responseClass, String variant) {
        String key = key(requestClass.getSimpleName(), responseClass.getSimpleName(), variant);
        Rule rule = RULES.get(key);
        if (rule == null) {
            throw new IllegalStateException("В " + CONFIG_FILE + " нет правила для " + key
                    + ". Добавьте строку: " + requestClass.getSimpleName() + "=" + responseClass.getSimpleName()
                    + (variant == null ? "" : VARIANT_MARK + variant) + ":поле=поле");
        }
        return rule;
    }

    private static String key(String requestClass, String responseClass, String variant) {
        return requestClass + KEY_SEPARATOR + responseClass
                + (variant == null || variant.isBlank() ? "" : VARIANT_MARK + variant);
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
                for (String segment : properties.getProperty(requestClass).split(RESPONSE_SEPARATOR)) {
                    if (segment.isBlank()) {
                        continue;
                    }
                    Rule rule = parseRule(requestClass.trim(), segment.trim());
                    // responseClass может содержать "#вариант"
                    rules.put(requestClass.trim() + KEY_SEPARATOR + rule.responseClass(), rule);
                }
            }
            return rules;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать " + CONFIG_FILE, e);
        }
    }

    private static Rule parseRule(String requestClass, String value) {
        int colon = value.indexOf(CLASS_FIELDS_SEPARATOR);
        if (colon < 0) {
            throw new IllegalStateException("Неверный формат правила для " + requestClass
                    + ": ожидается ИмяОтвета:поле=поле,..., а указано " + value);
        }
        String responseClass = value.substring(0, colon).trim();
        List<FieldMapping> fields = Arrays.stream(value.substring(colon + 1).split(FIELDS_SEPARATOR))
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
