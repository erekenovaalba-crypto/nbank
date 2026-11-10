package iteration_2;
import static org.junit.jupiter.api.Assertions.*;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositMoneyTest {
    private static final String URL = "http://localhost:4111/";
    @BeforeAll
    public static void setupRestAssured(){
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));

    }
    @Test
    public void userCanGenerateAuthTokenTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "albina490",
                          "password": "verysTRongPassword33$"
                        }
                        """)
                .post(URL+"api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue());

    }
    //два раза запустить тест, чтобы создать два аккаунта
    @Test
    public void userCanCreateAccountTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .post(URL + "api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("id", Matchers.notNullValue())
                .body("accountNumber", Matchers.notNullValue())
                .body("balance", Matchers.equalTo(0.0F));

    }
    // Позитивный кейс: Внесение денег на счет, граничные суммы: 4999, 5000,1
    public static Stream<Arguments> userValidData(){
        return Stream.of(
                Arguments.of(1, new BigDecimal("5000.0")),
                Arguments.of(1, new BigDecimal("4999.99")),
                Arguments.of(1, new BigDecimal("0.01")));
    }
    @MethodSource("userValidData")
    @ParameterizedTest
    public void userDepositsMoneyIntoAccount(int id, BigDecimal balance) {

        String requestBody = String.format(
                """
                        {
                          "id": %d,
                          "balance": %s
                        }
                        """, id, balance
        );
        List<TransactionData> transactionDataList =
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                        .body(requestBody)
                        .post(URL +"api/v1/accounts/deposit")
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract()
                        .body()
                        .jsonPath()
                        .getList("transactions", TransactionData.class)
                ;

        TransactionData last = transactionDataList.stream()
                .max(Comparator.comparing(TransactionData::getId))
                .orElseThrow(() -> new AssertionError("Список транзакций пуст"));
        assertAll("Последняя транзакция",
                () -> assertNotNull(last.getId()),
                () -> assertTrue(last.getId() > 0),
                () -> assertEquals("DEPOSIT", last.getType()),
                () -> assertNotNull(last.getAmount()),
                () -> assertEquals(0, last.getAmount().compareTo(balance),
                        "Ожидалась сумма " + balance + ", пришла " + last.getAmount()),
                () -> assertNotNull(last.getTimestamp())
        );


    }
    //Негативный кейс: Внесение денег на счет, сумма больше максимальной 5001
    public static Stream<Arguments> userInvalidData(){
        return Stream.of(
                //Негативный кейс: Внесение денег на счет, сумма больше максимальной 5000.1
                Arguments.of(1, new BigDecimal("5000.01"),"Deposit amount cannot exceed 5000"),
                //внесение отрицательной суммы
                Arguments.of(1, new BigDecimal("-1"), "Deposit amount must be at least 0.01"),
                //внесение 0
                Arguments.of(1, new BigDecimal("0"),"Deposit amount must be at least 0.01"));


    }
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userDepositsMoneyIntoAccountWithIncorrectData(int id, BigDecimal balance, String responseError) {

        String requestBody = String.format(
                """
                        {
                          "id": %d,
                          "balance": %s
                        }
                        """, id, balance
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .post(URL +"api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(responseError));


    }
    //внесение на не существующий счет
    public static Stream<Arguments> userIncorrectAccount(){
        return Stream.of(
                Arguments.of(10, new BigDecimal("100.0"),"Unauthorized access to account"));
    }
    @MethodSource("userIncorrectAccount")
    @ParameterizedTest
    public void userDepositsMoneyIntoUnrealAccount(int id, BigDecimal balance, String responseError) {

        String requestBody = String.format(
                """
                        {
                          "id": %d,
                          "balance": %s
                        }
                        """, id, balance
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==")
                .body(requestBody)
                .post(URL +"api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo(responseError));


    }
}
