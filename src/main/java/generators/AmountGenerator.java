package generators;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

public final class AmountGenerator {
    // ограничения на суммы операций
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("5000");
    public static final BigDecimal MAX_TRANSFER = new BigDecimal("10000");
    // сервер хранит баланс как double (например 24349.699999999983), поэтому балансы сравниваем с допуском
    public static final BigDecimal BALANCE_DELTA = new BigDecimal("0.005");

    // два знака после запятой: центы
    public static final int AMOUNT_SCALE = 2;
    private static final BigDecimal ABOVE_LIMIT_FACTOR = BigDecimal.TWO;

    private AmountGenerator() {}

    // случайная сумма от min до max включительно, с двумя знаками после запятой
    public static BigDecimal randomAmount(BigDecimal min, BigDecimal max) {
        long minCents = min.movePointRight(AMOUNT_SCALE).longValueExact();
        long maxCents = max.movePointRight(AMOUNT_SCALE).longValueExact();
        long cents = ThreadLocalRandom.current().nextLong(minCents, maxCents + 1);
        return BigDecimal.valueOf(cents, AMOUNT_SCALE);
    }

    public static BigDecimal validDepositAmount() {
        return randomAmount(MIN_AMOUNT, MAX_DEPOSIT);
    }

    public static BigDecimal validTransferAmount() {
        return randomAmount(MIN_AMOUNT, MAX_TRANSFER);
    }

    // сумма больше limit, но не больше limit * 2
    public static BigDecimal amountAbove(BigDecimal limit) {
        return randomAmount(limit.add(MIN_AMOUNT), limit.multiply(ABOVE_LIMIT_FACTOR));
    }

    // отрицательная сумма от -limit до -0.01
    public static BigDecimal negativeAmount(BigDecimal limit) {
        return randomAmount(limit.negate(), MIN_AMOUNT.negate());
    }
}