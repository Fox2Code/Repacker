package com.fox2code.repacker;

import java.io.File;

public interface DirLayout {
    File getIndexCache();
    File getVersionIndexFile(String version);
    File getMappingFile(String version, boolean client);
    File getMinecraftFile(String version, boolean client);
    File getMinecraftRepackFile(String version, boolean client);
    void generateDirsFor(String version);

    class MavenDirLayout implements DirLayout {
        protected File root;

        public MavenDirLayout(File root) {
            this.root = root;
            this.root.mkdirs();
        }

        @Override
        public File getIndexCache() {
            return new File(root, "index-cache.json");
        }

        @Override
        public File getVersionIndexFile(String version) {
            return new File(root, "net/minecraft/minecraft/"+version+"/"+version+".json");
        }

        @Override
        public File getMappingFile(String version, boolean client) {
            return new File(root, "net/minecraft/minecraft/"+version+"/"+(client?"client":"server")+"-mappings.txt");
        }

        @Override
        public File getMinecraftFile(String version, boolean client) {
            return new File(root, "net/minecraft/minecraft/"+version+"/minecraft-"+version+(client?"":"-server")+".jar");
        }

        @Override
        public File getMinecraftRepackFile(String version, boolean client) {
            return new File(root, "net/minecraft/minecraft/"+version+"/minecraft-"+version+(client?"":"-server")+"-remaped.jar");
        }

        @Override
        public void generateDirsFor(String version) {
            new File(root, "net/minecraft/minecraft/"+version).mkdirs();
        }
    }

    class FlatDirLayout implements DirLayout {
        protected File root;

        public FlatDirLayout(File root) {
            this.root = root;
            this.root.mkdirs();
        }

        @Override
        public File getIndexCache() {
            return new File(root, "index-cache.json");
        }

        @Override
        public File getVersionIndexFile(String version) {
            return new File(root, version+".json");
        }

        @Override
        public File getMappingFile(String version, boolean client) {
            return new File(root, version+(client?"":"-server")+"-mappings.txt");
        }

        @Override
        public File getMinecraftFile(String version, boolean client) {
            return new File(root, "minecraft-"+version+(client?"":"-server")+".jar");
        }

        @Override
        public File getMinecraftRepackFile(String version, boolean client) {
            return new File(root, "minecraft-"+version+(client?"":"-server")+"-remaped.jar");
        }

        @Override
        public void generateDirsFor(String version) {}
    }
}
