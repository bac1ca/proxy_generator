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
            System.out.println("1");
            parser.parseArgument(args);
            System.out.println("2");

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
