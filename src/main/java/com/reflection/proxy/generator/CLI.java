/*
 * The MIT License
 *
 * Copyright 2015.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.reflection.proxy.generator;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;


/**
 * Created by Vasily Romanikhin on 01/03/15.
 */
public class CLI {

    @Option(name="-c", required = true, usage="full class name")
    private String className;

    @Option(name="-i", usage="path to jar (or zip) which contains appropriate " +
            "interface/class/enum; if it's not specified only java runtime " +
            "classes will be available")
    private String pathToLib;

    @Option(name="-p", usage="package name (optional parameter), " +
            "if it's not specified EMPTY package name will be used")
    private String packageName = Generator.PACKAGE_NONE;

    @Option(name="-o", usage="path to output directory, " +
            "will be used for saving generated java class")
    private String outputDirectory = ".";


    public static void main(String[] args) throws IOException {
        new CLI().run(args);
    }

    public void run(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            Generator generator = pathToLib == null ?
                    new Generator(className, packageName) :
                    new Generator(pathToLib, className, packageName);
            Generator.GenerationResult result = generator.generate();

            final String path = outputDirectory + File.separator +
                                      result.getClassName() + ".java";
            new ClassWriter(path, result.getClassBody()).flushToFile();
            System.out.println("SUCCESS, saved to: " + path);
        } catch(CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println("java -jar proxy_generator.jar [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
        }
    }
}
