package spec;

import configs.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.AuthConstants;
import models.LoginUserRequest;
import org.apache.http.HttpHeaders;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestSpecs {
    private static final Map<String, String> authHeaders = new HashMap<>(Map.of(AuthConstants.ADMIN_USERNAME, AuthConstants.ADMIN_AUTH_HEADER));

    private RequestSpecs(){}
    private static RequestSpecBuilder defaultRequestBuilder(){
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(Config.getProperty(Config.SERVER) + Config.getProperty(Config.API_VERSION));
    }
    public static RequestSpecification unauthenticSpec(){
        return  defaultRequestBuilder().build();
    }
    public static RequestSpecification adminSpec(){
        return  defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, AuthConstants.ADMIN_AUTH_HEADER)
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
