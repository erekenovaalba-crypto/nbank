package iteration_1;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.LoginUserRequest;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import requests.steps.AdminSteps;
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
        new ValidatedCrudRequesters<CreateUserResponse>(RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(userRequest);
    }


  @Test
    public void userCanGenerateAuthTokenTest() {
      CreateUserRequest userRequest = AdminSteps.createUser();
        //логин
        new CrudRequester(RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .header("Authorization",Matchers.notNullValue());

    }

}
