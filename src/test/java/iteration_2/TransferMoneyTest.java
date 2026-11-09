package iteration_2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.TransactionData;
import models.TransferRequest;
import models.TransferResponse;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountTransactionsRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.CustomerAccountsRequester;
import requests.DepositRequester;
import requests.TransferRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferMoneyTest extends BaseTest {

    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("5000");
    private static final String INVALID_TRANSFER =
            "Invalid transfer: insufficient funds or invalid accounts";
    // счёт, которого заведомо нет
    private static final long NON_EXISTENT_ACCOUNT_ID = 2_000_000_000L;

    private RequestSpecification spec;
    private long senderId;
    private long receiverId;

    // у каждого теста свой новый пользователь с двумя пустыми счетами
    @BeforeEach
    public void setupUserWithTwoAccounts() {
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
        senderId = createAccount();
        receiverId = createAccount();
    }

    private long createAccount() {
        return new CreateAccountRequester(spec, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().as(CreateAccountResponse.class)
                .getId();
    }

    // максимум за один депозит 5000, поэтому большие суммы вносим частями
    private void fundAccount(long accountId, BigDecimal amount) {
        BigDecimal rest = amount;
        while (rest.signum() > 0) {
            BigDecimal chunk = rest.min(MAX_DEPOSIT);
            new DepositRequester(spec, ResponseSpecs.requestResponseOK())
                    .post(DepositRequest.builder().id(accountId).balance(chunk).build());
            rest = rest.subtract(chunk);
        }
    }

    private List<TransactionData> getTransactions(long accountId) {
        return new AccountTransactionsRequester(spec, ResponseSpecs.requestResponseOK())
                .get(accountId)
                .extract().jsonPath()
                .getList("", TransactionData.class);
    }

    private BigDecimal getBalance(long accountId) {
        return new CustomerAccountsRequester(spec, ResponseSpecs.requestResponseOK())
                .get()
                .extract().jsonPath()
                .getList("", CreateAccountResponse.class)
                .stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Счёт " + accountId + " не найден у пользователя"))
                .getBalance();
    }

    private TransactionData singleTransaction(List<TransactionData> transactions, String type) {
        List<TransactionData> filtered = transactions.stream()
                .filter(t -> type.equals(t.getType()))
                .toList();
        assertThat(filtered)
                .as("Ожидалась ровно одна транзакция " + type + ", есть: " + transactions)
                .hasSize(1);
        return filtered.get(0);
    }

    private void transferExpectingError(long from, long to, BigDecimal amount, String errorMessage) {
        new TransferRequester(
                spec,
                ResponseSpecs.requestResponseBadRequestWithMessage(errorMessage))
                .post(TransferRequest.builder()
                        .senderAccountId(from)
                        .receiverAccountId(to)
                        .amount(amount)
                        .build());
    }

    private void assertNothingWasTransferred() {
        softly.assertThat(getTransactions(senderId))
                .extracting(TransactionData::getType)
                .as("У отправителя не должно быть TRANSFER_OUT")
                .doesNotContain("TRANSFER_OUT");
        softly.assertThat(getTransactions(receiverId))
                .as("У получателя не должно быть транзакций")
                .isEmpty();
    }

    // Позитивный кейс: минимальная, обычная и максимальная суммы перевода
    public static Stream<Arguments> validTransferAmounts() {
        return Stream.of(
                Arguments.of(new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("1234.56")),
                Arguments.of(new BigDecimal("10000.00")));
    }

    @MethodSource("validTransferAmounts")
    @ParameterizedTest
    public void userCanTransferMoneyBetweenOwnAccounts(BigDecimal amount) {
        fundAccount(senderId, amount);
        BigDecimal senderBefore = getBalance(senderId);
        BigDecimal receiverBefore = getBalance(receiverId);

        TransferResponse response = new TransferRequester(
                spec,
                ResponseSpecs.requestResponseOK())
                .post(TransferRequest.builder()
                        .senderAccountId(senderId)
                        .receiverAccountId(receiverId)
                        .amount(amount)
                        .build())
                .extract().as(TransferResponse.class);

        softly.assertThat(response.getSenderAccountId()).isEqualTo(senderId);
        softly.assertThat(response.getReceiverAccountId()).isEqualTo(receiverId);
        softly.assertThat(response.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(response.getMessage()).isEqualTo("Transfer successful");

        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя")
                .isEqualByComparingTo(senderBefore.subtract(amount));
        softly.assertThat(getBalance(receiverId))
                .as("Баланс получателя")
                .isEqualByComparingTo(receiverBefore.add(amount));

        TransactionData out = singleTransaction(getTransactions(senderId), "TRANSFER_OUT");
        softly.assertThat(out.getId()).isNotNull().isPositive();
        softly.assertThat(out.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(out.getRelatedAccountId()).isEqualTo((int) receiverId);
        softly.assertThat(out.getTimestamp()).isNotNull();

        TransactionData in = singleTransaction(getTransactions(receiverId), "TRANSFER_IN");
        softly.assertThat(in.getId()).isNotNull().isPositive();
        softly.assertThat(in.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(in.getRelatedAccountId()).isEqualTo((int) senderId);
        softly.assertThat(in.getTimestamp()).isNotNull();
    }

    // Негативный кейс: сумма больше максимальной, отрицательная и ноль
    public static Stream<Arguments> invalidTransferAmounts() {
        return Stream.of(
                Arguments.of(new BigDecimal("10000.01"), "Transfer amount cannot exceed 10000"),
                Arguments.of(new BigDecimal("-1"), "Transfer amount must be at least 0.01"),
                Arguments.of(new BigDecimal("0"), "Transfer amount must be at least 0.01"));
    }

    @MethodSource("invalidTransferAmounts")
    @ParameterizedTest
    public void userCannotTransferInvalidAmount(BigDecimal amount, String errorMessage) {
        // на счёте достаточно денег, чтобы сработала именно проверка суммы, а не нехватка средств
        fundAccount(senderId, new BigDecimal("15000"));

        transferExpectingError(senderId, receiverId, amount, errorMessage);

        assertNothingWasTransferred();
    }

    // Негативный кейс: перевод больше, чем есть на счёте
    @Test
    public void userCannotTransferMoreThanBalance() {
        fundAccount(senderId, new BigDecimal("100.00"));

        transferExpectingError(senderId, receiverId, new BigDecimal("5000"), INVALID_TRANSFER);

        assertNothingWasTransferred();
        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя не должен измениться")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // Негативный кейс: перевод на несуществующий счёт
    @Test
    public void userCannotTransferToNonExistentAccount() {
        fundAccount(senderId, new BigDecimal("1000"));

        transferExpectingError(senderId, NON_EXISTENT_ACCOUNT_ID, new BigDecimal("500"), INVALID_TRANSFER);

        assertNothingWasTransferred();
        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя не должен измениться")
                .isEqualByComparingTo(new BigDecimal("1000"));
    }
}