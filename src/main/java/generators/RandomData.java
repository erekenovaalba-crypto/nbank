package generators;

import com.github.curiousoddman.rgxgen.RgxGen;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomData {
    // id несуществующего счёта: большое число, до которого счета не дорастут
    private static final long NON_EXISTENT_ACCOUNT_ID_MIN = 1_000_000_000L;
    private static final long NON_EXISTENT_ACCOUNT_ID_MAX = Integer.MAX_VALUE;
    private static final String SPACE = " ";
    private static final int SHORTEST_USERNAME_LENGTH = 1;

    private RandomData() {}

    // корректный пароль по тому же правилу, что и в моделях
    public static String getPassword() {
        return RgxGen.parse(GeneratingRules.PASSWORD).generate();
    }

    // логин из одних пробелов допустимой длины
    public static String getBlankUsername() {
        int length = ThreadLocalRandom.current()
                .nextInt(GeneratingRules.USERNAME_MIN_LENGTH, GeneratingRules.USERNAME_MAX_LENGTH + 1);
        return SPACE.repeat(length);
    }

    // логин короче минимальной длины
    public static String getTooShortUsername() {
        return RandomStringUtils.randomAlphabetic(SHORTEST_USERNAME_LENGTH, GeneratingRules.USERNAME_MIN_LENGTH);
    }

    public static long getNonExistentAccountId() {
        return ThreadLocalRandom.current().nextLong(NON_EXISTENT_ACCOUNT_ID_MIN, NON_EXISTENT_ACCOUNT_ID_MAX);
    }
}
