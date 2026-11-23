package iteration_1;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.ErrorMessage;
import models.JsonField;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {

    @Test
    public void adminCanCreateUserWithCorrectDataTest() {
        // Arrange
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        // Act
        CreateUserResponse userResponse = new ValidatedCrudRequesters<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // Assert
        comparison.ModelAssertions.assertThatModels(userRequest, userResponse).match();
    }

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
    public void adminCannotCreateUserWithInvalidDataTest(
            String username, String password, String role,
            JsonField errorKey, ErrorMessage errorValue) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestResponseBadRequest(errorKey, errorValue))
                .post(request);
    }
}
