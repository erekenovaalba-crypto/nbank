package iteration_1;

import generators.RandomData;
import io.restassured.http.ContentType;
import models.*;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.LoginUserRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;

public class CreateAccountTest extends BaseTest{
  @Test
  public  void userCanCreateAccountTest(){
      CreateUserRequest userRequest = CreateUserRequest.builder()
              .username(RandomData.getUsername())
              .password(RandomData.getPassword())
              .role(UserRole.USER.toString())
              .build();
      LoginUserRequest loginUserRequest = LoginUserRequest.builder()
              .username(userRequest.getUsername())
              .password(userRequest.getPassword())
              .build();
      //создание пользователя
      new AdminCreateUserRequester(
              RequestSpecs.adminSpec(),
              ResponseSpecs.entityWasCreated())
              .post(userRequest);
  //получаем токен юзера
        String userAuthHeader = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestResponseOK())
                .post(loginUserRequest)
                .extract()
                .header("Authorization");


        //создаем аккаунт
      models.CreateAccountResponse createAccountResponse =  new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
              ResponseSpecs.entityWasCreated())
              .post(null).extract().as(CreateAccountResponse.class);
      softly.assertThat(createAccountResponse.getId()).as("Проверка, что не null")
                      .isNotNull();
      softly.assertThat(createAccountResponse.getAccountNumber()).as("Проверка, что не null")
              .isNotNull();
      softly.assertThat(createAccountResponse.getBalance()).as("Проверка, что равна 0")
              .isEqualTo(new BigDecimal("0"));

    }
}
