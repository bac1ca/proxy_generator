package com.reflection.proxy.generator;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineException;

import static com.reflection.proxy.generator.Generator.GenerationResult;

public class Main {

    public static void main(String[] args) throws Exception {
        Generator generator = new Generator(
                "java.util.Collection",
                Generator.PACKAGE_NONE);
        GenerationResult result = generator.generate();

        System.out.println(result.getClassName());
        System.out.println(result.getClassBody());
        throw new CmdLineException("sasa");
    }
}
