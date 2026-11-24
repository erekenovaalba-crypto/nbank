package iteration_1;

import comparison.ModelAssertions;
import models.*;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        CreateAccountResponse createdAccount = UserSteps.createAccount(userRequest);

        softly.assertThat(createdAccount.getId()).as("Проверка, что id положительный").isPositive();
        softly.assertThat(createdAccount.getAccountNumber()).as("Проверка, что не null").isNotNull();
        softly.assertThat(createdAccount.getBalance()).as("Проверка, что баланс равен 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

        // GET профиля
        CustomerProfile profile = UserSteps.getProfile(userRequest);
        Optional<CreateAccountResponse> savedAccount = UserSteps.findAccount(profile, createdAccount.getId());
        assertThat(savedAccount)
                .as("Созданный счёт должен быть в профиле пользователя")
                .isPresent();
        ModelAssertions.assertThatModels(createdAccount, savedAccount.get()).match();
        softly.assertThat(profile.getAccounts()).as("У нового пользователя должен быть ровно один счёт")
                .hasSize(1);
    }
}
