package iteration_1;

import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.replaceFiltersWith;

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


    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                Arguments.of("    ", "Passwordsssd335$","USER","username","Username cannot be blank"),
                Arguments.of("ad", "Passwordsssd335$","USER","username","Username must be between 3 and 15 characters"));
    }
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanCreateUserWithNotCorrectDataTest(String username, String password, String role, String errorKey, String errorValue) {
        CreateUserRequest createUserRequest =  CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
      new AdminCreateUserRequester(RequestSpecs.adminSpec(),
              ResponseSpecs.requestResponseBadRequest(errorKey,errorValue))
              .post(createUserRequest);
    }

}

