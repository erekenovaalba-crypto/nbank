package iteration_1;

import models.CreateUserRequest;
import models.LoginUserRequest;
import models.LoginUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class LoginUserTest extends BaseTest {

    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();

        LoginUserResponse response = new CrudRequester(
                RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(loginRequest)
                .header("Authorization", notNullValue())
                .extract()
                .as(LoginUserResponse.class);

        softly.assertThat(response.getUsername()).isEqualTo("admin");
        softly.assertThat(response.getRole()).isEqualTo(UserRole.ADMIN.name());
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        // Arrange
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Act
        LoginUserResponse response = new CrudRequester(
                RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .header("Authorization", notNullValue())
                .extract()
                .as(LoginUserResponse.class);

        // Assert
        softly.assertThat(response.getUsername()).isEqualTo(userRequest.getUsername());
        softly.assertThat(response.getRole()).isEqualTo(UserRole.USER.name());
    }
}
