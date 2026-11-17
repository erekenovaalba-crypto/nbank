package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class CustomerAccountsRequester {
    private final RequestSpecification requestSpecification;
    private final ResponseSpecification responseSpecification;

    public CustomerAccountsRequester(RequestSpecification requestSpecification,
                                     ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}