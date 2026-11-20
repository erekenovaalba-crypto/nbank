package iteration_1;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {

    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        CreateUserRequest userRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);
        CreateUserResponse userResponse = new ValidatedCrudRequesters<CreateUserResponse>
                (RequestSpecs.adminSpec(),
                        Endpoint.ADMIN_USER,
                        ResponseSpecs.
                                entityWasCreated())
                .post(userRequest);
    comparison.ModelAssertions.assertThatModels(userRequest, userResponse).match();
    }


    // граничные значения логина: из одних пробелов и короче минимальной длины
    private static final String BLANK_USERNAME = "    ";
    private static final String TOO_SHORT_USERNAME = "ad";

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of(BLANK_USERNAME, RandomData.getPassword(), UserRole.USER.name(),
                        JsonField.USERNAME, ErrorMessage.USERNAME_BLANK),
                Arguments.of(TOO_SHORT_USERNAME, RandomData.getPassword(), UserRole.USER.name(),
                        JsonField.USERNAME, ErrorMessage.USERNAME_INVALID_LENGTH));
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanCreateUserWithNotCorrectDataTest(String username, String password, String role, JsonField errorKey, ErrorMessage errorValue) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
        new CrudRequester(RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestResponseBadRequest(errorKey, errorValue))
                .post(createUserRequest);

        // статус 400 ещё не значит, что пользователь не создан: проверяем фактическое состояние через GET
  /*      softly.assertThat(getUsernames())
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
*/
}}
