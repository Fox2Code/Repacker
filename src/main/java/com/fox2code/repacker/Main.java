package com.fox2code.repacker;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: java -jar Repaker.jar <cacheDir> <version>");
            return;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println("Cache dir doesn't exists!");
            return;
        }
        try {
            new Repacker(file).repackClient(args[1]);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}
