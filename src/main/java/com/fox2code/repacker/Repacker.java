package com.fox2code.repacker;

import com.fox2code.repacker.layout.DirLayout;
import com.fox2code.repacker.layout.MavenDirLayout;
import com.fox2code.repacker.patchers.ClientAnnotationPatcher;
import com.fox2code.repacker.patchers.Mapping;
import com.fox2code.repacker.patchers.PostPatcher;
import com.fox2code.repacker.utils.ConsoleColors;
import com.fox2code.repacker.utils.RepackException;
import com.fox2code.repacker.utils.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassVisitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/*
  * Repacker parrot editions
 */
public class Repacker {
    private final LinkedHashMap<String, String> cache;
    private final PrintStream out;
    private final DirLayout dirLayout;
    private String lastRelease;
    private String lastSnapshot;

    @Deprecated
    public Repacker(File cacheDir) {
        this(cacheDir, System.out);
    }

    @Deprecated
    public Repacker(File cacheDir,PrintStream out) {
        this(new MavenDirLayout(cacheDir), out);
    }

    public Repacker(DirLayout dirLayout) {
        this(dirLayout, System.out);
    }

    public Repacker(DirLayout dirLayout, PrintStream out) {
        this.dirLayout = dirLayout;
        this.cache = new LinkedHashMap<>();
        this.out = out;
    }

    public void repackClient(String version) throws IOException {
        this.repackClient(version, null);
    }

    public void repackClient(String version, PostPatcher postPatcher) throws IOException {
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
                this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading client jar..." + ConsoleColors.RESET);
                Utils.download(downloads.getAsJsonObject("client").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("client_mappings").get("url").getAsString(), "client");
            Mapping mappingSrv = getMappings(versionMappingsSrv, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");
            this.out.println(ConsoleColors.YELLOW_BRIGHT + "Parsing mapping..." + ConsoleColors.RESET);
            ClientAnnotationPatcher clientAnnotationPatcher = new ClientAnnotationPatcher(mapping, mappingSrv);
            this.out.println(ConsoleColors.YELLOW_BRIGHT + "Remapping client jar..." + ConsoleColors.RESET);
            mapping.remap(versionJar, versionJarRemap, new LogPatcher(clientAnnotationPatcher, postPatcher, "client"));
        }
    }

    public void downloadClient(String version) throws IOException {
        File versionJar = dirLayout.getMinecraftFile(version, true);
        if (!versionJar.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading client jar..." + ConsoleColors.RESET);
            Utils.download(downloads.getAsJsonObject("client").get("url").getAsString(), new FileOutputStream(versionJar));
        }
    }

    public void repackServer(String version) throws IOException {
        this.repackServer(version, null);
    }

    public void repackServer(String version,PostPatcher postPatcher) throws IOException {
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
                this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading server jar..." + ConsoleColors.RESET);
                Utils.download(downloads.getAsJsonObject("server").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");

            this.out.println(ConsoleColors.YELLOW_BRIGHT + "Remapping server jar..." + ConsoleColors.RESET);
            mapping.remap(versionJar, versionJarRemap, new LogPatcher(postPatcher, "server"));
        }
    }

    public void downloadServer(String version) throws IOException {
        File versionJar = dirLayout.getMinecraftFile(version, false);
        if (!versionJar.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading client jar..." + ConsoleColors.RESET);
            Utils.download(downloads.getAsJsonObject("server").get("url").getAsString(), new FileOutputStream(versionJar));
        }
    }

    public JsonObject getVersionManifest(String version) throws IOException {
        return this.getVersionManifest0(version, false);
    }

