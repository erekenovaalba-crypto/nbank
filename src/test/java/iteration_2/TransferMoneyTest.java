package iteration_2;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class TransferMoneyTest {
    private static final String URL = "http://localhost:4111/";
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
        RestAssured.config = RestAssured.config().jsonConfig(
                JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                //Негативный кейс: перевод больше допустимой суммы
                Arguments.of(1,2, new BigDecimal("10000.01"),"Transfer amount cannot exceed 10000"),
                //перевод отрицательной суммы отрицательной суммы
                Arguments.of(1, 2, new BigDecimal("-1"), "Transfer amount must be at least 0.01"),
                //перевод 0
                Arguments.of(1,2, new BigDecimal("0"),"Transfer amount must be at least 0.01"),
                // перевод суммы, большей чем есть на счету
                Arguments.of(2,1, new BigDecimal("5000"),"Invalid transfer: insufficient funds or invalid accounts"),
                //перевод на не существующий аккаунт
                Arguments.of(1,10, new BigDecimal("500"),"Invalid transfer: insufficient funds or invalid accounts")
        );
    }
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userTransferMoneyIntoAccountWithIncorrectData(int senderId, int receiverId, BigDecimal amount, String responseError) {

        String requestBody = String.format(
                """
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %s
                        }
                        """, senderId, receiverId, amount
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .post(URL +"api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(responseError));
    }
    public static Stream<Arguments> userValidData(){
        return Stream.of(
                //
                Arguments.of(1,2, new BigDecimal("0.01")),
                Arguments.of(1,2, new BigDecimal("10000.0")));


    }
    @MethodSource("userValidData")
    @ParameterizedTest
    public void userTransferMoneyIntoAccountWithCorrectData(int senderId, int receiverId, BigDecimal amount) {

        String requestBody = String.format(
                """
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %s
                        }
                        """, senderId, receiverId, amount
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .post(URL +"api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("receiverAccountId",Matchers.equalTo(receiverId))
                .body("senderAccountId",Matchers.equalTo(senderId))
                .body("message",Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.comparesEqualTo(amount));
    }
}
