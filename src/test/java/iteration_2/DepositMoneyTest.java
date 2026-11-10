package iteration_2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.TransactionData;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountTransactionsRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositMoneyTest extends BaseTest {

    private RequestSpecification userSpec;
    private long accountId;

    // у каждого теста свой новый пользователь и свой пустой счёт
    @BeforeEach
    public void setupUser() {
        userSpec = createUser();
        accountId = createAccount(userSpec);
    }

    // создаёт пользователя через админа и возвращает спецификацию для запросов от его имени
    private RequestSpecification createUser() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        return RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());
    }

    // создаёт счёт пользователю и возвращает его id
    private long createAccount(RequestSpecification spec) {
        return new CreateAccountRequester(spec, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().as(CreateAccountResponse.class)
                .getId();
    }

    private List<TransactionData> getTransactions(RequestSpecification spec, long id) {
        return new AccountTransactionsRequester(spec, ResponseSpecs.requestResponseOK())
                .get(id)
                .extract().jsonPath()
                .getList("", TransactionData.class);
    }

    // Позитивный кейс: граничные суммы 0.01, 4999.99, 5000
    public static Stream<Arguments> validDepositAmounts() {
        return Stream.of(
                Arguments.of(new BigDecimal("5000.00")),
                Arguments.of(new BigDecimal("4999.99")),
                Arguments.of(new BigDecimal("0.01")));
    }

    @MethodSource("validDepositAmounts")
    @ParameterizedTest
    public void userCanDepositMoneyIntoOwnAccount(BigDecimal amount) {
        CreateAccountResponse response = new DepositRequester(
                userSpec,
                ResponseSpecs.requestResponseOK())
                .post(DepositRequest.builder().id(accountId).balance(amount).build())
                .extract().as(CreateAccountResponse.class);

        softly.assertThat(response.getId()).isEqualTo(accountId);
        softly.assertThat(response.getBalance()).as("Баланс в ответе депозита").isEqualByComparingTo(amount);

        // счёт новый, значит транзакция должна быть ровно одна
        assertThat(response.getTransactions()).hasSize(1);
        TransactionData deposit = response.getTransactions().get(0);

        softly.assertThat(deposit.getId()).isNotNull().isPositive();
        softly.assertThat(deposit.getType()).isEqualTo("DEPOSIT");
        softly.assertThat(deposit.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(deposit.getRelatedAccountId()).isEqualTo((int) accountId);
        softly.assertThat(deposit.getTimestamp()).isNotNull();

        // то же самое должно быть в истории транзакций счёта
        List<TransactionData> stored = getTransactions(userSpec, accountId);
        assertThat(stored).hasSize(1);
        softly.assertThat(stored.get(0))
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(deposit);
    }

    // Негативный кейс: сумма больше максимальной, 0 и отрицательная
    public static Stream<Arguments> invalidDepositAmounts() {
        return Stream.of(
                Arguments.of(new BigDecimal("5000.01"), "Deposit amount cannot exceed 5000"),
                Arguments.of(new BigDecimal("-1"), "Deposit amount must be at least 0.01"),
                Arguments.of(new BigDecimal("0"), "Deposit amount must be at least 0.01"));
    }

    @MethodSource("invalidDepositAmounts")
    @ParameterizedTest
    public void userCannotDepositInvalidAmount(BigDecimal amount, String errorMessage) {
        new DepositRequester(
                userSpec,
                ResponseSpecs.requestResponseBadRequestWithMessage(errorMessage))
                .post(DepositRequest.builder().id(accountId).balance(amount).build());

        softly.assertThat(getTransactions(userSpec, accountId))
                .as("После неудачного депозита транзакций быть не должно")
                .isEmpty();
    }

    // Негативный кейс: депозит на чужой счёт
    @Test
    public void userCannotDepositIntoForeignAccount() {
        RequestSpecification ownerSpec = createUser();
        long ownerAccountId = createAccount(ownerSpec);

        new DepositRequester(
                userSpec,
                ResponseSpecs.requestResponseForbidden("Unauthorized access to account"))
                .post(DepositRequest.builder()
                        .id(ownerAccountId)
                        .balance(new BigDecimal("100.00"))
                        .build());

        // историю чужого счёта тоже читать нельзя
        new AccountTransactionsRequester(
                userSpec,
                ResponseSpecs.requestResponseForbidden("You do not have permission to access this account"))
                .get(ownerAccountId);

        //  у владельца счёта ничего не изменилось
        softly.assertThat(getTransactions(ownerSpec, ownerAccountId)).isEmpty();
    }
}