/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on Nov, 2020 by @author bobo
 *
 * Util to easily convert various time formats to milliseconds
 *
 * 参考：
 * https://github.com/vercel/ms
 */
public class TimeSpanUtil {
    private static String patternString = "^(-?(?:\\d+)?\\.?\\d+) *(milliseconds?|msecs?|ms|seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h|days?|d|weeks?|w|years?|yrs?|y)?$";
    private static Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

    private static long s = 1000;
    private static long m = s * 60;
    private static long h = m * 60;
    private static long d = h * 24;
    private static long w = d * 7;
    private static long y = d * 365;

    public static long toMs(String input) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(input);
        }
        float n = Float.parseFloat(matcher.group(1));
        String type = matcher.group(2);
        if (StringUtils.isEmpty(type)) {
            type = "ms";
        }
        type = type.toLowerCase();

        switch (type) {
            case "years":
            case "year":
            case "yrs":
            case "yr":
            case "y":
                return (long) (n * y);
            case "weeks":
            case "week":
            case "w":
                return (long) (n * w);
            case "days":
            case "day":
            case "d":
                return (long) (n * d);
            case "hours":
            case "hour":
            case "hrs":
            case "hr":
            case "h":
                return (long) (n * h);
            case "minutes":
            case "minute":
            case "mins":
            case "min":
            case "m":
                return (long) (n * m);
            case "seconds":
            case "second":
            case "secs":
            case "sec":
            case "s":
                return (long) (n * s);
            case "milliseconds":
            case "millisecond":
            case "msecs":
            case "msec":
            case "ms":
                return (long) n;
            default:
                throw new IllegalArgumentException(input);
        }
    }
}
