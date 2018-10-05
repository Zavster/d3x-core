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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that captures an amount of memory measured in bytes
 *
 * @author Xavier Witdouck
 */
@lombok.EqualsAndHashCode(of={"bytes"}, callSuper = false)
public class Memory extends java.lang.Number implements Comparable<Memory>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final Memory ZERO = new Memory(0d);

    private static final Pattern PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?(?:E-?\\d+)?)\\s*(B|K|M|G|T|KB|MB|GB|TB|KiB|MiB|GiB|TiB)?", Pattern.CASE_INSENSITIVE);

    public static final double ONE_KB = 1024;
    public static final double ONE_MB = Math.pow(1024, 2);
    public static final double ONE_GB = Math.pow(1024, 3);
    public static final double ONE_TB = Math.pow(1024, 4);

    private double bytes;

    /**
     * Constructor
     * @param bytes     the value in bytes
     */
    public Memory(double bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns a new memory object for the value specified
     * @param value     the memory in bytes
     * @return          the newly created value
     */
    public static Memory of(double value) {
        return new Memory(value);
    }

    /**
     * Returns the value in bytes
     * @return  the value in bytes
     */
    public double getValue() {
        return bytes;
    }

    /**
     * Returns the memory measured in kilobytes
     * @return      the value in kilobytes
     */
    public double getKiloBytes() {
        return bytes / ONE_KB;
    }

    /**
     * Returns the memory measured in megabytes
     * @return      the value in megabytes
     */
    public double getMegaBytes() {
        return bytes / ONE_MB;
    }

    /**
     * Returns the memory measured in gigabytes
     * @return  the value in gigabytes
     */
    public double getGigaBytes() {
        return bytes / ONE_GB;
    }

    /**
     * Returns the memory measured in terabytes
     * @return  the value in terabytes
     */
    public double getTeraBytes() {
        return bytes / ONE_TB;
    }

    @Override()
    public int intValue() {
        return (int) bytes;
    }

    @Override()
    public long longValue() {
        return (long) bytes;
    }

    @Override()
    public float floatValue() {
        return (float) bytes;
    }

    @Override()
    public double doubleValue() {
        return bytes;
    }

    /**
     * Returns a memory definition parsed from the specified text
     * @param text  the text value such as 34, 34B, 34KB, 34MB, 34GB, 34TB
     * @return      the memory definition
     */
    public static Memory parse(String text) {
        if (text == null || text.length() == 0) {
            return new Memory(0);
        } else {
            final Matcher matcher = PATTERN.matcher("");
            if (!matcher.reset(text).matches()) {
                throw new IllegalArgumentException("Malformed memory value: " + text);
            } else {
                final double value = Double.parseDouble(matcher.group(1));
                final String units = matcher.group(2) != null ? matcher.group(2).trim().toUpperCase() : "B";
                if      (units.equals("B")) return new Memory(value);
                else if (units.equals("K")) return new Memory(value * ONE_KB);
                else if (units.equals("M")) return new Memory(value * ONE_MB);
                else if (units.equals("G")) return new Memory(value * ONE_GB);
                else if (units.equals("T")) return new Memory(value * ONE_TB);
                else if (units.equals("KB")) return new Memory(value * ONE_KB);
                else if (units.equals("MB")) return new Memory(value * ONE_MB);
                else if (units.equals("GB")) return new Memory(value * ONE_GB);
                else if (units.equals("TB")) return new Memory(value * ONE_TB);
                else if (units.equals("KIB")) return new Memory(value * ONE_KB);
                else if (units.equals("MIB")) return new Memory(value * ONE_MB);
                else if (units.equals("GIB")) return new Memory(value * ONE_GB);
                else if (units.equals("TIB")) return new Memory(value * ONE_GB);
                else throw new IllegalArgumentException("Unsupported memory specification: " + text);
            }
        }
    }

    @Override()
    public int compareTo(Memory other) {
        return other == null ? 1 : Double.compare(this.getValue(), other.getValue());
    }

    @Override()
    public String toString() {
        final Formatter formatter = new Formatter("0.##;-0.##", 1);
        if      (bytes == 0d) return "0MB";
        else if (bytes > ONE_TB) return formatter.format(bytes / ONE_TB) + "TB";
        else if (bytes > ONE_GB) return formatter.format(bytes / ONE_GB) + "GB";
        else if (bytes > ONE_MB) return formatter.format(bytes / ONE_MB) + "MB";
        else if (bytes > ONE_KB) return formatter.format(bytes / ONE_KB) + "KB";
        else return formatter.format(bytes) + "B";
    }


    public static void main(String[] args) {
        System.out.println(Memory.parse("3.23E9"));
        System.out.println(Memory.parse("345"));
        System.out.println(Memory.parse("34B"));
        System.out.println(Memory.parse("34KB"));
        System.out.println(Memory.parse("34kb"));
        System.out.println(Memory.parse("34MB"));
        System.out.println(Memory.parse("34mb"));
        System.out.println(Memory.parse("34GB"));
        System.out.println(Memory.parse("34TB"));
    }

}
