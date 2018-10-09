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

import java.io.*;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An IO utility class
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class IO {


    /**
     * More concise way for write to standard out
     * @param format    the format string
     * @param args      the format args
     */
    public static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }


    /**
     * More concise way for write to standard out
     * @param value     the value to print
     */
    public static void println(Object value) {
        System.out.println(value);
    }


    /**
     * Returns a free port that can be used to listen on
     * @return  a randomly assigned free port to listen
     * @throws IOException  if fails to initialize server socket
     */
    public static int freePort() throws IOException {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } finally {
            IO.close(socket);
        }
    }


    /**
     * Closes a closable while swallowing any potential exceptions
     * @param closeable     the closeable, can be null
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * Closes the array of resources and suppresses exceptions
     * @param resources     the resources to close
     */
    public static void close(AutoCloseable... resources) {
        if (resources != null) {
            for (AutoCloseable resource : resources) {
                try {
                    if (resource != null) {
                        resource.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    /**
     * Creates directories defined by the path specified
     * @param dir   the directory path to create
     * @return      the same as arg
     * @throws RuntimeException if directory creation fails
     */
    public static File mkdirs(File dir) {
        if (dir != null) {
            if (!dir.mkdirs()) {
                if (!dir.exists()) {
                    throw new RuntimeException("Failed to create directory: " + dir.getAbsolutePath());
                }
            }
        }
        return dir;
    }


    /**
     * Reads all the bytes from the file specified
     * @param is        the input stream to read from
     * @return          the bytes read
     * @throws IOException  if there is an I/O exception
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        BufferedInputStream bis = null;
        try {
            byte[] buffer = new byte[1024 * 100];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024 * 100);
            bis = new BufferedInputStream(is);
            while (true) {
                final int read = bis.read(buffer);
                if (read < 0) break;
                else bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        } finally {
            IO.close(bis);
        }
    }


    /**
     * Writes the array of bytes to the file
     * @param bytes     the bytes to write
     * @param os        the output stream to write to
     * @throws IOException  if there is an I/O exception
     */
    public static void writeBytes(byte[] bytes, OutputStream os) throws IOException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(os);
            bos.write(bytes);
            bos.flush();
        } finally {
            IO.close(bos);
        }
    }


    /**
     * Returns the text loaded from the file specified
     * @param file      the file to read from
     * @return          the text from file
     * @throws IOException  if there is an I/O exception
     */
    public static String readText(File file) throws IOException {
        return readText(new BufferedInputStream(new FileInputStream(file)));
    }


    /**
     * Reads bytes from the input stream as text
     * @param is        the input stream to read from
     * @return              the resulting text
     * @throws IOException  if there is an I/O exception
     */
    public static String readText(InputStream is) throws IOException {
        return readText(is, 1024 * 100);
    }


    /**
     * Reads bytes from the reader as text
     * @param reader        the reader
     * @return              the resulting text
     * @throws IOException  if there is an I/O exception
     */
    public static String readText(Reader reader) throws IOException {
        return readText(reader, 1024 * 100);
    }

    /**
     * Reads bytes from the reader as text
     * @param reader        the reader
     * @param bufferSize    the size of the byte buffer
     * @return              the resulting text
     * @throws IOException  if there is an I/O exception
     */
    public static String readText(Reader reader, int bufferSize) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(reader);
            final char[] buffer = new char[bufferSize];
            final StringBuilder result = new StringBuilder();
            while (true) {
                final int read = br.read(buffer);
                if (read < 0) break;
                else {
                    result.append(new String(buffer, 0, read));
                }
            }
            return result.toString();
        } finally {
            IO.close(br);
        }
    }


    /**
     * Reads bytes from the input stream as text
     * @param is        the input stream to read from
     * @param bufferSize    the size of the byte buffer
     * @return              the resulting text
     * @throws IOException  if there is an I/O exception
     */
    public static String readText(InputStream is, int bufferSize) throws IOException {
        if (is instanceof BufferedInputStream) {
            try {
                final byte[] buffer = new byte[bufferSize];
                final StringBuilder result = new StringBuilder();
                while (true) {
                    final int read = is.read(buffer);
                    if (read < 0) break;
                    else {
                        result.append(new String(buffer, 0, read));
                    }
                }
                return result.toString();
            } finally {
                close(is);
            }
        } else {
            return readText(new BufferedInputStream(is), bufferSize);
        }
    }


    /**
     * Reads lines from the input stream and echos them to the consumer
     * @param is            the input stream to read from
     * @param consumer      the consumer to receive lines
     * @throws IOException  if there is an I/O exception
     */
    public static void readLines(InputStream is, Consumer<String> consumer) throws IOException {
        BufferedReader reader = null;
        try {
            final InputStream stream = is instanceof BufferedInputStream ? is : new BufferedInputStream(is);
            reader = new BufferedReader(new InputStreamReader(stream));
            while (true) {
                final String line = reader.readLine();
                if (line == null) break;
                else {
                    consumer.accept(line);
                }
            }
        } finally {
            IO.close(reader);
        }
    }


    /**
     * Copies bytes from the input stream to the output stream
     * @param is        the input stream to read from
     * @param os        the output stream to write to
     * @throws IOException  if there is an I/O exception
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        copy(is, os, 1024 * 100);
    }


    /**
     * Copies bytes from the input stream to the output stream
     * @param is        the input stream to read from
     * @param os        the output stream to write to
     * @param bufferSize    the buffer size for chunks
     * @throws IOException  if there is an I/O exception
     */
    public static void copy(InputStream is, OutputStream os, int bufferSize) throws IOException {
        final byte[] bytes = new byte[bufferSize];
        BufferedInputStream bis = buffer(is);
        BufferedOutputStream bos = buffer(os);
        try {
            while (true) {
                final int read = bis.read(bytes);
                if (read < 0) break;
                else {
                    bos.write(bytes, 0, read);
                }
            }
        } finally {
            IO.close(bis, bos);
        }
    }


    /**
     * Returns a buffered input stream wrapper or the stream itself if is already a buffered stream
     * @param is    the stream to potentially wrap
     * @return      the buffered reference
     */
    public static BufferedInputStream buffer(InputStream is) {
        if (is instanceof BufferedInputStream) {
            return (BufferedInputStream)is;
        } else {
            return new BufferedInputStream(is);
        }
    }


    /**
     * Returns a buffered output stream wrapper or the stream itself if is already a buffered stream
     * @param os    the stream to potentially wrap
     * @return      the buffered reference
     */
    public static BufferedOutputStream buffer(OutputStream os) {
        if (os instanceof BufferedOutputStream) {
            return (BufferedOutputStream)os;
        } else {
            return new BufferedOutputStream(os);
        }
    }


    /**
     * Returns a stringified stack trace from the exception
     * @param throwable the throwable
     * @return          the stack trace
     */
    public static String getStackTrace(Throwable throwable) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        throwable.printStackTrace(new PrintStream(baos));
        return new String(baos.toByteArray());
    }



    /**
     * Returns the version of the jar file from which the class comes from
     * @param clazz     the clazz to infer version of jar file
     * @return          the version string if matched
     */
    public static Option<String> getVersion(Class<?> clazz) {
        try {
            final File file = getJarFile(clazz).orNull();
            if (file != null && file.isFile()) {
                final String name = file.getName();
                final Matcher matcher = Pattern.compile("(.+)-(.+).jar").matcher(name);
                if (matcher.matches()) {
                    final String version = matcher.group(2);
                    return Option.of(version);
                }
            }
            return Option.empty();
        } catch (Throwable t) {
            log.error("Failed to resolve jar file version for class: " + clazz, t);
            return Option.empty();
        }
    }


    /**
     * Returns a reference to the JAR file for the class if it is in a JAR
     * @param clazz     the class reference
     * @return          the JAR file handle
     */
    public static Option<File> getJarFile(Class<?> clazz) {
        try {
            final CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            final URL url = codeSource != null ? codeSource.getLocation() : null;
            return url != null ? Option.of(new File(url.toURI())) : Option.empty();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to resolve JAR file for class: " + clazz, ex);
        }
    }


    /**
     * Returns the set of MAC addresses resolved from this machine
     * @return  the set of MAC addresses for machine
     */
    public static Set<String> getMacAddresses() {
        try {
            final Set<String> macSet = new TreeSet<>();
            final Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                final NetworkInterface network = networks.nextElement();
                if (!network.isLoopback()) {
                    final byte[] bytes = network.getHardwareAddress();
                    if (bytes != null) {
                        final StringBuilder mac = new StringBuilder();
                        for (int i = 0; i < bytes.length; i++) {
                            mac.append(String.format("%02X%s", bytes[i], (i < bytes.length - 1) ? ":" : ""));
                        }
                        macSet.add(mac.toString().toUpperCase());
                    }
                }
            }
            return macSet;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to resolve MAC address from Network interfaces", ex);
        }
    }


    /**
     * Returns the first N lines from the text file specified
     * @param file      the file to read from
     * @param count     the max number of lines to include
     * @param consumer  the consumer to receive first N lines
     * @throws IOException  if there is an I/O exception
     */
    public static void head(File file, int count, Consumer<String> consumer) throws IOException {
        if (file.length() > 0) {
            Files.lines(file.toPath()).limit(count).forEach(consumer);
        }
    }


    /**
     * Echoes the last N lines of a file out to the consumer
     * @param file      the file to read from
     * @param count     the max number of lines to include
     * @param consumer  the consumer to receive last N lines
     * @throws IOException  if there is an I/O exception
     */
    public static void tail(File file, int count, Consumer<String> consumer) throws IOException {
        tail(file, 1024 * 100, count, consumer);
    }

    /**
     * Echoes the last N lines of a file out to the consumer
     * @param file          the file to read from
     * @param bufferSize    the read buffer size
     * @param count         the max number of lines to include
     * @param consumer      the consumer to receive last N lines
     * @throws IOException  if there is an I/O exception
     */
    public static void tail(File file, int bufferSize, int count, Consumer<String> consumer) throws IOException {
        if (file.length() > 0) {
            final TextBlock block = new TextBlock(bufferSize, file).end();
            int lineCount = block.lineCount();
            if (lineCount > count) {
                final int fromLine = lineCount - count;
                block.lines(fromLine, consumer);
            } else {
                while (lineCount <= count && !block.isStart()) {
                    block.moveUp();
                    lineCount += block.lineCount();
                }
                final int from = Math.max(lineCount - count, 0);
                block.lines(from, consumer);
                while (!block.isEnd()) {
                    block.moveDown();
                    block.lines(consumer);
                }
            }
        }
    }



    public static void main(String[] args) throws IOException {
        IO.tail(new File(args[0]), 100, System.out::println);
    }





    /**
     * A class that represents a contiguous block of text in a RandomAccessFile which can be moved around
     *
     * @author Xavier Witdouck
     */
    static class TextBlock {

        private int size;
        private byte[] bytes;
        private int capacity;
        private int lineCount = -1;
        private RandomAccessFile raf;
        private boolean truncatedStart;


        /**
         * Constructor
         * @param capacity  the capacity for text block
         * @param file      the file reference
         */
        TextBlock(int capacity, File file) throws IOException {
            this.capacity = capacity;
            this.raf = new RandomAccessFile(file, "r");
            this.bytes = new byte[capacity];
        }


        /**
         * Moves this block to the start of the file
         * @return  this text block
         * @throws IOException  if I/O error from random access file
         */
        TextBlock begin() throws IOException {
            this.raf.seek(0);
            this.size = raf.read(bytes);
            this.truncatedStart = false;
            return this;
        }


        /**
         * Moves this block to the end of the file
         * @return  this text block
         * @throws IOException  if I/O error from random access file
         */
        TextBlock end() throws IOException {
            final long length = raf.length();
            final long start = Math.max(0, length - capacity);
            this.raf.seek(start);
            this.size = raf.read(bytes);
            this.truncatedStart = true;
            return this;
        }

        /**
         * Returns true if this text block is at the start of file
         * @return  true if at start of file
         */
        boolean isStart() throws IOException {
            return toFilePosition(0) == 0;
        }

        /**
         * Returns true if this text block is at the end of file
         * @return  true if at end of file
         */
        boolean isEnd() throws IOException {
            return toFilePosition(size -1) == raf.length()-1;
        }


        /**
         * Moves this block of text up one increment
         * @throws IOException  if I/O error from random access file
         */
        void moveUp() throws IOException {
            try {
                final int index = truncatedStart ? nextEndOfLine(0) : -1;
                final long position = toFilePosition(index);
                final long start = Math.max(0, position - (capacity - 1));
                this.raf.seek(start);
                this.size = raf.read(bytes);
                this.truncatedStart = start > 0;
            } finally {
                this.lineCount = -1;
            }
        }


        /**
         * Moves this block of text down one increment
         * @throws IOException  if I/O error from random access file
         * @return  this text block
         */
        void moveDown() throws IOException {
            try {
                final int end = truncatedStart ? nextEndOfLine(size -1) : priorEndOfLine(size -1);
                final long position = toFilePosition(end);
                final long start = position + 1;
                this.raf.seek(start);
                this.size = raf.read(bytes);
                this.truncatedStart = false;
            } finally {
                this.lineCount = -1;
            }
        }


        /**
         * Returns the file position given the index in this block
         * @param index     the index in this block
         * @return          the corresponding file position
         */
        long toFilePosition(int index) throws IOException {
            final long position = raf.getFilePointer();
            return position - (size - index);
        }


        /**
         * Returns the number of complete lines in this block
         * @return      the number of complete lines in block
         */
        int lineCount() {
            if (lineCount < 0) {
                lineCount = 0;
                int nextEol = nextEndOfLine(0);
                while (nextEol < size - 1) {
                    final int start = adjustForward(nextEol);
                    nextEol = nextEndOfLine(start);
                    lineCount++;
                }
            }
            return lineCount;
        }


        /**
         * Echoes lines in this text block to the consumer specified
         * @param consumer  the consumer to receive lines
         */
        void lines(Consumer<String> consumer) throws IOException {
            this.lines(0, consumer);
        }


        /**
         * Echoes lines in this text block to the consumer specified
         * @param fromLine  only start emitting lines from this line number
         * @param consumer  the consumer to receive lines
         */
        void lines(int fromLine, Consumer<String> consumer) throws IOException {
            int lineNumber = 0;
            int index = truncatedStart ? nextEndOfLine(0) : 0;
            while (index < size - 1) {
                final int start = adjustForward(index);
                index = nextEndOfLine(start);
                if (truncatedStart || isEndOfLine(index) || isEnd()) {
                    final int length = index - start;
                    if (length > 0 && lineNumber >= fromLine) {
                        final String line = new String(bytes, start, length);
                        consumer.accept(line);
                    }
                    lineNumber++;
                }
            }
        }


        /**
         * Returns the position of next end of line starting from the offset
         * @param offset    the offset the start search in buffer
         * @return          the position of the next end of line
         */
        private int nextEndOfLine(int offset) {
            int position = offset;
            while (position < size) {
                if (isEndOfLine(position)) {
                    return position;
                } else {
                    position++;
                }
            }
            return position < size ? position : size - 1;
        }


        /**
         * Returns the position of the prior end of line starting from the offset
         * @param offset    the offset to start search in buffer
         * @return          the position of the prior end of line
         */
        private int priorEndOfLine(int offset) {
            int position = offset;
            while (position >= 0) {
                if (isEndOfLine(position)) {
                    return position;
                } else {
                    position--;
                }
            }
            return position;
        }


        /**
         * Adjusts the index forward if it's at an end of line char
         * @param index the index to adjust forwarded
         * @return      the optionally adjusted index
         */
        private int adjustForward(int index) {
            if (!isEndOfLine(index)) {
                return index;
            } else {
                final byte value = bytes[index];
                final int count = value == '\n' ? 1 : value == '\r' ? 2 : 0;
                return index + count;
            }
        }


        /**
         * Returns true if the index is at an end of line char
         * @param index     the index in this buffer
         * @return          true if is at an end of line char
         */
        private boolean isEndOfLine(int index) {
            return bytes[index] == '\n' || (bytes[index] == '\r' && index + 1 < bytes.length && bytes[index+1] == '\n');
        }



        @Override()
        public String toString() {
            try {
                final long start = toFilePosition(0);
                final long end = toFilePosition(size -1);
                return String.format("TextBlock capacity=%s, size=%s, start=%s, end=%s", capacity, size, start, end);
            } catch (IOException ex) {
                return "TextBlock: " + ex.getMessage();
            }
        }
    }

}
