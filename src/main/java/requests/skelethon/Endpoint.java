package requests.skelethon;

import lombok.Getter;

@Getter
public enum Endpoint {
    LOGIN("/api/v1/auth/login"),
    ADMIN_USERS("/api/v1/admin/users"),
    CREATE_ACCOUNT("/api/v1/accounts"),
    DEPOSIT("/api/v1/accounts/deposit"),
    TRANSFER("/api/v1/accounts/transfer"),
    ACCOUNT_TRANSACTIONS("/api/v1/accounts/{accountId}/transactions"),
    CUSTOMER_PROFILE("/api/v1/customer/profile");

    private final String path;

    Endpoint(String path) {
        this.path = path;
    }
}
