package spec;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import models.ErrorMessage;
import models.JsonField;
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
    // ошибка в JSON: ключ поля и список сообщений
    public static ResponseSpecification requestResponseBadRequest(JsonField errorKey, ErrorMessage errorMessage){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey.getName(), Matchers.hasItem(errorMessage.getMessage()))
                .build();
    }
    // когда текст ошибки неизвестен или не важен, проверяем только статус-код
    public static ResponseSpecification requestResponseBadRequest(){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .build();
    }
    // ошибка приходит обычным текстом, а не JSON, поэтому сравниваем всё тело
    public static ResponseSpecification requestResponseBadRequestWithMessage(ErrorMessage errorMessage){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo(errorMessage.getMessage()))
                .build();
    }
    public static ResponseSpecification requestResponseForbidden(ErrorMessage errorMessage){
        return  defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(Matchers.equalTo(errorMessage.getMessage()))
                .build();
    }
}
