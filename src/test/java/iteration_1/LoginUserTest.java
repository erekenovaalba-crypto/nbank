package iteration_1;

import io.restassured.response.Response;
import models.CreateUserRequest;
import models.CustomerProfile;
import models.LoginUserRequest;
import models.LoginUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.GetProfileRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import static org.hamcrest.Matchers.notNullValue;

public class LoginUserTest extends BaseTest {

    private Response login(String username, String password) {
        return new CrudRequester(
                RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(username).password(password).build())
                .header("Authorization", notNullValue())
                .extract().response();
    }

    private CustomerProfile getProfileUsingToken(String token) {
        return new GetProfileRequester(
                RequestSpecs.unauthenticSpec().header("Authorization", token),
                ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CustomerProfile.class);
    }

    @Test
    public void adminCanGenerateAuthTokenTest() {
        Response loginResponse = login("admin", "admin");
        LoginUserResponse response = loginResponse.as(LoginUserResponse.class);

        softly.assertThat(response.getUsername()).isEqualTo("admin");
        softly.assertThat(response.getRole()).isEqualTo(UserRole.ADMIN.name());

        CustomerProfile profile = getProfileUsingToken(loginResponse.getHeader("Authorization"));
        softly.assertThat(profile.getUsername()).isEqualTo("admin");
        softly.assertThat(profile.getRole()).isEqualTo(UserRole.ADMIN.name());
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        // Arrange
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Act
        Response loginResponse = login(userRequest.getUsername(), userRequest.getPassword());
        LoginUserResponse response = loginResponse.as(LoginUserResponse.class);

        // Assert: login response
        softly.assertThat(response.getUsername()).isEqualTo(userRequest.getUsername());
        softly.assertThat(response.getRole()).isEqualTo(UserRole.USER.name());

        // Assert: GET with the token returned by this login request
        CustomerProfile profile = getProfileUsingToken(loginResponse.getHeader("Authorization"));
        softly.assertThat(profile.getUsername()).isEqualTo(userRequest.getUsername());
        softly.assertThat(profile.getRole()).isEqualTo(UserRole.USER.name());
    }
}
