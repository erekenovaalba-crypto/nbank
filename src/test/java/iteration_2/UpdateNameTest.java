package iteration_2;

import generators.NameGenerator;
import iteration_1.BaseTest;
import models.CustomerProfile;
import models.ErrorMessage;
import models.SuccessMessage;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.GetProfileRequester;
import requests.UpdateProfileRequester;
import requests.steps.AdminSteps;
import io.restassured.specification.RequestSpecification;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateNameTest extends BaseTest {

    private RequestSpecification spec;

    @BeforeEach
    public void setupUser() {
        var userRequest = AdminSteps.createUser();
        spec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());
    }

    private String getProfileName() {
        return new GetProfileRequester(spec, ResponseSpecs.requestResponseOK())
                .get()
                .extract()
                .as(CustomerProfile.class)
                .getName();
    }

    public static Stream<Arguments> validNames() {
        return Stream.of(
                Arguments.of(NameGenerator.validName()),
                Arguments.of(NameGenerator.validNameLowerCase()));
    }

    @MethodSource("validNames")
    @ParameterizedTest
    public void userCanUpdateNameWithValidData(String name) {
        UpdateProfileResponse response = new UpdateProfileRequester(
                spec,
                ResponseSpecs.requestResponseOK())
                .put(UpdateProfileRequest.builder().name(name).build())
                .extract()
                .as(UpdateProfileResponse.class);

        softly.assertThat(response.getMessage())
                .isEqualTo(SuccessMessage.PROFILE_UPDATED.getMessage());
        softly.assertThat(response.getCustomer().getName()).isEqualTo(name);
        softly.assertThat(getProfileName())
                .as("Имя должно сохраниться в профиле")
                .isEqualTo(name);
    }

    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of(NameGenerator.singleWordName()),
                Arguments.of(NameGenerator.nameWithSymbol()),
                Arguments.of(NameGenerator.nameWithDigit()),
                Arguments.of(NameGenerator.nameWithExtraSpaces()));
    }

    @MethodSource("invalidNames")
    @ParameterizedTest
    public void userCannotUpdateNameWithInvalidData(String name) {
        String nameBefore = getProfileName();

        new UpdateProfileRequester(
                spec,
                ResponseSpecs.requestResponseBadRequestWithMessage(ErrorMessage.INVALID_NAME))
                .put(UpdateProfileRequest.builder().name(name).build());

        softly.assertThat(getProfileName())
                .as("Имя в профиле не должно измениться после невалидного запроса")
                .isEqualTo(nameBefore);
    }
}
