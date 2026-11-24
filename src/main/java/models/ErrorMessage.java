package models;

import lombok.Getter;

// тексты ошибок, которые возвращает API
@Getter
public enum ErrorMessage {
    UNAUTHORIZED_ACCESS_TO_ACCOUNT("Unauthorized access to account"),
    NO_PERMISSION_TO_ACCOUNT("You do not have permission to access this account"),

    DEPOSIT_AMOUNT_EXCEEDS_LIMIT("Deposit amount cannot exceed 5000"),
    DEPOSIT_AMOUNT_TOO_SMALL("Deposit amount must be at least 0.01"),

    TRANSFER_AMOUNT_EXCEEDS_LIMIT("Transfer amount cannot exceed 10000"),
    TRANSFER_AMOUNT_TOO_SMALL("Transfer amount must be at least 0.01"),
    INVALID_TRANSFER("Invalid transfer: insufficient funds or invalid accounts"),

    INVALID_NAME("Name must contain two words with letters only"),

    USERNAME_BLANK("Username cannot be blank"),
    USERNAME_INVALID_LENGTH("Username must be between 3 and 15 characters");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

}
