package requests.steps;

import generators.RandomModelGenerator;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.CreateUserResponse;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import spec.RequestSpecs;
import spec.ResponseSpecs;

public class AdminSteps {
    public static CreateUserRequest createUser(){
        CreateUserRequest userRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);
        //создание пользователя админом
       new ValidatedCrudRequesters<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);
       return userRequest;
    }
}
