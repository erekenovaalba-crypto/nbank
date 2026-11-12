package spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    private ResponseSpecs(){}
    private static ResponseSpecBuilder defaultResponseBuilder(){
        return  new ResponseSpecBuilder();
    }
    public static ResponseSpecification entityWasCreated(){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }
    public static ResponseSpecification requestResponseOK(){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }
    public static ResponseSpecification requestResponseBadRequest(String errorKey, String errorValue){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                .build();
    }
    public static ResponseSpecification requestResponseBadRequestWithMessage(String message){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo(message))
                .build();
    }
    public static ResponseSpecification requestResponseForbidden(String message){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(Matchers.equalTo(message))
                .build();
    }
}
