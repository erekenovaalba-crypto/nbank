package iteration_2;

import models.comparison.ModelAssertions;
import generators.NameGenerator;
import generators.RandomModelGenerator;
import iteration_1.BaseTest;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import spec.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateNameTest extends BaseTest {

    private CreateUserRequest user;

    // у каждого теста свой новый пользователь
    @BeforeEach
    public void setupUser() {
        user = AdminSteps.createUser();
    }

    // Позитивный кейс: два слова из букв, с заглавными и строчными буквами
    public static Stream<Arguments> validNames() {
        UpdateProfileRequest capitalized = RandomModelGenerator.generate(UpdateProfileRequest.class);
        UpdateProfileRequest lowerCase = new UpdateProfileRequest(capitalized.getName().toLowerCase());
        return Stream.of(
                Arguments.of(capitalized),
                Arguments.of(lowerCase));
    }

    @MethodSource("validNames")
    @ParameterizedTest
    public void userCanUpdateNameWithValidData(UpdateProfileRequest request) {
        UpdateProfileResponse response = UserSteps.updateName(user, request);

        softly.assertThat(response.getMessage()).isEqualTo(SuccessMessage.PROFILE_UPDATED.getMessage());
        ModelAssertions.assertThatModels(request, response).match();

        //  GET
        ModelAssertions.assertThatModels(request, UserSteps.getProfile(user)).match();
    }

    // Негативный кейс: имя должно состоять из двух слов, только из букв
    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of(new UpdateProfileRequest(NameGenerator.singleWordName())),
                Arguments.of(new UpdateProfileRequest(NameGenerator.nameWithSymbol())),
                Arguments.of(new UpdateProfileRequest(NameGenerator.nameWithDigit())),
                Arguments.of(new UpdateProfileRequest(NameGenerator.nameWithExtraSpaces())));
    }

    @MethodSource("invalidNames")
    @ParameterizedTest
    public void userCannotUpdateNameWithInvalidData(UpdateProfileRequest request) {
        String nameBefore = UserSteps.getProfile(user).getName();

        UserSteps.updateNameExpectingError(user, request,
                ResponseSpecs.requestResponseBadRequestWithMessage(ErrorMessage.INVALID_NAME));
        //GET
        softly.assertThat(UserSteps.getProfile(user).getName())
                .as("Имя в профиле не должно измениться")
                .isEqualTo(nameBefore);
    }
}
