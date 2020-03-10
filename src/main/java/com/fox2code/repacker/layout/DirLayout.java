package com.fox2code.repacker.layout;

import java.io.File;

public interface DirLayout {
    File getIndexCache();
    File getVersionIndexFile(String version);
    File getMappingFile(String version, boolean client);
    File getMinecraftFile(String version, boolean client);
    File getMinecraftRepackFile(String version, boolean client);
    void generateDirsFor(String version);
}
