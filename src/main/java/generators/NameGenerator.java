package generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

// Генератор имён для обновления профиля. Допустимое имя: два слова, только буквы.
public final class NameGenerator {
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_WORD_LENGTH = 10;
    private static final String SYMBOLS = "!@#$%^&*()_+=<>?";

    private NameGenerator() {}

    // допустимое имя с заглавными буквами, например "Qwerty Asdfgh"
    public static String validName() {
        return capitalizedWord() + " " + capitalizedWord();
    }

    // допустимое имя, написанное строчными буквами
    public static String validNameLowerCase() {
        return validName().toLowerCase();
    }

    // недопустимое имя: одно слово
    public static String singleWordName() {
        return capitalizedWord();
    }

    // недопустимое имя: два слова, в конце второго спецсимвол
    public static String nameWithSymbol() {
        char symbol = SYMBOLS.charAt(ThreadLocalRandom.current().nextInt(SYMBOLS.length()));
        return validName() + symbol;
    }

    // недопустимое имя: два слова, в конце второго цифра
    public static String nameWithDigit() {
        return validName() + RandomStringUtils.randomNumeric(1);
    }

    // недопустимое имя: два слова, между ними несколько пробелов
    public static String nameWithExtraSpaces() {
        String spaces = " ".repeat(ThreadLocalRandom.current().nextInt(2, 7));
        return capitalizedWord() + spaces + capitalizedWord();
    }

    private static String capitalizedWord() {
        String word = RandomStringUtils.randomAlphabetic(MIN_WORD_LENGTH, MAX_WORD_LENGTH + 1).toLowerCase();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}