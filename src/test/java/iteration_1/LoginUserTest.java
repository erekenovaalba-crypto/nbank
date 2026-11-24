package iteration_1;

import io.restassured.specification.RequestSpecification;
import models.*;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

public class LoginUserTest extends BaseTest {

    //войти админом
    @Test
    public void adminCanGenerateAuthTokenTest() {
        String token = UserSteps.login(AuthConstants.ADMIN_USERNAME, AuthConstants.ADMIN_PASSWORD);

        softly.assertThat(token).as("Токен админа").isNotBlank();

        // токен должен реально давать доступ: админский запрос с ним проходит (статус 200 проверяет спецификация)
        AdminSteps.getUsers(UserSteps.specWithToken(token));
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        String token = UserSteps.login(userRequest);

        softly.assertThat(token).as("Токен пользователя").isNotBlank();

        // токен должен давать доступ к данным именно этого пользователя: проверяем через GET профиля
        RequestSpecification tokenSpec = UserSteps.specWithToken(token);
        softly.assertThat(UserSteps.getProfile(tokenSpec).getUsername())
                .as("Профиль, полученный по токену, должен принадлежать залогиненному пользователю")
                .isEqualTo(userRequest.getUsername());
    }
}
