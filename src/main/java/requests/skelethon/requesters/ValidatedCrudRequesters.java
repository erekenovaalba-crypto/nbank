package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;
public class ValidatedCrudRequesters<M extends BaseModel> extends HttpRequest implements CrudEndpointInterface {
    private final CrudRequester crudRequester;

    public ValidatedCrudRequesters(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public M post() {
        return toModel(crudRequester.post());
    }

    @Override
    public M post(BaseModel model) {
        return toModel(crudRequester.post(model));
    }

    @Override
    public M get() {
        return toModel(crudRequester.get());
    }

    @Override
    public M get(long id) {
        return toModel(crudRequester.get(id));
    }

    @Override
    public M update(BaseModel model) {
        return toModel(crudRequester.update(model));
    }

    @Override
    public M update(long id, BaseModel model) {
        return toModel(crudRequester.update(id, model));
    }

    @Override
    public Object delete(long id) {
        return crudRequester.delete(id);
    }

    @SuppressWarnings("unchecked")
    private M toModel(ValidatableResponse response) {
        return (M) response.extract().as(endpoint.getResponseModel());
    }
}
