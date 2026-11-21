package iteration_1;
import models.*;
import generators.RandomData;
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
import java.util.Optional;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseTest {
    private List<CreateUserResponse> getUsers() {
        CreateUserResponse[] users = new AdminGetUsersRequester(RequestSpecs.adminSpec(), ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CreateUserResponse[].class);
        return Arrays.asList(users);
    }
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
        softly.assertThat(createUserRequest.getPassword()).isNotEqualTo(createUserResponse.getPassword());
        softly.assertThat(createUserRequest.getRole()).isEqualTo(createUserResponse.getRole());
        //GET
        Optional<CreateUserResponse> savedUser = getUsers().stream()
                .filter(user -> user.getId() == createUserResponse.getId())
                .findFirst();
        assertThat(savedUser)
                .as("Созданный пользователь должен быть в списке пользователей")
                .isPresent();
        softly.assertThat(savedUser.get().getUsername()).as("Логин сохранённого пользователя")
                .isEqualTo(createUserRequest.getUsername());
        softly.assertThat(savedUser.get().getRole()).as("Роль сохранённого пользователя")
                .isEqualTo(createUserRequest.getRole());
        softly.assertThat(savedUser.get().getPassword()).as("Пароль сохранённого пользователя хранится в виде хеша")
                .isNotEqualTo(createUserRequest.getPassword());
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

        //GET
        softly.assertThat(getUsers())
                .extracting(CreateUserResponse::getUsername)
                .as("Пользователь с некорректными данными не должен быть создан")
                .doesNotContain(username);
    }

}
