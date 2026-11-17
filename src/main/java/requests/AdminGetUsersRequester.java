package requests;

import requests.skelethon.Endpoint;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

// ВНИМАНИЕ: эндпоинт GET /api/v1/admin/users я предположила, сверьте его со Swagger
public class AdminGetUsersRequester {
    private final RequestSpecification requestSpecification;
    private final ResponseSpecification responseSpecification;

    public AdminGetUsersRequester(RequestSpecification requestSpecification,
                                  ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(Endpoint.ADMIN_USERS.getPath())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
