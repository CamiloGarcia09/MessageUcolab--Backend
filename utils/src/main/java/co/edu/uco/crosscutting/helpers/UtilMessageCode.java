package co.edu.uco.crosscutting.helpers;

import java.util.Locale;

public final class UtilMessageCode {

    public static final int MAX_SUFFIX_LENGTH = 10;
    public static final String SUFFIX_PATTERN = "^[A-Za-z0-9 ]+$";

    private static final String PREFIX = "MSG_";
    private static final String WHITESPACE_PATTERN = "\\s+";
    private static final String SEPARATOR = "_";

    private UtilMessageCode() {
    }

    public static String normalize(String suffix) {
        return PREFIX + UtilText.trim(suffix)
                .toUpperCase(Locale.ROOT)
                .replaceAll(WHITESPACE_PATTERN, SEPARATOR);
    }
}
