package iteration_1;

import models.CreateAccountResponse;
import models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        // Arrange
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Act
        CreateAccountResponse accountResponse = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CREATE_ACCOUNT,
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        // Assert
        softly.assertThat(accountResponse.getId())
                .as("У созданного счёта должен быть положительный id")
                .isPositive();
        softly.assertThat(accountResponse.getAccountNumber())
                .as("Номер счёта должен быть заполнен")
                .isNotBlank();
        softly.assertThat(accountResponse.getBalance())
                .as("Новый счёт должен иметь нулевой баланс")
                .isEqualByComparingTo("0.00");
        softly.assertThat(accountResponse.getTransactions())
                .as("У нового счёта не должно быть транзакций")
                .isEmpty();
    }
}
