package com.fox2code.repacker.layout;

import java.io.File;

public class MavenDirLayout implements DirLayout {
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
        return new File(root, "net/minecraft/minecraft/"+version+"/minecraft-"+version+(client?"":"-server")+"-remapped.jar");
    }

    @Override
    public void generateDirsFor(String version) {
        new File(root, "net/minecraft/minecraft/"+version).mkdirs();
    }
}
