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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositMoneyTest {
    private static final String URL = "http://localhost:4111/";
    private static final String AUTH_HEADER = "Basic YWxiaW5hNDkwOnZlcnlzVFJvbmdQYXNzd29yZDMzJA==";

    //метод для получения транзакций и экспорт их в list
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
                .post(URL + "api/v1/auth/login")
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
                .header("Authorization", AUTH_HEADER)
                .post(URL + "api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("id", Matchers.notNullValue())
                .body("accountNumber", Matchers.notNullValue())
                .body("balance", Matchers.equalTo(0.0F));

    }
    // Позитивный кейс: Внесение денег на счет, граничные суммы: 4999, 5000,1
    public static Stream<Arguments> userValidData() {
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
                        .header("Authorization", AUTH_HEADER)
                        .body(requestBody)
                        .post(URL + "api/v1/accounts/deposit")
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract()
                        .body()
                        .jsonPath()
                        .getList("transactions", TransactionData.class);

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
        List<TransactionData> transactionDataList1 =
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", AUTH_HEADER)
                .body(requestBody)
                .get(URL + "api/v1/accounts/" + last.getRelatedAccountId().toString() + "/transactions")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract()
                .body()
                .jsonPath()
                .getList("", TransactionData.class)
        ;
        TransactionData last1 = transactionDataList1.stream()
                .max(Comparator.comparing(TransactionData::getId))
                .orElseThrow(() -> new AssertionError("Список транзакций пуст"));
        assertAll("Последняя транзакция",
                () -> assertEquals(last.getId(), last1.getId()),
                () -> assertEquals(last.getType(), last1.getType()),
                () -> assertEquals(0, last.getAmount().compareTo(balance)),
                () -> assertEquals(last.getRelatedAccountId(), last1.getRelatedAccountId()),
                () -> assertEquals(last.getTimestamp(), last1.getTimestamp())
        );


    }

    //Негативный кейс: Внесение денег на счет, сумма больше максимальной 5001
    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                //Негативный кейс: Внесение денег на счет, сумма больше максимальной 5000.01
                Arguments.of(1, new BigDecimal("5000.01"), "Deposit amount cannot exceed 5000"),
                //внесение отрицательной суммы
                Arguments.of(1, new BigDecimal("-1"), "Deposit amount must be at least 0.01"),
                //внесение 0
                Arguments.of(1, new BigDecimal("0"), "Deposit amount must be at least 0.01"));


    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void userDepositsMoneyIntoAccountWithIncorrectData(int id, BigDecimal balance, String responseError) {
        int countBefore = getTransactions(id).size();
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
                .header("Authorization", AUTH_HEADER)
                .body(requestBody)
                .post(URL + "api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(responseError));


        List<TransactionData> transactionsAfter = getTransactions(id);
        assertEquals(countBefore, transactionsAfter.size(),
                "Количество транзакций изменилось после неудачного депозита: " + transactionsAfter);
    }

    //внесение на не существующий счет
    public static Stream<Arguments> userIncorrectAccount() {
        return Stream.of(
                Arguments.of(10, new BigDecimal("100.0"), "Unauthorized access to account"));
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
                .header("Authorization", AUTH_HEADER)
                .body(requestBody)
                .post(URL + "api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo(responseError));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", AUTH_HEADER)
                .get(URL + "api/v1/accounts/" + id + "/transactions")
                .then()
                .assertThat().statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("You do not have permission to access this account"));

    }
}
