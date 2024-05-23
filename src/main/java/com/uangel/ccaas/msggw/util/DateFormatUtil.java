package com.uangel.ccaas.msggw.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class DateFormatUtil {
    private static final String YYYY_MM_DD_HH_MM_SS = "yyyyMMddHHmmss";
    private static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String YYYY_MM_DD = "yyyyMMdd";
    private static final String HH_MM_SS = "HHmmss";
    private static final String HH_MM = "HHmm";
    private static final String HH = "HH";
    private static final String MM = "mm";

    private DateFormatUtil() {
        // Do Nothing
    }

    public static String formatYmdHms(long longDate) {
        return format(YYYY_MM_DD_HH_MM_SS, longDate);
    }

    public static String formatYmdHmsS(long longDate) {
        return format(YYYY_MM_DD_HH_MM_SS_SSS, longDate);
    }

    public static String formatYmd(Date date) {
        return format(YYYY_MM_DD, date);
    }

    public static String formatHH(Date date) {
        return format(HH, date);
    }

    public static String formatMM(Date date) {
        return format(MM, date);
    }

    public static String formatYmd(long longDate) {
        return format(YYYY_MM_DD, longDate);
    }

    public static String formatHms(long longDate) {
        return format(HH_MM_SS, longDate);
    }

    public static String formatHm(long longDate) {
        return format(HH_MM, longDate);
    }

    private static String format(String dateFormat, Date date) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        return format.format(date);
    }

    private static String format(String dateFormat, long longDate) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        return format.format(longDate);
    }

    public static String currentTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS_SSS));
    }

    public static String fastFormatYmdHmsS(Date date) {
        FastDateFormat format = FastDateFormat.getInstance(YYYY_MM_DD_HH_MM_SS_SSS);
        return format.format(date);
    }

    public static Date formatYmdHmsS(String time) {
        Date date = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_SSS);
            date = format.parse(time);
        } catch (ParseException e) {
            log.warn("Failed to formatYmdHmsS", e);
        }

        return date;
    }

}
