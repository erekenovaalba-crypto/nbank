package spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import models.ErrorMessage;
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
    // только статус-код; тело ошибки читается в шаге через десериализацию, без JSONPath
    public static ResponseSpecification requestResponseBadRequest(){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .build();
    }
    // ошибка приходит обычным текстом, а не JSON, поэтому сравниваем всё тело
    public static ResponseSpecification requestResponseBadRequestWithMessage(ErrorMessage errorMessage){
        return plainTextError(HttpStatus.SC_BAD_REQUEST, errorMessage);
    }
    public static ResponseSpecification requestResponseForbidden(ErrorMessage errorMessage){
        return plainTextError(HttpStatus.SC_FORBIDDEN, errorMessage);
    }
    private static ResponseSpecification plainTextError(int statusCode, ErrorMessage errorMessage){
        return  defaultResponseBuilder()
                .expectStatusCode(statusCode)
                .expectBody(Matchers.equalTo(errorMessage.getMessage()))
                .build();
    }
}
