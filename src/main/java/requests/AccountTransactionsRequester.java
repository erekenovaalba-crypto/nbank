
package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;
public class AccountTransactionsRequester {
    private final RequestSpecification requestSpecification;
    private final ResponseSpecification responseSpecification;

    public AccountTransactionsRequester(RequestSpecification requestSpecification,
                                        ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public ValidatableResponse get(long accountId) {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/accounts/" + accountId + "/transactions")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
