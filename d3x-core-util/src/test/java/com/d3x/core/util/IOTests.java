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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IOTests {


    @DataProvider(name="capacities")
    public Object[][] capacities() throws Exception {
        final URL resource = getClass().getResource("/io/test.csv");
        final File input = new File(resource.toURI());
        return new Object[][] {
            { input, 1924 },
            { input, 1024 * 10},
            { input, 1024 * 1024 }
        };
    }


    @Test(dataProvider="capacities")
    public void testTextBlockForward(File input, int capacity) throws Exception {
        final File output = File.createTempFile("text-block-forward", ".csv");
        try {
            final IO.TextBlock block = new IO.TextBlock(capacity, input).begin();
            echo(block, output);
        } finally {
            compare(input, output);
            if (output.delete()) {
                System.out.println("Deleted tmp file: " + output.getAbsolutePath());
            }
        }
    }


    @Test(dataProvider="capacities")
    public void testTextMovement(File input, int capacity) throws Exception {
        final File output = File.createTempFile("text-block-forward", ".csv");
        try {
            final IO.TextBlock block = new IO.TextBlock(capacity, input).begin();
            while (!block.isEnd()) block.moveDown();
            while (!block.isStart()) block.moveUp();
            echo(block, output);
        } finally {
            compare(input, output);
            if (output.delete()) {
                System.out.println("Deleted tmp file: " + output.getAbsolutePath());
            }
        }
    }


    @Test(dataProvider="capacities")
    public void testTail(File input, int capacity) throws Exception {
        final Path path = Paths.get(getClass().getResource("/io/tail-100.csv").toURI());
        final List<String> lines = new ArrayList<>();
        IO.tail(input, capacity, 100, lines::add);
        final List<String> expected = Files.readAllLines(path);
        Assert.assertEquals(lines.size(), expected.size(), "Same line count");
        for (int i=0; i<lines.size(); ++i) {
            final String left = expected.get(i);
            final String right = lines.get(i);
            Assert.assertEquals(right, left, "Lines match at line " + i);
        }
    }


    /**
     * Writes the text block to an output file until it gets to the end
     * @param block     the block to echo
     * @param output    the output file
     */
    private void echo(IO.TextBlock block, File output) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            while (true) {
                block.lines(0, line -> {
                    try {
                        writer.write(line);
                        writer.newLine();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                });
                if (block.isEnd()) {
                    break;
                } else {
                    block.moveDown();
                }
            }
        }
    }


    /**
     * Comapres two files to ensure they have the same content
     * @param left      the left file
     * @param right     the right file
     */
    private void compare(File left, File right) throws IOException {
        Assert.assertEquals(left.length(), right.length(), "The file sizes are the same");
        BufferedReader reader1 = null;
        BufferedReader reader2 = null;
        try {
            int line = 0;
            reader1 = new BufferedReader(new FileReader(left));
            reader2 = new BufferedReader(new FileReader(right));
            while (true) {
                final String leftLine = reader1.readLine();
                final String rightLine = reader2.readLine();
                if (leftLine == null && rightLine == null) {
                    break;
                } else {
                    Assert.assertEquals(leftLine, rightLine, "The lines match for line number " + line);
                    line++;
                }
            }
        } finally {
            IO.close(reader1, reader2);
        }
    }
}
