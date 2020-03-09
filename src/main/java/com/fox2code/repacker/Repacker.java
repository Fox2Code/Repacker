package com.fox2code.repacker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassVisitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Repacker {
    private HashMap<String, String> cache;
    private PrintStream out;
    private DirLayout dirLayout;

    @Deprecated
    public Repacker(File cacheDir) {
        this(cacheDir, System.out);
    }

    @Deprecated
    public Repacker(File cacheDir,PrintStream out) {
        this(new DirLayout.MavenDirLayout(cacheDir), out);
    }

    public Repacker(DirLayout dirLayout) {
        this(dirLayout, System.out);
    }

    public Repacker(DirLayout dirLayout, PrintStream out) {
        this.dirLayout = dirLayout;
        this.cache = new HashMap<>();
        this.out = out;
    }

    public void repackClient(String version) throws IOException {
        File versionIndex = dirLayout.getVersionIndexFile(version);
        File versionJar = dirLayout.getMinecraftFile(version, true);
        File versionMappings = dirLayout.getMappingFile(version, true);
        File versionMappingsSrv = dirLayout.getMappingFile(version, false);
        File versionJarRemap = dirLayout.getMinecraftRepackFile(version, true);
        if (!versionIndex.exists() || !versionJar.exists() || !versionMappings.exists() || !versionJarRemap.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            if (!downloads.has("client_mappings")) {
                throw new RepackException("Missing Obfuscation mapping in the current version!");
            }
            if (!versionJar.exists()) {
                this.out.println("Downloading client jar...");
                Utils.download(downloads.getAsJsonObject("client").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("client_mappings").get("url").getAsString(), "client");
            Mapping mappingSrv = getMappings(versionMappingsSrv, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");
            this.out.println("Parsing mapping...");
            ClientAnnotationPatcher clientAnnotationPatcher = new ClientAnnotationPatcher(mapping, mappingSrv);
            this.out.println("Remapping client jar...");
            mapping.remap(versionJar, versionJarRemap, new LogPatcher(clientAnnotationPatcher, "client"));
        }
    }

    public void repackServer(String version) throws IOException {
        File versionIndex = dirLayout.getVersionIndexFile(version);
        File versionJar = dirLayout.getMinecraftFile(version, false);
        File versionMappings = dirLayout.getMappingFile(version, false);
        File versionJarRemap = dirLayout.getMinecraftRepackFile(version, false);
        if (!versionIndex.exists() || !versionJar.exists() || !versionMappings.exists() || !versionJarRemap.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            if (!downloads.has("client_mappings")) {
                throw new RepackException("Missing Obfuscation mapping in the current version!");
            }
            if (!versionJar.exists()) {
                this.out.println("Downloading server jar...");
                Utils.download(downloads.getAsJsonObject("server").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");
            this.out.println("Remapping server jar...");
            mapping.remap(versionJar, versionJarRemap, new LogPatcher("server"));
        }
    }

    public JsonObject getVersionManifest(String version) throws IOException {
        if (version == null) {
            throw new NullPointerException("Version cannot be null!");
        }
        File versionIndex = dirLayout.getVersionIndexFile(version);
        if (versionIndex.exists()) {
            try {
                return (JsonObject) JsonParser.parseString(Utils.readAll(new FileInputStream(versionIndex)));
            } catch (Exception e) {
                versionIndex.delete();
            }
        }

        File verCache = dirLayout.getIndexCache();
        if (cache.isEmpty() && verCache.exists()) {
            try {
                JsonObject jsonObject = (JsonObject) JsonParser.parseString(Utils.readAll(new FileInputStream(versionIndex)));
                jsonObject.getAsJsonArray().forEach(jsonElement -> {
                    JsonObject object = jsonElement.getAsJsonObject();
                    cache.put(object.get("id").getAsString(), object.get("url").getAsString());
                });
            } catch (Exception e) {
                verCache.delete();
            }
        }
        String verURL = cache.get(version);
        if (verURL == null) {
            String json = Utils.get("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            Files.write(verCache.toPath(), json.getBytes(StandardCharsets.UTF_8));
            JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
            jsonObject.getAsJsonArray("versions").forEach(jsonElement -> {
                JsonObject object = jsonElement.getAsJsonObject();
                cache.put(object.get("id").getAsString(), object.get("url").getAsString());
            });
            verURL = cache.get(version);
            if (verURL == null) {
                throw new RepackException("Missing version entry!");
            }
        }
        this.out.println("Downloading "+version+" manifest...");
        String manifest = Utils.get(verURL);
        dirLayout.generateDirsFor(version);
        Files.write(versionIndex.toPath(), manifest.getBytes(StandardCharsets.UTF_8));
        return (JsonObject) JsonParser.parseString(manifest);
    }

    public Mapping getClientMappings(String version) throws IOException {
        File versionMappings = dirLayout.getMappingFile(version, true);
        JsonObject jsonObject = getVersionManifest(version);
        JsonObject downloads = jsonObject.getAsJsonObject("downloads");
        return getMappings(versionMappings, downloads.getAsJsonObject("client_mappings").get("url").getAsString(), "client");
    }

    public Mapping getServerMappings(String version) throws IOException {
        File versionMappings = dirLayout.getMappingFile(version, false);
        JsonObject jsonObject = getVersionManifest(version);
        JsonObject downloads = jsonObject.getAsJsonObject("downloads");
        return getMappings(versionMappings, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");
    }

    public Mapping getMappings(File file,String fallBack,String type) throws IOException {
        if (file.exists()) {
            return new Mapping(Utils.readAll(new FileInputStream(file)));
        }
        this.out.println("Downloading "+type+" mappings...");
        String mappings = Utils.get(fallBack);
        Files.write(file.toPath(), mappings.getBytes(StandardCharsets.UTF_8));
        return new Mapping(mappings);
    }

    public File getClientRemappedFile(String version) {
        return dirLayout.getMinecraftRepackFile(version, true);
    }

    public File getServerRemappedFile(String version) {
        return dirLayout.getMinecraftRepackFile(version, false);
    }

    public File getClientMappingFile(String version) {
        return dirLayout.getMappingFile(version, true);
    }

    public File getServerMappingFile(String version) {
        return dirLayout.getMappingFile(version, false);
    }

    /**
     * Use {@link #getDirLayout()} instead
     */
    @Deprecated
    public File getVersionRemapDir(String version) {
        return dirLayout.getVersionIndexFile(version).getParentFile();
    }

    public DirLayout getDirLayout() {
        return dirLayout;
    }

    /**
     * Indicate revision of generated bytecode by repacker
     * Increment each time the process of repack is modified
     */
    public final int repackRevision() {
        return Utils.REPACK_REVISION;
    }

    private class LogPatcher implements PostPatcher {
        private PostPatcher postPatcher;
        private String side;

        private LogPatcher(String side) {
            this.postPatcher = null;
            this.side = side;
        }

        private LogPatcher(PostPatcher postPatcher,String side) {
            this.postPatcher = postPatcher;
            this.side = side;
        }

        @Override
        public ClassVisitor patch(ClassVisitor classVisitor) {
            return postPatcher == null ? classVisitor : postPatcher.patch(classVisitor);
        }

        @Override
        public void post(Map<String, byte[]> remapJar) {
            if (postPatcher != null) {
                postPatcher.post(remapJar);
            }
            out.println("Writing "+side+" jar...");
        }
    }
}
