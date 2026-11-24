package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UpdateProfileRequest;

import static io.restassured.RestAssured.given;

// Не наследуется от Request<T>: у него метод называется post, а профиль обновляется через PUT
public class UpdateProfileRequester {
    private final RequestSpecification requestSpecification;
    private final ResponseSpecification responseSpecification;

    public UpdateProfileRequester(RequestSpecification requestSpecification,
                                  ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public ValidatableResponse put(UpdateProfileRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
