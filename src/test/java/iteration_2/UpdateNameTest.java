package iteration_2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.CreateUserRequest;
import models.CustomerProfile;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.GetProfileRequester;
import requests.UpdateProfileRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateNameTest extends BaseTest {

    private RequestSpecification spec;

    // у каждого теста свой новый пользователь
    @BeforeEach
    public void setupUser() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        spec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());
    }

    private String getProfileName() {
        return new GetProfileRequester(spec, ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CustomerProfile.class)
                .getName();
    }

    // Позитивный кейс: два слова из букв, в любом регистре
    public static Stream<Arguments> validNames() {
        return Stream.of(
                Arguments.of("Albina Abzhetova"),
                Arguments.of("albina abzhetova"));
    }

    @MethodSource("validNames")
    @ParameterizedTest
    public void userCanUpdateNameWithValidData(String name) {
        UpdateProfileResponse response = new UpdateProfileRequester(
                spec,
                ResponseSpecs.requestResponseOK())
                .put(UpdateProfileRequest.builder().name(name).build())
                .extract().as(UpdateProfileResponse.class);

        softly.assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        softly.assertThat(response.getCustomer().getName()).isEqualTo(name);

        // имя действительно сохранилось в профиле
        softly.assertThat(getProfileName()).isEqualTo(name);
    }

    // Негативный кейс: имя должно состоять из двух слов, только из букв
    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                // одно слово
                Arguments.of("John"),
                // два слова, во втором символ
                Arguments.of("ajak sksk("),
                // два слова, во втором цифра
                Arguments.of("ajak sksk1"),
                // два слова через несколько пробелов
                Arguments.of("ajak     sksk"));
    }

    @MethodSource("invalidNames")
    @ParameterizedTest
    public void userCannotUpdateNameWithInvalidData(String name) {
        String nameBefore = getProfileName();

        new UpdateProfileRequester(
                spec,
                ResponseSpecs.requestResponseBadRequestWithMessage("Name must contain two words with letters only"))
                .put(UpdateProfileRequest.builder().name(name).build());

        softly.assertThat(getProfileName())
                .as("Имя в профиле не должно измениться")
                .isEqualTo(nameBefore);
    }
}