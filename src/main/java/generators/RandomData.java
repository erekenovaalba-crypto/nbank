package generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomData {
    private RandomData(){}
    public static String getUsername(){
        return RandomStringUtils.randomAlphabetic(10);
    }
    public static String getPassword(){
        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
                RandomStringUtils.randomNumeric(3) + "$";
    }
    public static String getName(){

        return RandomStringUtils.randomAlphabetic(10) + " " + RandomStringUtils.randomAlphabetic(10) ;
    }
    // id счёта, которого заведомо нет: большое число, до которого счета не дорастут
    public static long getNonExistentAccountId(){
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, Integer.MAX_VALUE);
    }
}
