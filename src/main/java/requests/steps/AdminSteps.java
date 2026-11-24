package requests.steps;

import generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Общие шаги от имени админа.
// В тесте достаточно написать: CreateUserRequest userRequest = AdminSteps.createUser();
public class AdminSteps {

    // создаёт пользователя (роль USER) со случайными данными и возвращает запрос, по которому он создан:
    // по нему можно авторизоваться и вызывать шаги UserSteps
    public static CreateUserRequest createUser() {
        CreateUserRequest request = RandomModelGenerator.generate(CreateUserRequest.class);
        // шаги UserSteps работают от имени обычного пользователя, поэтому роль всегда USER
        request.setRole(UserRole.USER.name());
        createUser(request);
        return request;
    }

    // создаёт пользователя по готовому запросу, ждёт 201 и возвращает ответ
    public static CreateUserResponse createUser(CreateUserRequest request) {
        return new ValidatedCrudRequesters<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(request);
    }

    // создание, которое должно завершиться ошибкой: ожидаемый ответ задаётся спецификацией
    public static void createUserExpectingError(CreateUserRequest request, ResponseSpecification responseSpec) {
        new CrudRequester(RequestSpecs.adminSpec(), Endpoint.ADMIN_USER, responseSpec)
                .post(request);
    }

    public static List<CreateUserResponse> getUsers() {
        return getUsers(RequestSpecs.adminSpec());
    }

    // список пользователей с произвольной авторизацией (например, по токену)
    public static List<CreateUserResponse> getUsers(RequestSpecification spec) {
        CreateUserResponse[] users = new CrudRequester(spec, Endpoint.ADMIN_USER, ResponseSpecs.requestResponseOK())
                .get()
                .extract().as(CreateUserResponse[].class);
        return Arrays.asList(users);
    }

    public static Optional<CreateUserResponse> findUser(long id) {
        return getUsers().stream()
                .filter(user -> user.getId() == id)
                .findFirst();
    }
}
