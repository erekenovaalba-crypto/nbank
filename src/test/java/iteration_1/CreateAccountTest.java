package iteration_1;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import org.junit.jupiter.api.Test;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequesters;
import requests.steps.AdminSteps;
import spec.RequestSpecs;
import spec.ResponseSpecs;

public class CreateAccountTest extends BaseTest{
   @Test
    public  void userCanCreateAccountTest(){
       CreateUserRequest userRequest = AdminSteps.createUser();
      new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
              Endpoint.CREATE_ACCOUNT,
              ResponseSpecs.entityWasCreated())
              .post(null);
    }

}
