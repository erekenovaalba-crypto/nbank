package iteration_1;

import generators.RandomData;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.LoginUserRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import static io.restassured.RestAssured.given;

public class LoginUserTest {

    //войти админом
    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest userRequest = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();
        new LoginUserRequester(RequestSpecs.unauthSpec(), ResponseSpecs.requestResponseOK())
                .post(userRequest);
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
        new LoginUserRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .header("Authorization",Matchers.notNullValue());

    }
}
