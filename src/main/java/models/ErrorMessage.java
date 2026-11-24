package models;

import generators.AmountGenerator;
import generators.GeneratingRules;
import lombok.Getter;

// тексты ошибок, которые возвращает API
@Getter
public enum ErrorMessage {
    UNAUTHORIZED_ACCESS_TO_ACCOUNT("Unauthorized access to account"),
    NO_PERMISSION_TO_ACCOUNT("You do not have permission to access this account"),

    DEPOSIT_AMOUNT_EXCEEDS_LIMIT("Deposit amount cannot exceed " + AmountGenerator.MAX_DEPOSIT.toPlainString()),
    DEPOSIT_AMOUNT_TOO_SMALL("Deposit amount must be at least " + AmountGenerator.MIN_AMOUNT.toPlainString()),

    TRANSFER_AMOUNT_EXCEEDS_LIMIT("Transfer amount cannot exceed " + AmountGenerator.MAX_TRANSFER.toPlainString()),
    TRANSFER_AMOUNT_TOO_SMALL("Transfer amount must be at least " + AmountGenerator.MIN_AMOUNT.toPlainString()),
    INVALID_TRANSFER("Invalid transfer: insufficient funds or invalid accounts"),

    INVALID_NAME("Name must contain two words with letters only"),

    USERNAME_BLANK("Username cannot be blank"),
    USERNAME_INVALID_LENGTH("Username must be between " + GeneratingRules.USERNAME_MIN_LENGTH
            + " and " + GeneratingRules.USERNAME_MAX_LENGTH + " characters");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

}
