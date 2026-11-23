package iteration_2;

import generators.AmountGenerator;
import iteration_1.BaseTest;
import models.CreateAccountResponse;
import models.DepositRequest;
import models.ErrorMessage;
import models.TransactionData;
import models.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountTransactionsRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import io.restassured.specification.RequestSpecification;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DepositMoneyTest extends BaseTest {

    private RequestSpecification userSpec;
    private long accountId;

    @BeforeEach
    public void setupUserAndAccount() {
        var userRequest = AdminSteps.createUser();
        userSpec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());

        CreateAccountResponse account = new CrudRequester(
                userSpec, Endpoint.CREATE_ACCOUNT, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().as(CreateAccountResponse.class);
        accountId = account.getId();
    }

    private List<TransactionData> getTransactions(long id) {
        TransactionData[] transactions = new AccountTransactionsRequester(
                userSpec, ResponseSpecs.requestResponseOK())
                .get(id)
                .extract().as(TransactionData[].class);
        return Arrays.asList(transactions);
    }

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
        CreateAccountResponse response = new CrudRequester(
                userSpec, Endpoint.DEPOSIT, ResponseSpecs.requestResponseOK())
                .post(DepositRequest.builder().id(accountId).balance(amount).build())
                .extract().as(CreateAccountResponse.class);

        softly.assertThat(response.getId()).isEqualTo(accountId);
        softly.assertThat(response.getBalance()).isEqualByComparingTo(amount);
        softly.assertThat(response.getTransactions()).hasSize(1);

        TransactionData deposit = response.getTransactions().getFirst();
        softly.assertThat(deposit.getId()).isNotNull().isPositive();
        softly.assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT.name());
        softly.assertThat(deposit.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(deposit.getRelatedAccountId()).isEqualTo((int) accountId);
        softly.assertThat(deposit.getTimestamp()).isNotBlank();

        List<TransactionData> history = getTransactions(accountId);
        softly.assertThat(history).hasSize(1);
        softly.assertThat(history.getFirst())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(deposit);
    }

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
    public void userCannotDepositInvalidAmount(BigDecimal amount, ErrorMessage error) {
        new CrudRequester(userSpec, Endpoint.DEPOSIT,
                ResponseSpecs.requestResponseBadRequestWithMessage(error))
                .post(DepositRequest.builder().id(accountId).balance(amount).build());

        softly.assertThat(getTransactions(accountId))
                .as("Неуспешный депозит не должен создавать транзакцию")
                .isEmpty();
    }

    @Test
    public void userCannotDepositIntoForeignAccount() {
        var ownerRequest = AdminSteps.createUser();
        RequestSpecification ownerSpec = RequestSpecs.authAsUser(
                ownerRequest.getUsername(), ownerRequest.getPassword());
        CreateAccountResponse ownerAccount = new CrudRequester(
                ownerSpec, Endpoint.CREATE_ACCOUNT, ResponseSpecs.entityWasCreated())
                .post(null).extract().as(CreateAccountResponse.class);

        new CrudRequester(userSpec, Endpoint.DEPOSIT,
                ResponseSpecs.requestResponseForbidden(ErrorMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT))
                .post(DepositRequest.builder()
                        .id(ownerAccount.getId())
                        .balance(AmountGenerator.validDepositAmount())
                        .build());

        new AccountTransactionsRequester(userSpec,
                ResponseSpecs.requestResponseForbidden(ErrorMessage.NO_PERMISSION_TO_ACCOUNT))
                .get(ownerAccount.getId());

        TransactionData[] ownerHistory = new AccountTransactionsRequester(
                ownerSpec, ResponseSpecs.requestResponseOK())
                .get(ownerAccount.getId()).extract().as(TransactionData[].class);
        softly.assertThat(ownerHistory).isEmpty();
    }
}
