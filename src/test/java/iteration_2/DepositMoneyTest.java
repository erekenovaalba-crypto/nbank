package iteration_2;

import models.comparison.ModelAssertions;
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

    // новый счёт после неудачной операции должен остаться пустым: ни транзакций, ни денег
    private void assertAccountIsUntouched(CreateUserRequest owner, long id) {
        softly.assertThat(UserSteps.getTransactions(owner, id))
                .as("Транзакций быть не должно")
                .isEmpty();
        softly.assertThat(UserSteps.getBalance(owner, id))
                .as("Баланс не должен измениться")
                .isEqualByComparingTo(BigDecimal.ZERO);
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

        // проверка, что одна транзакция
        TransactionData deposit = UserSteps.singleTransaction(response.getTransactions(), TransactionType.DEPOSIT);
        softly.assertThat(deposit.getId()).isNotNull().isPositive();
        softly.assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT.name());
        ModelAssertions.assertThatModels(request, deposit).match();
        softly.assertThat(deposit.getTimestamp()).isNotNull();

        // GET: баланс счёта в профиле изменился
        ModelAssertions.assertThatModels(request, UserSteps.getAccount(user, accountId)).match();

        // транзакция сохранилась в истории счёта
        List<TransactionData> stored = UserSteps.getTransactions(user, accountId);
        softly.assertThat(UserSteps.singleTransaction(stored, TransactionType.DEPOSIT))
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

        // GET
        assertAccountIsUntouched(user, accountId);
    }

    // Негативный кейс: депозит на чужой счёт
    @Test
    public void userCannotDepositIntoForeignAccount() {
        CreateUserRequest owner = AdminSteps.createUser();
        long ownerAccountId = UserSteps.createAccount(owner).getId();

        UserSteps.depositExpectingError(user,
                depositRequest(ownerAccountId, AmountGenerator.validDepositAmount()),
                ResponseSpecs.requestResponseForbidden(ErrorMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT));

        // историю чужого счёта читать нельзя
        UserSteps.getTransactionsExpectingError(user, ownerAccountId,
                ResponseSpecs.requestResponseForbidden(ErrorMessage.NO_PERMISSION_TO_ACCOUNT));

        // у владельца счёта ничего не изменилось
        assertAccountIsUntouched(owner, ownerAccountId);
    }

    // Негативный кейс: депозит на несуществующий счёт
    @Test
    public void userCannotDepositIntoNonExistentAccount() {
        UserSteps.depositExpectingError(user,
                depositRequest(RandomData.getNonExistentAccountId(), AmountGenerator.validDepositAmount()),
                ResponseSpecs.requestResponseForbidden(ErrorMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT));

        // GET свои счета остались пустыми
        assertAccountIsUntouched(user, accountId);
    }
}
