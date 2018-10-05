/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.util;


import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.TimeZone;

/**
 * An extension of <code>java.text.Format</code> that supports formatting objects of various types.
 *
 * @author Xavier Witdouck
 */
public class Formatter extends Format {

    private static final double ONE_DAY = 24d * 60d * 60d * 1000d;
    private static final double ONE_HOUR = 60d * 60d * 1000d;
    private static final double ONE_MINUTE = 60d * 1000d;
    private static final double ONE_SECOND = 1000d;

    private String nullText = "";
    private DateFormat dateFormat;
    private DecimalFormat decimalFormat;

    /**
     * Constructor
     */
    public Formatter() {
        this("###,##0.##;-###,##0.##", 1);
    }

    /**
     * Constructor
     * @param decimalPattern    the decimal pattern
     * @param multiplier        the decimal multiplier
     */
    public Formatter(String decimalPattern, int multiplier) {
        this(decimalPattern, multiplier, "");
    }

    /**
     * Constructor
     * @param decimalPattern    the decimal pattern
     * @param multiplier        the decimal multiplier
     * @param nullText          the text to return for null value
     */
    public Formatter(String decimalPattern, int multiplier, String nullText) {
        this(decimalPattern, multiplier, "dd-MMM-yyyy", TimeZone.getDefault(), nullText);
    }

    /**
     * Constructor
     * @param datePattern       the date pattern
     * @param timeZone          the time zone for dates
     */
    public Formatter(String datePattern, TimeZone timeZone) {
        this(datePattern, timeZone, "");
    }

    /**
     * Constructor
     * @param datePattern       the date pattern
     * @param timeZone          the time zone for dates
     * @param nullText          the text to return for null value
     */
    public Formatter(String datePattern, TimeZone timeZone, String nullText) {
        this("###,##0.##;-###,##0.##", 1, datePattern, timeZone, nullText);
    }

    /**
     * Constructor
     * @param decimalPattern    the decimal pattern
     * @param multiplier        the decimal multiplier
     * @param datePattern       the date pattern
     * @param timeZone          the time zone for dates
     * @param nullText          the text to return for null value
     */
    public Formatter(String decimalPattern, int multiplier, String datePattern, TimeZone timeZone, String nullText) {
        this.decimalFormat = new DecimalFormat(decimalPattern);
        this.decimalFormat.setMultiplier(multiplier);
        this.dateFormat = new SimpleDateFormat(datePattern);
        this.dateFormat.setTimeZone(timeZone);
        this.nullText = nullText;
    }

    /**
     * Sets the null text for this formatter
     * @param nullText  the null text
     */
    public void setNullText(String nullText) {
        this.nullText = nullText;
    }

    @Override()
    public StringBuffer format(Object object, StringBuffer buffer, FieldPosition position) {
        try {
            if (object == null) {
                buffer.append(nullText);
                position.setEndIndex(buffer.length());
                return buffer;
            } else if (object instanceof Double) {
                buffer.append(decimalFormat.format(object));
                position.setEndIndex(buffer.length());
                return buffer;
            } else if (object instanceof Float) {
                buffer.append(decimalFormat.format(object));
                position.setEndIndex(buffer.length());
                return buffer;
            } else if (object instanceof Date) {
                buffer.append(dateFormat.format(object));
                position.setEndIndex(buffer.length());
                return buffer;
            } else if (object instanceof Duration) {
                buffer.append(format((Duration)object));
                position.setEndIndex(buffer.length());
                return buffer;
            } else {
                buffer.append(object.toString());
                position.setEndIndex(buffer.length());
                return buffer;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to format value to string: " + object, t);
        }
    }


    /**
     * Returns a formatted string of the duration
     * @param duration  the duration value
     * @return          the formatted string
     */
    public static String format(Duration duration) {
        if (duration == null) {
            return "-";
        } else {
            final double millis = duration.toMillis();
            if (millis == 0d) {
                return "0";
            } else {
                final Formatter formatter = new Formatter("0.##;-0.##", 1);
                if (millis > ONE_DAY) {
                    return formatter.format(millis / ONE_DAY) + "d";
                } else if (millis > ONE_HOUR) {
                    return formatter.format(millis / ONE_HOUR) + "h";
                } else if (millis > ONE_MINUTE) {
                    return formatter.format(millis / ONE_MINUTE) + "m";
                } else if (millis > ONE_SECOND) {
                    return formatter.format(millis / ONE_SECOND) + "s";
                } else {
                    return formatter.format(millis) + "ms";
                }
            }
        }

    }

    @Override()
    public Object parseObject(String source, ParsePosition position) {
        throw new UnsupportedOperationException("Formatter.parseObject() is not currently supported");
    }
}
