package com.reflection.proxy.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Vasily Romanikhin on 28/02/15.
 */
public class ClassWriter {

    private final String filePath;
    private final String fileData;

    public ClassWriter(String fileName, String fileData) {
        this.filePath = fileName;
        this.fileData = fileData;
    }

    public void flushToFile() {
        final File file = new File(filePath);
        try (BufferedWriter output = new BufferedWriter(new FileWriter(file))) {
            output.write(fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
