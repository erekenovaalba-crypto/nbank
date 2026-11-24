package requests.steps;

import generators.AmountGenerator;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.*;
import org.hamcrest.Matchers;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UserSteps {
    private static final int SINGLE_TRANSACTION = 1;


    // спецификация запросов от имени пользователя (токен кэшируется в RequestSpecs)
    private static RequestSpecification spec(CreateUserRequest user) {
        return RequestSpecs.authAsUser(user.getUsername(), user.getPassword());
    }

    // запрос, который должен завершиться успехом, ответ разбирается в модель M
    private static <M extends BaseModel> ValidatedCrudRequesters<M> validated(
            CreateUserRequest user, Endpoint endpoint, ResponseSpecification responseSpec) {
        return new ValidatedCrudRequesters<>(spec(user), endpoint, responseSpec);
    }

    private static <M extends BaseModel> ValidatedCrudRequesters<M> ok(CreateUserRequest user, Endpoint endpoint) {
        return validated(user, endpoint, ResponseSpecs.requestResponseOK());
    }

    // запрос с любой спецификацией ответа: для ошибок и для чтения списков
    private static CrudRequester requester(CreateUserRequest user, Endpoint endpoint,
                                           ResponseSpecification responseSpec) {
        return new CrudRequester(spec(user), endpoint, responseSpec);
    }

    // ---------- авторизация ----------

    // логин: возвращает значение заголовка Authorization
    public static String login(String username, String password) {
        return new CrudRequester(RequestSpecs.unauthenticSpec(), Endpoint.LOGIN, ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(username).password(password).build())
                .header(AuthConstants.HEADER_AUTHORIZATION, Matchers.notNullValue())
                .extract().header(AuthConstants.HEADER_AUTHORIZATION);
    }

    public static String login(CreateUserRequest user) {
        return login(user.getUsername(), user.getPassword());
    }

    // спецификация запросов с уже полученным токеном
    public static RequestSpecification specWithToken(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(RequestSpecs.unauthenticSpec())
                .addHeader(AuthConstants.HEADER_AUTHORIZATION, token)
                .build();
    }

    // ---------- счета ----------

    public static CreateAccountResponse createAccount(CreateUserRequest user) {
        return UserSteps.<CreateAccountResponse>validated(
                        user, Endpoint.CREATE_ACCOUNT, ResponseSpecs.entityWasCreated())
                .post();
    }

    public static CustomerProfile getProfile(CreateUserRequest user) {
        return getProfile(spec(user));
    }

    // профиль с произвольной авторизацией (например, по токену)
    public static CustomerProfile getProfile(RequestSpecification spec) {
        return new CrudRequester(spec, Endpoint.CUSTOMER_PROFILE, ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CustomerProfile.class);
    }

    public static Optional<CreateAccountResponse> findAccount(CustomerProfile profile, long accountId) {
        return profile.getAccounts().stream()
                .filter(account -> account.getId() == accountId)
                .findFirst();
    }

    // счёт из профиля, то есть его фактическое состояние в системе
    public static CreateAccountResponse getAccount(CreateUserRequest user, long accountId) {
        return findAccount(getProfile(user), accountId)
                .orElseThrow(() -> new AssertionError("Счёт " + accountId + " не найден у пользователя"));
    }

    public static BigDecimal getBalance(CreateUserRequest user, long accountId) {
        return getAccount(user, accountId).getBalance();
    }

    // ---------- депозит ----------

    public static CreateAccountResponse deposit(CreateUserRequest user, DepositRequest request) {
        return UserSteps.<CreateAccountResponse>ok(user, Endpoint.DEPOSIT).post(request);
    }

    public static void depositExpectingError(CreateUserRequest user, DepositRequest request,
                                             ResponseSpecification responseSpec) {
        requester(user, Endpoint.DEPOSIT, responseSpec).post(request);
    }

    // максимум за один депозит ограничен, поэтому большие суммы вносим частями
    public static void fundAccount(CreateUserRequest user, long accountId, BigDecimal amount) {
        BigDecimal rest = amount;
        while (rest.signum() > 0) {
            BigDecimal chunk = rest.min(AmountGenerator.MAX_DEPOSIT);
            deposit(user, DepositRequest.builder().id(accountId).balance(chunk).build());
            rest = rest.subtract(chunk);
        }
    }

    // ---------- перевод ----------

    public static TransferResponse transfer(CreateUserRequest user, TransferRequest request) {
        return UserSteps.<TransferResponse>ok(user, Endpoint.TRANSFER).post(request);
    }

    public static void transferExpectingError(CreateUserRequest user, TransferRequest request,
                                              ResponseSpecification responseSpec) {
        requester(user, Endpoint.TRANSFER, responseSpec).post(request);
    }

    // ---------- транзакции ----------

    public static List<TransactionData> getTransactions(CreateUserRequest user, long accountId) {
        TransactionData[] transactions = requester(
                user, Endpoint.ACCOUNT_TRANSACTIONS, ResponseSpecs.requestResponseOK())
                .get(accountId)
                .extract().as(TransactionData[].class);
        return Arrays.asList(transactions);
    }

    public static void getTransactionsExpectingError(CreateUserRequest user, long accountId,
                                                     ResponseSpecification responseSpec) {
        requester(user, Endpoint.ACCOUNT_TRANSACTIONS, responseSpec).get(accountId);
    }

    // единственная транзакция нужного типа; если их не одна, шаг падает с понятным сообщением
    public static TransactionData singleTransaction(List<TransactionData> transactions, TransactionType type) {
        List<TransactionData> filtered = transactions.stream()
                .filter(transaction -> type.name().equals(transaction.getType()))
                .toList();
        if (filtered.size() != SINGLE_TRANSACTION) {
            throw new AssertionError("Ожидалась ровно одна транзакция " + type.name() + ", есть: " + transactions);
        }
        return filtered.getFirst();
    }

    // ---------- профиль ----------

    public static UpdateProfileResponse updateName(CreateUserRequest user, UpdateProfileRequest request) {
        return UserSteps.<UpdateProfileResponse>ok(user, Endpoint.CUSTOMER_PROFILE).update(request);
    }

    public static void updateNameExpectingError(CreateUserRequest user, UpdateProfileRequest request,
                                                ResponseSpecification responseSpec) {
        requester(user, Endpoint.CUSTOMER_PROFILE, responseSpec).update(request);
    }
}
