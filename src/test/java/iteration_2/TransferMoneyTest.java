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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.sessionId;
import static org.junit.jupiter.api.Assertions.*;

public class TransferMoneyTest {
    private static final String URL = "http://localhost:4111/";
    private static final String AUTH_HEADER = "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==";
    private List<TransactionData> getTransactions(int id) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", AUTH_HEADER)
                .get(URL + "api/v1/accounts/" + id + "/transactions")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract()
                .body()
                .jsonPath()
                .getList("", TransactionData.class);
    }
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
                Arguments.of(3,1, new BigDecimal("5000"),"Invalid transfer: insufficient funds or invalid accounts"),
                //перевод на не существующий аккаунт
                Arguments.of(1,10, new BigDecimal("500"),"Invalid transfer: insufficient funds or invalid accounts")
        );
    }
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userTransferMoneyIntoAccountWithIncorrectData(int senderId, int receiverId, BigDecimal amount, String responseError) {
        int countBefore = getTransactions(senderId).size();
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
                .header("Authorization", AUTH_HEADER)
                .body(requestBody)
                .post(URL +"api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(responseError));
        List<TransactionData> transactionsAfter = getTransactions(senderId);
        assertEquals(countBefore, transactionsAfter.size(),
                "Количество транзакций изменилось после неудачного депозита: " + transactionsAfter);

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
                .header("Authorization", AUTH_HEADER)
                .body(requestBody)
                .post(URL +"api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("receiverAccountId",Matchers.equalTo(receiverId))
                .body("senderAccountId",Matchers.equalTo(senderId))
                .body("message",Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.comparesEqualTo(amount));

        List<TransactionData> transactionsAfter = getTransactions(senderId);
        TransactionData last = transactionsAfter.stream()
                .max(Comparator.comparing(TransactionData::getId))
                .orElseThrow(() -> new AssertionError("Список транзакций пуст"));
        assertAll("Последняя транзакция",
                () -> assertNotNull(last.getId()),
                () -> assertTrue(last.getId() > 0),
                () -> assertEquals("TRANSFER_OUT", last.getType()),
                ()->assertEquals(receiverId,last.getRelatedAccountId()),
                () -> assertEquals(0, last.getAmount().compareTo(amount),
                        "Ожидалась сумма " + amount + ", пришла " + last.getAmount()),
                () -> assertNotNull(last.getTimestamp())
        );
    }
}
