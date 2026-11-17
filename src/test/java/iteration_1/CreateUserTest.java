package iteration_1;

import models.ErrorMessage;
import models.JsonField;
import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.AdminGetUsersRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CreateUserTest extends BaseTest{

    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        CreateUserRequest createUserRequest =  CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();
        CreateUserResponse createUserResponse = new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(createUserRequest).extract().as(CreateUserResponse.class);
        softly.assertThat(createUserRequest.getUsername()).isEqualTo(createUserResponse.getUsername());
        softly.assertThat(createUserRequest.getPassword()).isNotEqualTo(createUserResponse.getUsername());
        softly.assertThat(createUserRequest.getRole()).isEqualTo(createUserResponse.getRole());
    }


    // граничные значения логина: из одних пробелов и короче минимальной длины
    private static final String BLANK_USERNAME = "    ";
    private static final String TOO_SHORT_USERNAME = "ad";

    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                Arguments.of(BLANK_USERNAME, RandomData.getPassword(), UserRole.USER.name(),
                        JsonField.USERNAME, ErrorMessage.USERNAME_BLANK),
                Arguments.of(TOO_SHORT_USERNAME, RandomData.getPassword(), UserRole.USER.name(),
                        JsonField.USERNAME, ErrorMessage.USERNAME_INVALID_LENGTH));
    }
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanCreateUserWithNotCorrectDataTest(String username, String password, String role, JsonField errorKey, ErrorMessage errorValue) {
        CreateUserRequest createUserRequest =  CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.requestResponseBadRequest(errorKey,errorValue))
                .post(createUserRequest);

        // статус 400 ещё не значит, что пользователь не создан: проверяем фактическое состояние через GET
        softly.assertThat(getUsernames())
                .as("Пользователь с некорректными данными не должен быть создан")
                .doesNotContain(username);
    }

    private List<String> getUsernames() {
        CreateUserResponse[] users = new AdminGetUsersRequester(RequestSpecs.adminSpec(), ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CreateUserResponse[].class);
        return Arrays.stream(users)
                .map(CreateUserResponse::getUsername)
                .toList();
    }

}
