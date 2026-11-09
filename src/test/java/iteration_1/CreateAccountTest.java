package iteration_1;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.LoginUserRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

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
       new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
              ResponseSpecs.entityWasCreated())
              .post(null);

    }
}
