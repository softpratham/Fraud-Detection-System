package com.fraud.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtil {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime parse(String s) {
        if (s == null) return null;
        try {
            return LocalDateTime.parse(s.trim(), FORMATTER);
        } catch (DateTimeParseException ex) {
            // try ISO fallback (if your CSV uses '2025-06-18T02:58')
            try {
                return LocalDateTime.parse(s.trim());
            } catch (Exception ex2) {
                throw new IllegalArgumentException("Unable to parse datetime: " + s, ex);
            }
        }
    }

    public static String format(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(FORMATTER);
    }
}