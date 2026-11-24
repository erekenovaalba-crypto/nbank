
package models.comparison;

import org.assertj.core.api.AbstractAssert;

import java.util.List;

// Класс 3: AssertJ-проверка соответствия ответа запросу.
// Использование: ModelAssertions.assertThatModels(request, response).match();
public class ModelAssertions extends AbstractAssert<ModelAssertions, Object> {
    private final Object response;
    private String variant;

    private ModelAssertions(Object request, Object response) {
        super(request, ModelAssertions.class);
        this.response = response;
    }

    public static ModelAssertions assertThatModels(Object request, Object response) {
        return new ModelAssertions(request, response);
    }

    // выбрать набор правил с суффиксом "#имя" из model-comparison.properties
    public ModelAssertions usingRule(String variant) {
        this.variant = variant;
        return this;
    }

    // Все расхождения собираются и выводятся одной ошибкой.
    public ModelAssertions match() {
        isNotNull();
        if (response == null) {
            failWithMessage("Ответ не должен быть null");
        }

        List<String> mismatches = ModelComparator.compare(actual, response, variant);
        if (!mismatches.isEmpty()) {
            // "%s", чтобы символы % в значениях не принимались за формат
            failWithMessage("%s", "Модели не совпадают:\n" + String.join("\n", mismatches));
        }
        return this;
    }
}
