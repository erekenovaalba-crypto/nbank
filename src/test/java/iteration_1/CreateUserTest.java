package iteration_1;

import comparison.ModelAssertions;
import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import spec.ResponseSpecs;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseTest {

    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse = AdminSteps.createUser(createUserRequest);

        ModelAssertions.assertThatModels(createUserRequest, createUserResponse).match();

        // статус 201 ещё не значит, что пользователь сохранён: проверяем фактическое состояние через GET
        Optional<CreateUserResponse> savedUser = AdminSteps.findUser(createUserResponse.getId());
        assertThat(savedUser)
                .as("Созданный пользователь должен быть в списке пользователей")
                .isPresent();
        ModelAssertions.assertThatModels(createUserRequest, savedUser.get()).match();
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
    public void adminCannotCreateUserWithNotCorrectDataTest(String username, String password, String role,
                                                            JsonField errorKey, ErrorMessage errorValue) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        AdminSteps.createUserExpectingError(createUserRequest,
                ResponseSpecs.requestResponseBadRequest(errorKey, errorValue));

        // статус 400 ещё не значит, что пользователь не создан: проверяем фактическое состояние через GET
        softly.assertThat(AdminSteps.getUsers())
                .extracting(CreateUserResponse::getUsername)
                .as("Пользователь с некорректными данными не должен быть создан")
                .doesNotContain(username);
    }
}
