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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositMoneyTest extends BaseTest {

    private CreateUserRequest user;
    private long accountId;

    // у каждого теста свой новый пользователь и свой пустой счёт
    @BeforeEach
    public void setupUser() {
        user = AdminSteps.createUser();
        accountId = UserSteps.createAccount(user).getId();
    }

    private DepositRequest depositRequest(long id, BigDecimal amount) {
        return DepositRequest.builder().id(id).balance(amount).build();
    }

    // Позитивный кейс: граничные суммы и случайная сумма из допустимого диапазона
    public static Stream<Arguments> validDepositAmounts() {
        return Stream.of(
                Arguments.of(AmountGenerator.MIN_AMOUNT),
                Arguments.of(AmountGenerator.MAX_DEPOSIT.subtract(AmountGenerator.MIN_AMOUNT)),
                Arguments.of(AmountGenerator.MAX_DEPOSIT),
                Arguments.of(AmountGenerator.validDepositAmount()));
    }

    @MethodSource("validDepositAmounts")
    @ParameterizedTest
    public void userCanDepositMoneyIntoOwnAccount(BigDecimal amount) {
        DepositRequest request = depositRequest(accountId, amount);

        CreateAccountResponse response = UserSteps.deposit(user, request);

        ModelAssertions.assertThatModels(request, response).match();

        // счёт новый, значит транзакция должна быть ровно одна
        assertThat(response.getTransactions()).hasSize(1);
        TransactionData deposit = response.getTransactions().get(0);
        softly.assertThat(deposit.getId()).isNotNull().isPositive();
        softly.assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT.name());
        softly.assertThat(deposit.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(deposit.getRelatedAccountId()).isEqualTo((int) accountId);
        softly.assertThat(deposit.getTimestamp()).isNotNull();

        // фактическое состояние через GET: баланс счёта в профиле изменился
        ModelAssertions.assertThatModels(request, UserSteps.getAccount(user, accountId)).match();

        // и транзакция сохранилась в истории счёта
        List<TransactionData> stored = UserSteps.getTransactions(user, accountId);
        assertThat(stored).hasSize(1);
        softly.assertThat(stored.get(0))
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(deposit);
    }

    // Негативный кейс: граничные значения (лимит + 0.01 и ноль)
    // и случайные суммы вне допустимого диапазона
    public static Stream<Arguments> invalidDepositAmounts() {
        return Stream.of(
                Arguments.of(AmountGenerator.MAX_DEPOSIT.add(AmountGenerator.MIN_AMOUNT),
                        ErrorMessage.DEPOSIT_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(BigDecimal.ZERO, ErrorMessage.DEPOSIT_AMOUNT_TOO_SMALL),
                Arguments.of(AmountGenerator.amountAbove(AmountGenerator.MAX_DEPOSIT),
                        ErrorMessage.DEPOSIT_AMOUNT_EXCEEDS_LIMIT),
                Arguments.of(AmountGenerator.negativeAmount(AmountGenerator.MAX_DEPOSIT),
                        ErrorMessage.DEPOSIT_AMOUNT_TOO_SMALL));
    }

    @MethodSource("invalidDepositAmounts")
    @ParameterizedTest
    public void userCannotDepositInvalidAmount(BigDecimal amount, ErrorMessage errorMessage) {
        UserSteps.depositExpectingError(user, depositRequest(accountId, amount),
                ResponseSpecs.requestResponseBadRequestWithMessage(errorMessage));

        // статус 400 ещё не значит, что деньги не зачислены: проверяем фактическое состояние через GET
        softly.assertThat(UserSteps.getTransactions(user, accountId))
                .as("После неудачного депозита транзакций быть не должно")
                .isEmpty();
        softly.assertThat(UserSteps.getBalance(user, accountId))
                .as("Баланс не должен измениться")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Негативный кейс: депозит на чужой счёт
    @Test
    public void userCannotDepositIntoForeignAccount() {
        CreateUserRequest owner = AdminSteps.createUser();
        long ownerAccountId = UserSteps.createAccount(owner).getId();

        UserSteps.depositExpectingError(user,
                depositRequest(ownerAccountId, AmountGenerator.validDepositAmount()),
                ResponseSpecs.requestResponseForbidden(ErrorMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT));

        // историю чужого счёта тоже читать нельзя
        UserSteps.getTransactionsExpectingError(user, ownerAccountId,
                ResponseSpecs.requestResponseForbidden(ErrorMessage.NO_PERMISSION_TO_ACCOUNT));

        // а у владельца счёта ничего не изменилось
        softly.assertThat(UserSteps.getTransactions(owner, ownerAccountId)).isEmpty();
        softly.assertThat(UserSteps.getBalance(owner, ownerAccountId)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Негативный кейс: депозит на несуществующий счёт
    @Test
    public void userCannotDepositIntoNonExistentAccount() {
        UserSteps.depositExpectingError(user,
                depositRequest(RandomData.getNonExistentAccountId(), AmountGenerator.validDepositAmount()),
                ResponseSpecs.requestResponseForbidden(ErrorMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT));

        // свои счета остались пустыми
        softly.assertThat(UserSteps.getTransactions(user, accountId)).isEmpty();
        softly.assertThat(UserSteps.getBalance(user, accountId)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
