package iteration_1;

import generators.RandomData;
import models.AuthConstants;
import models.CreateUserRequest;
import models.CustomerProfile;
import models.LoginUserRequest;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.AdminGetUsersRequester;
import requests.GetProfileRequester;
import requests.LoginUserRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginUserTest {

    //войти админом
    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest userRequest = LoginUserRequest.builder()
                .username(AuthConstants.ADMIN_USERNAME)
                .password(AuthConstants.ADMIN_PASSWORD)
                .build();
        String token = new LoginUserRequester(RequestSpecs.unauthSpec(), ResponseSpecs.requestResponseOK())
                .post(userRequest)
                .header(AuthConstants.HEADER_AUTHORIZATION, Matchers.notNullValue())
                .extract().header(AuthConstants.HEADER_AUTHORIZATION);

        // GET
        new AdminGetUsersRequester(RequestSpecs.authWithToken(token), ResponseSpecs.requestResponseOK())
                .get();
    }


    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();
        //создание пользователя
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);
        //логин
        String token = new LoginUserRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .header(AuthConstants.HEADER_AUTHORIZATION,Matchers.notNullValue())
                .extract().header(AuthConstants.HEADER_AUTHORIZATION);

        // GET
        CustomerProfile profile = new GetProfileRequester(RequestSpecs.authWithToken(token), ResponseSpecs.requestResponseOK())
                .get().extract().as(CustomerProfile.class);
        assertThat(profile.getUsername())
                .as("Профиль, полученный по токену, должен принадлежать залогиненному пользователю")
                .isEqualTo(userRequest.getUsername());
    }
}