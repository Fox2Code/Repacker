package com.fox2code.repacker;

import com.fox2code.repacker.layout.DirLayout;
import com.fox2code.repacker.layout.FlatDirLayout;
import com.fox2code.repacker.layout.MavenDirLayout;
import com.fox2code.repacker.utils.ConsoleColors;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try { // Note: Lib builds don't add jansi
            AnsiConsole.systemInstall();
        } catch (Throwable ignored) {}
        if (args.length != 2 && (args.length != 3 || !args[0].startsWith("-") || args[0].length() == 1)) {
            String color = (args.length == 0 ||
                    (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))))
                    ? ConsoleColors.BOLD : ConsoleColors.RED_BOLD;

            System.out.println(color + "usage: java -jar Repaker.jar --help");
            System.out.println(color + "usage: java -jar Repaker.jar (-parms) <cacheDir> <version>(-server)");
            System.out.println(color + "    -f => force repackage of the .jar");
            System.out.println(color + "    -m => use maven dir layout");
            System.out.println(color + "    -c => clean temporary files when finished");
            System.out.println(color + "    -d => download only without repack" + ConsoleColors.RESET);
            return;
        }
        int d = args.length - 2;
        boolean force = false, maven = false, clean = false, download = false;
        if (d != 0) {
            String p = args[0];
            int i = 1;
            while (i < p.length()) {
                char c = p.charAt(i);
                switch (c) {
                    default:
                        System.err.println(ConsoleColors.RED_BOLD +
                                "Unknown argument: -" + c + ConsoleColors.RESET);
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
                    case 'd':
                        download = true;
                        break;
                }
                i++;
            }
        }
        File file = new File(args[d]);
        if (!file.exists()) {
            System.out.println(ConsoleColors.RED_BOLD + "Cache dir doesn't exists!" + ConsoleColors.RESET);
            return;
        }
        boolean server = args[d+1].endsWith("-server");
        if (server) {
            args[d+1] = args[d+1].substring(0, args[d+1].length()-7);
        }
        try {
            Repacker repacker = new Repacker(maven ?
                    new MavenDirLayout(file) : new FlatDirLayout(file));
            args[d+1] = repacker.realVersion(args[d+1]);
            DirLayout dirLayout = repacker.getDirLayout();
            if (server) {
                if (force) {
                    if (download) {
                        repacker.getServerFile(args[d+1]).delete();
                    } else {
                        repacker.getServerRemappedFile(args[d+1]).delete();
                    }
                }
                if (download) {
                    repacker.downloadServer(args[d+1]);
                } else {
                    repacker.repackServer(args[d + 1]);
                }
                if (clean) {
                    System.out.println(ConsoleColors.YELLOW_BRIGHT +"Cleaning files..." + ConsoleColors.RESET);
                    if (!download) {
                        dirLayout.getMinecraftFile(args[d + 1], false).delete();
                    }
                }
            } else {
                if (force) {
                    if (download) {
                        repacker.getClientFile(args[d+1]).delete();
                    } else {
                        repacker.getClientRemappedFile(args[d+1]).delete();
                    }
                }
                if (download) {
                    repacker.downloadClient(args[d+1]);
                } else {
                    repacker.repackClient(args[d + 1]);
                }
                if (clean) {
                    System.out.println(ConsoleColors.YELLOW_BRIGHT + "Cleaning files..." + ConsoleColors.RESET);
                    if (!download) {
                        dirLayout.getMinecraftFile(args[d + 1], true).delete();
                        dirLayout.getMappingFile(args[d + 1], true).delete();
                    }
                }
            }
            if (clean) {
                if (!download) {
                    dirLayout.getMappingFile(args[d + 1], false).delete();
                }
                dirLayout.getVersionIndexFile(args[d+1]).delete();
                dirLayout.getIndexCache().delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println(ConsoleColors.GREEN_BRIGHT + "Finished!" + ConsoleColors.RESET);
        System.exit(0);
    }
}
