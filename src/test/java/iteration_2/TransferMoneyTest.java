package iteration_2;

import generators.AmountGenerator;
import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration_1.BaseTest;
import models.CreateAccountResponse;
import models.CustomerProfile;
import models.DepositRequest;
import models.ErrorMessage;
import models.SuccessMessage;
import models.TransactionData;
import models.TransactionType;
import models.TransferRequest;
import models.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountTransactionsRequester;
import requests.GetProfileRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

public class TransferMoneyTest extends BaseTest {

    private static final BigDecimal BALANCE_ABOVE_TRANSFER_LIMIT =
            AmountGenerator.MAX_TRANSFER.add(AmountGenerator.MAX_DEPOSIT);
    private static final BigDecimal BALANCE_DELTA = new BigDecimal("0.005");

    private RequestSpecification spec;
    private long senderId;
    private long receiverId;

    @BeforeEach
    public void setupUserWithTwoAccounts() {
        var userRequest = AdminSteps.createUser();
        spec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());
        senderId = createAccount();
        receiverId = createAccount();
    }

    private long createAccount() {
        return new CrudRequester(spec, Endpoint.CREATE_ACCOUNT, ResponseSpecs.entityWasCreated())
                .post(null).extract().as(CreateAccountResponse.class).getId();
    }

    private void fundAccount(long accountId, BigDecimal amount) {
        BigDecimal rest = amount;
        while (rest.signum() > 0) {
            BigDecimal chunk = rest.min(AmountGenerator.MAX_DEPOSIT);
            new CrudRequester(spec, Endpoint.DEPOSIT, ResponseSpecs.requestResponseOK())
                    .post(DepositRequest.builder().id(accountId).balance(chunk).build());
            rest = rest.subtract(chunk);
        }
    }

    private List<TransactionData> getTransactions(long accountId) {
        TransactionData[] transactions = new AccountTransactionsRequester(
                spec, ResponseSpecs.requestResponseOK()).get(accountId)
                .extract().as(TransactionData[].class);
        return Arrays.asList(transactions);
    }

    private BigDecimal getBalance(long accountId) {
        return new GetProfileRequester(spec, ResponseSpecs.requestResponseOK())
                .get().extract().as(CustomerProfile.class)
                .getAccounts().stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Счёт не найден: " + accountId))
                .getBalance();
    }

    private TransactionData singleTransaction(List<TransactionData> transactions, TransactionType type) {
        List<TransactionData> filtered = transactions.stream()
                .filter(transaction -> type.name().equals(transaction.getType()))
                .toList();
        softly.assertThat(filtered)
                .as("Ожидалась ровно одна транзакция " + type)
                .hasSize(1);
        return filtered.getFirst();
    }

    private void transferExpectingError(long from, long to, BigDecimal amount, ErrorMessage error) {
        new CrudRequester(spec, Endpoint.TRANSFER,
                ResponseSpecs.requestResponseBadRequestWithMessage(error))
                .post(TransferRequest.builder()
                        .senderAccountId(from).receiverAccountId(to).amount(amount).build());
    }

    private void assertNoTransfer() {
        softly.assertThat(getTransactions(senderId))
                .extracting(TransactionData::getType)
                .doesNotContain(TransactionType.TRANSFER_OUT.name());
        softly.assertThat(getTransactions(receiverId)).isEmpty();
    }

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

        TransferResponse response = new CrudRequester(spec, Endpoint.TRANSFER,
                ResponseSpecs.requestResponseOK())
                .post(TransferRequest.builder()
                        .senderAccountId(senderId).receiverAccountId(receiverId).amount(amount).build())
                .extract().as(TransferResponse.class);

        softly.assertThat(response.getSenderAccountId()).isEqualTo(senderId);
        softly.assertThat(response.getReceiverAccountId()).isEqualTo(receiverId);
        softly.assertThat(response.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(response.getMessage())
                .isEqualTo(SuccessMessage.TRANSFER_SUCCESSFUL.getMessage());

        softly.assertThat(getBalance(senderId))
                .isCloseTo(senderBefore.subtract(amount), within(BALANCE_DELTA));
        softly.assertThat(getBalance(receiverId))
                .isCloseTo(receiverBefore.add(amount), within(BALANCE_DELTA));

        TransactionData outgoing = singleTransaction(getTransactions(senderId), TransactionType.TRANSFER_OUT);
        softly.assertThat(outgoing.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(outgoing.getRelatedAccountId()).isEqualTo((int) receiverId);
        softly.assertThat(outgoing.getTimestamp()).isNotBlank();

        TransactionData incoming = singleTransaction(getTransactions(receiverId), TransactionType.TRANSFER_IN);
        softly.assertThat(incoming.getAmount()).isEqualByComparingTo(amount);
        softly.assertThat(incoming.getRelatedAccountId()).isEqualTo((int) senderId);
        softly.assertThat(incoming.getTimestamp()).isNotBlank();
    }

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
    public void userCannotTransferInvalidAmount(BigDecimal amount, ErrorMessage error) {
        fundAccount(senderId, BALANCE_ABOVE_TRANSFER_LIMIT);
        transferExpectingError(senderId, receiverId, amount, error);
        assertNoTransfer();
        softly.assertThat(getBalance(senderId))
                .isCloseTo(BALANCE_ABOVE_TRANSFER_LIMIT, within(BALANCE_DELTA));
        softly.assertThat(getBalance(receiverId))
                .isCloseTo(BigDecimal.ZERO, within(BALANCE_DELTA));
    }

    @Test
    public void userCannotTransferMoreThanAvailableBalance() {
        BigDecimal balance = AmountGenerator.randomAmount(
                AmountGenerator.MIN_AMOUNT,
                AmountGenerator.MAX_TRANSFER.subtract(AmountGenerator.MIN_AMOUNT));
        BigDecimal amount = AmountGenerator.randomAmount(
                balance.add(AmountGenerator.MIN_AMOUNT), AmountGenerator.MAX_TRANSFER);
        fundAccount(senderId, balance);

        transferExpectingError(senderId, receiverId, amount, ErrorMessage.INVALID_TRANSFER);
        assertNoTransfer();
        softly.assertThat(getBalance(senderId)).isCloseTo(balance, within(BALANCE_DELTA));
    }

    @Test
    public void userCannotTransferToNonExistentAccount() {
        BigDecimal balance = AmountGenerator.randomAmount(
                AmountGenerator.MIN_AMOUNT, AmountGenerator.MAX_TRANSFER);
        BigDecimal amount = AmountGenerator.randomAmount(AmountGenerator.MIN_AMOUNT, balance);
        fundAccount(senderId, balance);

        transferExpectingError(senderId, RandomData.getNonExistentAccountId(),
                amount, ErrorMessage.INVALID_TRANSFER);
        assertNoTransfer();
        softly.assertThat(getBalance(senderId)).isCloseTo(balance, within(BALANCE_DELTA));
    }
}
