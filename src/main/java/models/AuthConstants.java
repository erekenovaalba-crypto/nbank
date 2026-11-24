package models;

public final class AuthConstants {
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    // Basic-авторизация админа собирается из логина и пароля, а не хранится готовой строкой
    public static final String ADMIN_AUTH_HEADER = "Basic " + java.util.Base64.getEncoder()
            .encodeToString((ADMIN_USERNAME + ":" + ADMIN_PASSWORD).getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private AuthConstants() {
    }
}