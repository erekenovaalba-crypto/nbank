package iteration_2;

import comparison.ModelAssertions;
import generators.AmountGenerator;
import generators.RandomData;
import iteration_1.BaseTest;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

public class TransferMoneyTest extends BaseTest {

    // на счёте больше лимита перевода, чтобы срабатывала проверка суммы, а не нехватка средств
    private static final BigDecimal BALANCE_ABOVE_TRANSFER_LIMIT =
            AmountGenerator.MAX_TRANSFER.add(AmountGenerator.MAX_DEPOSIT);
    // сервер хранит баланс как double (например 24349.699999999983), поэтому сравниваем с допуском
    private static final BigDecimal BALANCE_DELTA = new BigDecimal("0.005");

    private CreateUserRequest user;
    private long senderId;
    private long receiverId;

    // у каждого теста свой новый пользователь с двумя пустыми счетами
    @BeforeEach
    public void setupUserWithTwoAccounts() {
        user = AdminSteps.createUser();
        senderId = UserSteps.createAccount(user).getId();
        receiverId = UserSteps.createAccount(user).getId();
    }

    private TransferRequest transferRequest(long from, long to, BigDecimal amount) {
        return TransferRequest.builder()
                .senderAccountId(from)
                .receiverAccountId(to)
                .amount(amount)
                .build();
    }

