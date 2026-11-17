package iteration_1;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;

public class CreateAccountTest extends BaseTest{
    @Test
    public  void userCanCreateAccountTest(){
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();
        //создание пользователя
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);
        //получаем токен юзера
        //создаем аккаунт
        models.CreateAccountResponse createAccountResponse =  new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post().extract().as(CreateAccountResponse.class);
        softly.assertThat(createAccountResponse.getId()).as("Проверка, что не null")
                .isNotNull();
        softly.assertThat(createAccountResponse.getAccountNumber()).as("Проверка, что не null")
                .isNotNull();
        softly.assertThat(createAccountResponse.getBalance()).as("Проверка, что равна 0")
                .isEqualByComparingTo(BigDecimal.ZERO);

    }
}
