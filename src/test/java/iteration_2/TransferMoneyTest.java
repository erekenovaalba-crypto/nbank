/*
package iteration_2;

import models.ErrorMessage;
import models.SuccessMessage;
import models.TransactionType;
import generators.AmountGenerator;
import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.CustomerProfile;
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
import requests.CreateAccountRequester;
import requests.DepositRequester;
import requests.GetProfileRequester;
import requests.TransferRequester;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        spec = createUser();
        senderId = createAccount(spec);
        receiverId = createAccount(spec);
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
    private long createAccount(RequestSpecification userSpec) {
        return new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .post()
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
        return getTransactions(spec, accountId);
    }

    private List<TransactionData> getTransactions(RequestSpecification userSpec, long accountId) {
        TransactionData[] transactions = new AccountTransactionsRequester(userSpec, ResponseSpecs.requestResponseOK())
                .get(accountId)
                .extract().as(TransactionData[].class);
        return Arrays.asList(transactions);
    }

    private BigDecimal getBalance(long accountId) {
        return getBalance(spec, accountId);
    }

    // баланс берём из профиля: GET /api/v1/customer/profile возвращает счета пользователя
    private BigDecimal getBalance(RequestSpecification userSpec, long accountId) {
        return new GetProfileRequester(userSpec, ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CustomerProfile.class)
                .getAccounts()
                .stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Счёт " + accountId + " не найден у пользователя"))
                .getBalance();
    }

    private TransactionData singleTransaction(List<TransactionData> transactions, TransactionType type) {
        List<TransactionData> filtered = transactions.stream()
                .filter(t -> type.name().equals(t.getType()))
                .toList();
        assertThat(filtered)
                .as("Ожидалась ровно одна транзакция " + type.name() + ", есть: " + transactions)
                .hasSize(1);
        return filtered.getFirst();
    }

    private void transferExpectingError(long from, long to, BigDecimal amount, ErrorMessage errorMessage) {
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
                .as("У отправителя не должно быть " + TransactionType.TRANSFER_OUT)
                .doesNotContain(TransactionType.TRANSFER_OUT.name());
        softly.assertThat(getTransactions(receiverId))
                .as("У получателя не должно быть транзакций")
                .isEmpty();
    }

    // Позитивный кейс: граничные суммы и случайная сумма из допустимого диапазона
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
        softly.assertThat(response.getMessage()).isEqualTo(SuccessMessage.TRANSFER_SUCCESSFUL.getMessage());

        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя")
                .isCloseTo(senderBefore.subtract(amount), within(BALANCE_DELTA));
        softly.assertThat(getBalance(receiverId))
                .as("Баланс получателя")
                .isCloseTo(receiverBefore.add(amount), within(BALANCE_DELTA));

        TransactionData out = singleTransaction(getTransactions(senderId), TransactionType.TRANSFER_OUT);
        softly.assertThat(out.getId()).isNotNull().isPositive();
        softly.assertThat(out.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(out.getRelatedAccountId()).isEqualTo((int) receiverId);
        softly.assertThat(out.getTimestamp()).isNotNull();

        TransactionData in = singleTransaction(getTransactions(receiverId), TransactionType.TRANSFER_IN);
        softly.assertThat(in.getId()).isNotNull().isPositive();
        softly.assertThat(in.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(in.getRelatedAccountId()).isEqualTo((int) senderId);
        softly.assertThat(in.getTimestamp()).isNotNull();
    }

    // Негативный кейс: граничные значения (лимит + 0.01 и ноль) и случайные суммы вне диапазона
    public static Stream<Arguments> invalidTransferAmounts() {
        return Stream.of(
                Arguments.of(AmountGenerator.MAX_TRANSFER.add(AmountGenerator.MIN_AMOUNT),
                        ErrorMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(BigDecimal.ZERO, ErrorMessage.TRANSFER_AMOUNT_TOO_SMALL),
                Arguments.of(AmountGenerator.amountAbove(AmountGenerator.MAX_TRANSFER),
                        ErrorMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(AmountGenerator.negativeAmount(AmountGenerator.MAX_TRANSFER),
                        ErrorMessage.TRANSFER_AMOUNT_TOO_SMALL));
    }

    @MethodSource("invalidTransferAmounts")
    @ParameterizedTest
    public void userCannotTransferInvalidAmount(BigDecimal amount, ErrorMessage errorMessage) {
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

        transferExpectingError(senderId, receiverId, amount, ErrorMessage.INVALID_TRANSFER);

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

        transferExpectingError(senderId, RandomData.getNonExistentAccountId(), amount, ErrorMessage.INVALID_TRANSFER);

        assertNothingWasTransferred();
        softly.assertThat(getBalance(senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(balance, within(BALANCE_DELTA));
    }
}*/
