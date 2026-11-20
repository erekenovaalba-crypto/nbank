package spec;

import configs.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.LoginUserRequest;
import org.apache.http.HttpHeaders;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestSpecs {
    private static Map<String, String> authHeaders = new HashMap<>(Map.of("admin", "Basic YWRtaW46YWRtaW4="));

    private RequestSpecs(){}
    private static RequestSpecBuilder defaultRequestBuilder(){
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(Config.getProperty("server") + Config.getProperty("apiVersion"));
    }
    public static RequestSpecification unauthenticSpec(){
        return  defaultRequestBuilder().build();
    }
    public static RequestSpecification adminSpec(){
        return  defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4=")
                .build();
    }
    public static RequestSpecification authAsUser(String username, String password){
        String userAuthHeader;
        if (!authHeaders.containsKey(username)){
            userAuthHeader =
         new CrudRequester(
                RequestSpecs.unauthenticSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestResponseOK())
                .post(LoginUserRequest.builder().username(username).password(password).build())
                .extract()
                .header(HttpHeaders.AUTHORIZATION);
        authHeaders.put(username,userAuthHeader);
        }
        else userAuthHeader = authHeaders.get(username);
        return defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, userAuthHeader).build();
    }
}
