package com.fox2code.repacker;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2 && (args.length != 3 || !args[0].startsWith("-") || args[0].length() == 1)) {
            System.out.println("usage: java -jar Repaker.jar (-parms) <cacheDir> <version>(-server)");
            System.out.println("    -f => force repackage of the .jar");
            System.out.println("    -m => use maven dir layout");
            System.out.println("    -c => clean temporary files when finished");
            return;
        }
        int d = args.length - 2;
        boolean force = false, maven = false, clean = false;
        if (d != 0) {
            String p = args[0];
            int i = 1;
            while (i < p.length()) {
                char c = p.charAt(i);
                switch (c) {
                    default:
                        System.err.println("Unknown argument: -"+c);
                        System.exit(-2);
                        return;
                    case 'f':
                        force = true;
                        break;
                    case 'm':
                        maven = true;
                        break;
                    case 'c':
                        clean = true;
                        break;
                }
                i++;
            }
        }
        File file = new File(args[d]);
        if (!file.exists()) {
            System.out.println("Cache dir doesn't exists!");
            return;
        }
        boolean server = args[d+1].endsWith("-server");
        if (server) {
            args[d+1] = args[d+1].substring(0, args[d+1].length()-7);
        }
        try {
            Repacker repacker = new Repacker(maven ?
                    new DirLayout.MavenDirLayout(file) : new DirLayout.FlatDirLayout(file));
            DirLayout dirLayout = repacker.getDirLayout();
            if (server) {
                if (force) {
                    repacker.getServerRemappedFile(args[d+1]).delete();
                }
                repacker.repackServer(args[d+1]);
                if (clean) {
                    System.out.println("Cleaning files...");
                    dirLayout.getMinecraftFile(args[d+1], false).delete();
                }
            } else {
                if (force) {
                    repacker.getClientRemappedFile(args[d+1]).delete();
                }
                repacker.repackClient(args[d+1]);
                if (clean) {
                    System.out.println("Cleaning files...");
                    dirLayout.getMinecraftFile(args[d+1], true).delete();
                    dirLayout.getMappingFile(args[d+1], true).delete();
                }
            }
            if (clean) {
                dirLayout.getMappingFile(args[d+1], false).delete();
                dirLayout.getVersionIndexFile(args[d+1]).delete();
                dirLayout.getIndexCache().delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Finished!");
        System.exit(0);
    }
}
