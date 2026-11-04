package iteration_2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@Nested
class UpdateNameTest {
    private static final String URL = "http://localhost:4111/";
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> userValidDataForUpdateName(){
        return Stream.of(

                Arguments.of("Albina Abzhetova"),
                Arguments.of("albina abzhetova")

        );
    }
    @MethodSource("userValidDataForUpdateName")
    @ParameterizedTest
    public void userUpdateNameWithCorrectData(String name) {

        String requestBody = String.format(
                """
                       {
                          "name": "%s"
                        }
                        """, name
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .put(URL +"api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.equalTo(name))
                .body("message", Matchers.equalTo("Profile updated successfully"));;
    }
    //негативные кейсы
    public static Stream<Arguments> userInvalidDataForUpdateName(){
        return Stream.of(
                // Негативный кейс:Изменение имени пользователя (одно слово)
                Arguments.of("Johооо"),
                //Негативный кейс:Изменение имени пользователя (два слова разделенные пробелом с символом
                Arguments.of("ajak sksk("),
                //Негативный кейс:Изменение имени пользователя (два слова разделенные пробелом с цифрой)
                Arguments.of("ajak sksk1"),
                //Негативный кейс:Изменение имени пользователя (два слова разделенные  несколькими пробелами)
                Arguments.of("ajak     sksk")

        );
    }
    @MethodSource("userInvalidDataForUpdateName")
    @ParameterizedTest
    public void userUpdateNameWithIncorrectData(String name) {

        String requestBody = String.format(
                """
                       {
                          "name": "%s"
                        }
                        """, name
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .put(URL +"api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body( Matchers.equalTo("Name must contain two words with letters only"));
    }
}


