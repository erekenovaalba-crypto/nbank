package generators;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

public final class AmountGenerator {
    // ограничения на суммы операций
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("5000");
    public static final BigDecimal MAX_TRANSFER = new BigDecimal("10000");

    private AmountGenerator() {}

    // случайная сумма от min до max включительно, с двумя знаками после запятой
    public static BigDecimal randomAmount(BigDecimal min, BigDecimal max) {
        long minCents = min.movePointRight(2).longValueExact();
        long maxCents = max.movePointRight(2).longValueExact();
        long cents = ThreadLocalRandom.current().nextLong(minCents, maxCents + 1);
        return BigDecimal.valueOf(cents, 2);
    }

    public static BigDecimal validDepositAmount() {
        return randomAmount(MIN_AMOUNT, MAX_DEPOSIT);
    }

    public static BigDecimal validTransferAmount() {
        return randomAmount(MIN_AMOUNT, MAX_TRANSFER);
    }

    // сумма больше limit, но не больше limit * 2
    public static BigDecimal amountAbove(BigDecimal limit) {
        return randomAmount(limit.add(MIN_AMOUNT), limit.multiply(BigDecimal.valueOf(2)));
    }

    // отрицательная сумма от -limit до -0.01
    public static BigDecimal negativeAmount(BigDecimal limit) {
        return randomAmount(limit.negate(), MIN_AMOUNT.negate());
    }
}