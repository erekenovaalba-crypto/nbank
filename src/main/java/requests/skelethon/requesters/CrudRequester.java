package requests.skelethon.requesters;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    // POST без тела, например создание счёта
    public ValidatableResponse post() {
        return post(null);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        RequestSpecification request = given().spec(requestSpecification);
        // пустое тело "" не отправляем: если модели нет, запрос уходит без тела
        if (model != null) {
            request = request.body(model);
        }
        return validate(request.post(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse get() {
        return validate(given().spec(requestSpecification).get(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse get(long id) {
        return validate(given().spec(requestSpecification).get(endpoint.getUrl(), id));
    }

    @Override
    public ValidatableResponse update(BaseModel model) {
        return validate(given().spec(requestSpecification).body(model).put(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse update(long id, BaseModel model) {
        return validate(given().spec(requestSpecification).body(model).put(endpoint.getUrl(), id));
    }

    @Override
    public ValidatableResponse delete(long id) {
        return validate(given().spec(requestSpecification).delete(endpoint.getUrl(), id));
    }

    private ValidatableResponse validate(Response response) {
        return response.then().assertThat().spec(responseSpecification);
    }
}
