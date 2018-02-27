package com.example.xyzreader.data;

/**
 * A helper class that provides methods applicable to the entire code base. As of now, it
 * only includes a single method.
 */

public class Utilities {

    public static final String ERRONEOUS_CHARACTER_ENCODING_NEW_LINE = "\r\n\r\n";
    public static final String ERRONEOUS_CHARACTER_ENCODING_SPACE = "\r\n";
    public static final String ERRONEOUS_CHARACTER_EXTRA_SPACE = "    ";

    /*
    A helper method which aids in formatting the text within the body of the RSS feed so that it
    is displayed in a more legible format.
     */
    public static String sanitizeBodyText(String bodyText) {
        return bodyText
                .replaceAll(ERRONEOUS_CHARACTER_ENCODING_NEW_LINE, "\n\n")
                .replaceAll(ERRONEOUS_CHARACTER_ENCODING_SPACE, " ")
                .replaceAll(ERRONEOUS_CHARACTER_EXTRA_SPACE, "");
    }
}
