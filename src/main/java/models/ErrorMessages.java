package models;

public class ErrorMessages {
    private ErrorMessages() {}

    public static final String UNAUTHORIZED_ACCESS_TO_ACCOUNT = "Unauthorized access to account";
    public static final String NO_PERMISSION_TO_ACCOUNT = "You do not have permission to access this account";

    public static final String DEPOSIT_AMOUNT_EXCEEDS_LIMIT = "Deposit amount cannot exceed 5000";
    public static final String DEPOSIT_AMOUNT_TOO_SMALL = "Deposit amount must be at least 0.01";

    public static final String TRANSFER_AMOUNT_EXCEEDS_LIMIT = "Transfer amount cannot exceed 10000";
    public static final String TRANSFER_AMOUNT_TOO_SMALL = "Transfer amount must be at least 0.01";
    public static final String INVALID_TRANSFER = "Invalid transfer: insufficient funds or invalid accounts";

    public static final String INVALID_NAME = "Name must contain two words with letters only";
}
