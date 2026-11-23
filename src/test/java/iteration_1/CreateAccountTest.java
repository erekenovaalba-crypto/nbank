package iteration_1;

import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.CustomerProfile;
import org.junit.jupiter.api.Test;
import requests.GetProfileRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        // Arrange
        CreateUserRequest userRequest = AdminSteps.createUser();
        var userSpec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());

        // Act
        CreateAccountResponse createdAccount = new CrudRequester(
                userSpec,
                Endpoint.CREATE_ACCOUNT,
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        // Assert: POST response
        softly.assertThat(createdAccount.getId()).isPositive();
        softly.assertThat(createdAccount.getAccountNumber()).isNotBlank();
        softly.assertThat(createdAccount.getBalance()).isEqualByComparingTo("0.00");
        softly.assertThat(createdAccount.getTransactions()).isEmpty();

        // Assert: GET profile confirms the account was persisted for this user
        CustomerProfile profile = new GetProfileRequester(userSpec, ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CustomerProfile.class);

        softly.assertThat(profile.getAccounts())
                .as("Созданный счёт должен присутствовать в профиле пользователя")
                .anySatisfy(account -> {
                    softly.assertThat(account.getId()).isEqualTo(createdAccount.getId());
                    softly.assertThat(account.getAccountNumber()).isEqualTo(createdAccount.getAccountNumber());
                    softly.assertThat(account.getBalance()).isEqualByComparingTo("0.00");
                    softly.assertThat(account.getTransactions()).isEmpty();
                });
    }
}
