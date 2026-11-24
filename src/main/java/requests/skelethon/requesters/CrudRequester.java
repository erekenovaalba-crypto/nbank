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
    @Override
    public ValidatableResponse post() {
        return validate(request().post(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return validate(request().body(model).post(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse get() {
        return validate(request().get(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse get(long id) {
        return validate(request().get(endpoint.getUrl(), id));
    }

    @Override
    public ValidatableResponse update(BaseModel model) {
        return validate(request().body(model).put(endpoint.getUrl()));
    }

    @Override
    public ValidatableResponse update(long id, BaseModel model) {
        return validate(request().body(model).put(endpoint.getUrl(), id));
    }

    @Override
    public ValidatableResponse delete(long id) {
        return validate(request().delete(endpoint.getUrl(), id));
    }

    // каждый запрос начинается с одной и той же спецификации
    private RequestSpecification request() {
        return given().spec(requestSpecification);
    }

    private ValidatableResponse validate(Response response) {
        return response.then().assertThat().spec(responseSpecification);
    }
}
