package models;

import lombok.Getter;

// тексты успешных ответов API
@Getter
public enum SuccessMessage {
    TRANSFER_SUCCESSFUL("Transfer successful"),
    PROFILE_UPDATED("Profile updated successfully");

    private final String message;

    SuccessMessage(String message) {
        this.message = message;
    }

}
