package generators;


public final class GeneratingRules {
    private GeneratingRules() {}

    // логин: от 3 до 15 символов
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 15;
    public static final String USERNAME = "[A-Za-z0-9]{" + USERNAME_MIN_LENGTH + "," + USERNAME_MAX_LENGTH + "}";

    // пароль: заглавные, строчные, цифры и спецсимвол $ (его сервер принимает точно)
    public static final int PASSWORD_UPPER_COUNT = 3;
    public static final int PASSWORD_LOWER_COUNT = 5;
    public static final int PASSWORD_DIGIT_COUNT = 3;
    public static final String PASSWORD_SPECIAL_CHAR = "\\$";
    public static final String PASSWORD = "[A-Z]{" + PASSWORD_UPPER_COUNT + "}"
            + "[a-z]{" + PASSWORD_LOWER_COUNT + "}"
            + "[0-9]{" + PASSWORD_DIGIT_COUNT + "}"
            + PASSWORD_SPECIAL_CHAR;

    // роль обычного пользователя (то же значение, что UserRole.USER.name())
    public static final String USER_ROLE = "USER";

    // имя в профиле: два слова с заглавной буквы, только буквы
    private static final String NAME_WORD = "[A-Z][a-z]{2,9}";
    public static final String PROFILE_NAME = NAME_WORD + " " + NAME_WORD;

    // id счёта: от 6 до 9 цифр, без ведущего нуля
    public static final String ACCOUNT_ID = "[1-9][0-9]{5,8}";

    // сумма с двумя знаками после запятой: от 1.00 до 4999.99 (лимит депозита 5000)
    public static final String DEPOSIT_AMOUNT = "([1-9][0-9]{0,2}|[1-4][0-9]{3})\\.[0-9]{2}";
    // от 1.00 до 9999.99 (лимит перевода 10000)
    public static final String TRANSFER_AMOUNT = "[1-9][0-9]{0,3}\\.[0-9]{2}";
}
