package com.fox2code.repacker.layout;

import java.io.File;

public class FlatDirLayout implements DirLayout {
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
