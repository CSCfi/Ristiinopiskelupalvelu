package fi.uta.ristiinopiskelu.persistence.utils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class DateUtils {

    private static final String PATTERN = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    /**
     * Compares given local dates.
     * Does not do null checks so will throw nullptr exception if
     * either of methods is null.
     * @return d1 is before cmpTo || d1 equals cmpTo
     */
    public static boolean isBeforeOrEqual(LocalDate d1, LocalDate cmpTo) {
        return d1.isBefore(cmpTo) || d1.isEqual(cmpTo);
    }

    /**
     * Compares given local dates.
     * Does not do null checks so will throw nullptr exception if
     * either of methods is null.
     * @return d1 is after cmpTo || d1 equals cmpTo
     */
    public static boolean isAfterOrEqual(LocalDate d1, LocalDate cmpTo) {
        return d1.isAfter(cmpTo) || d1.isEqual(cmpTo);
    }

    /**
     * Compares given offset datetimes.
     * Does not do null checks so will throw nullptr exception if
     * either of methods is null.
     * @return o1 is before cmpTo || o1 equals cmpTo
     */
    public static boolean isBeforeOrEqual(OffsetDateTime o1, OffsetDateTime cmpTo) {
        return o1.isBefore(cmpTo) || o1.isEqual(cmpTo);
    }

    /**
     * Compares given given offset datetimes.
     * Does not do null checks so will throw nullptr exception if
     * either of methods is null.
     * @return o1 is after cmpTo || o1 equals cmpTo
     */
    public static boolean isAfterOrEqual(OffsetDateTime o1, OffsetDateTime cmpTo) {
        return o1.isAfter(cmpTo) || o1.isEqual(cmpTo);
    }

    public static String getFormattedNow() {
        return getFormatted(OffsetDateTime.now());
    }

    public static String getFormatted(OffsetDateTime date) {
        return FORMATTER.format(date);
    }

    public static String getFormatted(String pattern, TemporalAccessor date) {
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }

    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }

    public static String getPattern() {
        return PATTERN;
    }
}
