package co.edu.uco.crosscutting.helpers;

import static co.edu.uco.crosscutting.helpers.UtilObject.*;

public final class UtilText {
    public static final String EMPTY = "";
    public static final String LETTERS_AND_SPACES_ONLY = "^[a-zA-ZñÑ\\s]+$";
    public static final String ANY_CHARACTER_EXCEPT_NUMBER = "[^\\d]";
    public static final String ONLY_NUMBERS = "\\d+";
    public static final String ONLY_LETTERS = "^[a-zA-Z]+$";
    public static final String ONLY_ANY_NUMBER = "[-+]?\\d*\\.?\\d+";
    public static final String ONLY_CHARACTERS_AND_NUMBERS = "^[a-zA-Z0-9]*$";
    public static final String SPACE = " ";
    private UtilText() {}
    public static boolean isNull(String value) {
        return isNullObject(value);
    }
    public static String getDefault(String value, String defaultValue) {
        return getDefaultIsNullObject(value, defaultValue);
    }
    public static String getDefault(String value) {
        return getDefaultIsNullObject(value, EMPTY);
    }

    public static String trim(String value) {return getDefault(value).trim();}

    public static boolean isEmpty(String value) {
        return EMPTY.equals(trim(value));
    }

    public static boolean isContainsOnlyLetterAndSpace(String value) {
        return validMatch(value, LETTERS_AND_SPACES_ONLY);
    }
    public static boolean validMatch(String value, String expression) {
        return value.matches(expression);
    }

    public static boolean isEmptyOrNull(String value) {
        return isNull(value) || isEmpty(value);
    }

    public static String concatenateWithoutSeparator(String... values) {
        return String.join(EMPTY, values);
    }

    public static String concatenateWithSeparator(String separator, String... values) {
        return String.join(separator, values);
    }

    public static String stringToUpperCase(String value) {
        return getDefault(value).toUpperCase();
    }
}