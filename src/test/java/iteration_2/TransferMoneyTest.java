package iteration_2;

import generators.AmountGenerator;
import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.*;
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
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class TransferMoneyTest extends BaseTest {

    // на счёте больше лимита перевода, чтобы срабатывала проверка суммы, а не нехватка средств
    private static final BigDecimal BALANCE_ABOVE_TRANSFER_LIMIT =
            AmountGenerator.MAX_TRANSFER.add(AmountGenerator.MAX_DEPOSIT);
    // сервер хранит баланс как double (например 24349.699999999983), поэтому сравниваем с допуском
    private static final BigDecimal BALANCE_DELTA = new BigDecimal("0.005");

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
            BigDecimal chunk = rest.min(AmountGenerator.MAX_DEPOSIT);
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
                .doesNotContain(OperationType.TRANSFER_OUT.toString());
        softly.assertThat(getTransactions(receiverId))
                .as("У получателя не должно быть транзакций")
                .isEmpty();
    }

    // Позитивный кейс: минимальная, обычная и максимальная суммы перевода
    public static Stream<Arguments> validTransferAmounts() {
        return Stream.of(
                Arguments.of(AmountGenerator.MIN_AMOUNT),
                Arguments.of(AmountGenerator.validTransferAmount()),
                Arguments.of(AmountGenerator.MAX_TRANSFER));
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

        TransactionData out = singleTransaction(getTransactions(senderId), OperationType.TRANSFER_OUT.toString());
        softly.assertThat(out.getId()).isNotNull().isPositive();
        softly.assertThat(out.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(out.getRelatedAccountId()).isEqualTo((int) receiverId);
        softly.assertThat(out.getTimestamp()).isNotNull();

        TransactionData in = singleTransaction(getTransactions(receiverId), OperationType.TRANSFER_IN.toString());
        softly.assertThat(in.getId()).isNotNull().isPositive();
        softly.assertThat(in.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(in.getRelatedAccountId()).isEqualTo((int) senderId);
        softly.assertThat(in.getTimestamp()).isNotNull();
    }

    // Негативный кейс: сумма больше максимальной, отрицательная и ноль
    public static Stream<Arguments> invalidTransferAmounts() {
        return Stream.of(
                Arguments.of(AmountGenerator.MAX_TRANSFER.add(AmountGenerator.MIN_AMOUNT),
                        ErrorMessages.TRANSFER_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(BigDecimal.ZERO, ErrorMessages.TRANSFER_AMOUNT_TOO_SMALL),
                Arguments.of(AmountGenerator.amountAbove(AmountGenerator.MAX_TRANSFER),
                        ErrorMessages.TRANSFER_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(AmountGenerator.negativeAmount(AmountGenerator.MAX_TRANSFER),
                        ErrorMessages.TRANSFER_AMOUNT_TOO_SMALL));
    }

    @MethodSource("invalidTransferAmounts")
    @ParameterizedTest
    public void userCannotTransferInvalidAmount(BigDecimal amount, String errorMessage) {
        // на счёте достаточно денег, чтобы сработала именно проверка суммы, а не нехватка средств
        fundAccount(senderId, BALANCE_ABOVE_TRANSFER_LIMIT);

        transferExpectingError(senderId, receiverId, amount, errorMessage);

        assertNothingWasTransferred();
    }

    // Негативный кейс: перевод больше, чем есть на счёте
    @Test
    public void userCannotTransferMoreThanBalance() {
        // баланс подбираем так, чтобы сумма перевода не вышла за лимит перевода:
        // иначе сервер ответит ошибкой лимита, а не нехваткой средств
        BigDecimal balance = AmountGenerator.randomAmount(
                AmountGenerator.MIN_AMOUNT, AmountGenerator.MAX_TRANSFER.subtract(AmountGenerator.MIN_AMOUNT));
        BigDecimal amount = AmountGenerator.randomAmount(
                balance.add(AmountGenerator.MIN_AMOUNT), AmountGenerator.MAX_TRANSFER);
        fundAccount(senderId, balance);

        transferExpectingError(senderId, receiverId, amount, ErrorMessages.INVALID_TRANSFER);

        assertNothingWasTransferred();
        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(balance, within(BALANCE_DELTA));

    }

    // Негативный кейс: перевод на несуществующий счёт
    @Test
    public void userCannotTransferToNonExistentAccount() {
        BigDecimal balance = AmountGenerator.randomAmount(AmountGenerator.MIN_AMOUNT, AmountGenerator.MAX_TRANSFER);
        // сумма допустимая и не больше баланса, ошибка должна быть только из-за счёта получателя
        BigDecimal amount = AmountGenerator.randomAmount(AmountGenerator.MIN_AMOUNT, balance);
        fundAccount(senderId, balance);
        transferExpectingError(senderId, RandomData.getNonExistentAccountId(), amount, ErrorMessages.INVALID_TRANSFER);
        assertNothingWasTransferred();
        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(balance, within(BALANCE_DELTA));
    }
}