    private JsonObject getVersionManifest0(String version,boolean check) throws IOException {
        if (version == null) {
            throw new NullPointerException(ConsoleColors.RED_BOLD + "Version cannot be null!" + ConsoleColors.RESET);
        }
        String realVersion = version;
        if (check) {
            switch (version) {
                case "release":
                    realVersion = lastRelease;
                    break;
                case "snapshot":
                    realVersion = lastSnapshot;
                    break;
            }
        }
        File versionIndex = realVersion != null ?
                dirLayout.getVersionIndexFile(realVersion) : null;
        if (versionIndex != null && versionIndex.exists()) {
            try {
                return (JsonObject) JsonParser.parseString(Utils.readAll(new FileInputStream(versionIndex)));
            } catch (Exception e) {
                versionIndex.delete();
            }
        }

        File verCache = dirLayout.getIndexCache();
        if (cache.isEmpty() && verCache.exists() && realVersion != null) {
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
        String verURL = cache.get(realVersion);
        if (verURL == null || realVersion == null) {
            String json = Utils.get("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            Files.write(verCache.toPath(), json.getBytes(StandardCharsets.UTF_8));
            JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
            jsonObject.getAsJsonArray("versions").forEach(jsonElement -> {
                JsonObject object = jsonElement.getAsJsonObject();
                cache.put(object.get("id").getAsString(), object.get("url").getAsString());
            });
            jsonObject = jsonObject.getAsJsonObject("latest");
            lastRelease = jsonObject.get("release").getAsString();
            lastSnapshot = jsonObject.get("snapshot").getAsString();
            if (realVersion == null) {
                switch (version) {
                    case "release":
                        realVersion = lastRelease;
                        break;
                    case "snapshot":
                        realVersion = lastSnapshot;
                        break;
                }
            }
            verURL = cache.get(realVersion);
            if (verURL == null) {
                throw new RepackException("Missing version entry!");
            }
        }
        if (check) return null;
        if (versionIndex == null)
            versionIndex = dirLayout.getVersionIndexFile(realVersion);
        this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading "+realVersion+" manifest..." + ConsoleColors.RESET);
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
        this.out.println(ConsoleColors.YELLOW_BRIGHT + "Downloading "+type+" mappings..." + ConsoleColors.RESET);
        String mappings = Utils.get(fallBack);
        Files.write(file.toPath(), mappings.getBytes(StandardCharsets.UTF_8));
        return new Mapping(mappings);
    }

    public File getClientFile(String version) {
        return dirLayout.getMinecraftFile(version, true);
    }

    public File getServerFile(String version) {
        return dirLayout.getMinecraftFile(version, false);
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

    public DirLayout getDirLayout() {
        return dirLayout;
    }

    public String getLastRelease() {
        if (lastRelease == null) {
            try {
                this.getVersionManifest0("release", true);
            } catch (Exception ignored) {}
        }
        return lastRelease;
    }

    public String getLastSnapshot() {
        if (lastSnapshot == null) {
            try {
                this.getVersionManifest0("snapshot", true);
            } catch (Exception ignored) {}
        }
        return lastSnapshot;
    }

    public String realVersion(String version) {
        switch (version) {
            case "20w14∞":
                return "20w14infinite";
            case "release":
                return this.getLastRelease();
            case "snapshot":
                return this.getLastSnapshot();
            default:
                return version;
        }
    }

    public Set<String> getAllCachedVersions() {
        return Collections.unmodifiableSet(this.cache.keySet());
    }

    public Set<String> getAllVersions() {
        if (lastSnapshot == null) {
            try {
                this.getVersionManifest0("snapshot", true);
            } catch (Exception ignored) {}
        }
        return this.getAllCachedVersions();
    }

    /**
     * Indicate revision of generated bytecode by repacker
     * Increment each time the process of repack is modified
     */
    public final int repackRevision() {
        return Utils.REPACK_REVISION;
    }

    private class LogPatcher implements PostPatcher {
        private final PostPatcher postPatcher;
        private final PostPatcher postPatcherSec;
        private final String side;

        private LogPatcher(String side) {
            this.postPatcher = null;
            this.postPatcherSec = null;
            this.side = side;
        }

        private LogPatcher(PostPatcher postPatcher,String side) {
            this.postPatcher = postPatcher;
            this.postPatcherSec = null;
            this.side = side;
        }

        private LogPatcher(PostPatcher postPatcher, PostPatcher postPatcherSec,String side) {
            if (postPatcher == null && postPatcherSec != null) {
                this.postPatcher = postPatcherSec;
                this.postPatcherSec = null;
            } else {
                this.postPatcher = postPatcher;
                this.postPatcherSec = postPatcherSec;
            }
            this.side = side;
        }

        @Override
        public ClassVisitor patch(ClassVisitor classVisitor) {
            if (postPatcher != null) {
                classVisitor = postPatcher.patch(classVisitor);
                if (postPatcherSec != null) {
                    classVisitor = postPatcherSec.patch(classVisitor);
                }
            }
            return classVisitor;
        }

        @Override
        public void post(Map<String, byte[]> remapJar) {
            if (postPatcher != null) {
                postPatcher.post(remapJar);
                if (postPatcherSec != null) {
                    postPatcherSec.post(remapJar);
                }
            }
            out.println(ConsoleColors.YELLOW_BRIGHT + "Writing "+side+" jar..." + ConsoleColors.RESET);//<3<3<3<3<3
        }
    }
}