    private void assertNothingWasTransferred() {
        softly.assertThat(UserSteps.getTransactions(user, senderId))
                .extracting(TransactionData::getType)
                .as("У отправителя не должно быть " + TransactionType.TRANSFER_OUT)
                .doesNotContain(TransactionType.TRANSFER_OUT.name());
        softly.assertThat(UserSteps.getTransactions(user, receiverId))
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
        UserSteps.fundAccount(user, senderId, amount);
        BigDecimal senderBefore = UserSteps.getBalance(user, senderId);
        BigDecimal receiverBefore = UserSteps.getBalance(user, receiverId);
        TransferRequest request = transferRequest(senderId, receiverId, amount);

        TransferResponse response = UserSteps.transfer(user, request);

        ModelAssertions.assertThatModels(request, response).match();
        softly.assertThat(response.getMessage()).isEqualTo(SuccessMessage.TRANSFER_SUCCESSFUL.getMessage());

        // фактическое состояние через GET: балансы обоих счетов и транзакции
        softly.assertThat(UserSteps.getBalance(user, senderId))
                .as("Баланс отправителя")
                .isCloseTo(senderBefore.subtract(amount), within(BALANCE_DELTA));
        softly.assertThat(UserSteps.getBalance(user, receiverId))
                .as("Баланс получателя")
                .isCloseTo(receiverBefore.add(amount), within(BALANCE_DELTA));

        TransactionData out = UserSteps.singleTransaction(
                UserSteps.getTransactions(user, senderId), TransactionType.TRANSFER_OUT);
        softly.assertThat(out.getId()).isNotNull().isPositive();
        softly.assertThat(out.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(out.getRelatedAccountId()).isEqualTo((int) receiverId);
        softly.assertThat(out.getTimestamp()).isNotNull();

        TransactionData in = UserSteps.singleTransaction(
                UserSteps.getTransactions(user, receiverId), TransactionType.TRANSFER_IN);
        softly.assertThat(in.getId()).isNotNull().isPositive();
        softly.assertThat(in.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(in.getRelatedAccountId()).isEqualTo((int) senderId);
        softly.assertThat(in.getTimestamp()).isNotNull();
    }

    // Позитивный кейс: перевод на счёт другого пользователя
    @Test
    public void userCanTransferMoneyToAnotherUsersAccount() {
        CreateUserRequest otherUser = AdminSteps.createUser();
        long otherAccountId = UserSteps.createAccount(otherUser).getId();
        BigDecimal amount = AmountGenerator.validTransferAmount();
        UserSteps.fundAccount(user, senderId, amount);
        BigDecimal senderBefore = UserSteps.getBalance(user, senderId);
        BigDecimal otherBefore = UserSteps.getBalance(otherUser, otherAccountId);
        TransferRequest request = transferRequest(senderId, otherAccountId, amount);

        TransferResponse response = UserSteps.transfer(user, request);

        ModelAssertions.assertThatModels(request, response).match();
        softly.assertThat(response.getMessage()).isEqualTo(SuccessMessage.TRANSFER_SUCCESSFUL.getMessage());

        softly.assertThat(UserSteps.getBalance(user, senderId))
                .as("Баланс отправителя")
                .isCloseTo(senderBefore.subtract(amount), within(BALANCE_DELTA));
        softly.assertThat(UserSteps.getBalance(otherUser, otherAccountId))
                .as("Баланс получателя (другого пользователя)")
                .isCloseTo(otherBefore.add(amount), within(BALANCE_DELTA));

        TransactionData out = UserSteps.singleTransaction(
                UserSteps.getTransactions(user, senderId), TransactionType.TRANSFER_OUT);
        softly.assertThat(out.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(out.getRelatedAccountId()).isEqualTo((int) otherAccountId);

        // получатель видит входящую транзакцию в своей истории
        TransactionData in = UserSteps.singleTransaction(
                UserSteps.getTransactions(otherUser, otherAccountId), TransactionType.TRANSFER_IN);
        softly.assertThat(in.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(in.getRelatedAccountId()).isEqualTo((int) senderId);
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
        UserSteps.fundAccount(user, senderId, BALANCE_ABOVE_TRANSFER_LIMIT);

        UserSteps.transferExpectingError(user, transferRequest(senderId, receiverId, amount),
                ResponseSpecs.requestResponseBadRequestWithMessage(errorMessage));

        // статус 400 ещё не значит, что деньги не ушли: проверяем фактическое состояние через GET
        assertNothingWasTransferred();
        softly.assertThat(UserSteps.getBalance(user, senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(BALANCE_ABOVE_TRANSFER_LIMIT, within(BALANCE_DELTA));
        softly.assertThat(UserSteps.getBalance(user, receiverId))
                .as("Баланс получателя не должен измениться")
                .isCloseTo(BigDecimal.ZERO, within(BALANCE_DELTA));
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
        UserSteps.fundAccount(user, senderId, balance);

        UserSteps.transferExpectingError(user, transferRequest(senderId, receiverId, amount),
                ResponseSpecs.requestResponseBadRequestWithMessage(ErrorMessage.INVALID_TRANSFER));

        assertNothingWasTransferred();
        softly.assertThat(UserSteps.getBalance(user, senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(balance, within(BALANCE_DELTA));
        softly.assertThat(UserSteps.getBalance(user, receiverId))
                .as("Баланс получателя не должен измениться")
                .isCloseTo(BigDecimal.ZERO, within(BALANCE_DELTA));
    }

    // Негативный кейс: перевод на несуществующий счёт
    @Test
    public void userCannotTransferToNonExistentAccount() {
        BigDecimal balance = AmountGenerator.randomAmount(AmountGenerator.MIN_AMOUNT, AmountGenerator.MAX_TRANSFER);
        // сумма допустимая и не больше баланса, ошибка должна быть только из-за счёта получателя
        BigDecimal amount = AmountGenerator.randomAmount(AmountGenerator.MIN_AMOUNT, balance);
        UserSteps.fundAccount(user, senderId, balance);

        UserSteps.transferExpectingError(user,
                transferRequest(senderId, RandomData.getNonExistentAccountId(), amount),
                ResponseSpecs.requestResponseBadRequestWithMessage(ErrorMessage.INVALID_TRANSFER));

        assertNothingWasTransferred();
        softly.assertThat(UserSteps.getBalance(user, senderId))
                .as("Баланс отправителя не должен измениться")
                .isCloseTo(balance, within(BALANCE_DELTA));
    }
}
