package requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.*;
@Getter
@AllArgsConstructor
public enum Endpoint {

    LOGIN("/auth/login" , LoginUserRequest.class, LoginUserResponse.class),
    ADMIN_USER("/admin/users", CreateUserRequest.class, CreateUserResponse.class),
    CREATE_ACCOUNT("/accounts", BaseModel.class, CreateAccountResponse.class ),
    DEPOSIT("/accounts/deposit", DepositRequest.class, BaseModel.class),
    TRANSFER("/accounts/transfer", TransferRequest.class, TransferResponse.class),
    ACCOUNT_TRANSACTIONS("/accounts/{accountId}/transactions", BaseModel.class, TransactionData.class),
    CUSTOMER_PROFILE("/customer/profile", UpdateProfileRequest.class, UpdateProfileResponse.class);


    private  final String url;
    private  final Class<? extends BaseModel> requestModel;
    private  final Class<? extends BaseModel> responseModel;

}
