package iteration_1;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetProfileRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
        //get
        CustomerProfile profile = new GetProfileRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()), ResponseSpecs.requestResponseOK())
                .get().extract().as(CustomerProfile.class);

        Optional<CreateAccountResponse> accountInProfile = profile.getAccounts().stream()
                .filter(account -> account.getId() == createAccountResponse.getId())
                .findFirst();
        assertThat(accountInProfile)
                .as("Созданный счёт должен быть в профиле пользователя")
                .isPresent();

        CreateAccountResponse savedAccount = accountInProfile.get();
        softly.assertThat(savedAccount.getAccountNumber()).as("Номер сохранённого счёта")
                .isEqualTo(createAccountResponse.getAccountNumber());
        softly.assertThat(savedAccount.getBalance()).as("Баланс сохранённого счёта")
                .isEqualByComparingTo(BigDecimal.ZERO);
        softly.assertThat(profile.getAccounts()).as("У нового пользователя должен быть ровно один счёт")
                .hasSize(1);
    }
}
