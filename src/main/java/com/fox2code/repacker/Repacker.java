package com.fox2code.repacker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

public class Repacker {
    private File cacheDir;
    private HashMap<String, String> cache;
    private PrintStream out;

    public Repacker(File cacheDir) {
        this(cacheDir, System.out);
    }

    public Repacker(File cacheDir,PrintStream out) {
        this.cacheDir = cacheDir;
        this.cache = new HashMap<>();
        this.out = out;
    }

    public void repackClient(String version) throws IOException {
        File versionIndex = new File(cacheDir, "net/minecraft/minecraft/"+version+"/"+version+".json");
        File versionJar = new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+".jar");
        File versionMappings = new File(cacheDir, "net/minecraft/minecraft/"+version+"/client-mappings.txt");
        File versionJarRemap = new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+"-remaped.jar");
        if (!versionIndex.exists() || !versionJar.exists() || !versionMappings.exists() || !versionJarRemap.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            if (!downloads.has("client_mappings")) {
                throw new RepackException("Missing Obfuscation mapping in current version!");
            }
            if (!versionJar.exists()) {
                this.out.println("Downloading client jar...");
                Utils.download(downloads.getAsJsonObject("client").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("client_mappings").get("url").getAsString(), "client");
            this.out.println("Remapping client jar...");
            mapping.remap(versionJar, versionJarRemap);
        }
    }

    public void repackServer(String version) throws IOException {
        File versionIndex = new File(cacheDir, "net/minecraft/minecraft/"+version+"/"+version+".json");
        File versionJar = new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+"-server.jar");
        File versionMappings = new File(cacheDir, "net/minecraft/minecraft/"+version+"/server-mappings.txt");
        File versionJarRemap = new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+"-server-remaped.jar");
        if (!versionIndex.exists() || !versionJar.exists() || !versionMappings.exists() || !versionJarRemap.exists()) {
            JsonObject jsonObject = getVersionManifest(version);
            JsonObject downloads = jsonObject.getAsJsonObject("downloads");
            if (!downloads.has("client_mappings")) {
                throw new RepackException("Missing Obfuscation mapping in current version!");
            }
            if (!versionJar.exists()) {
                this.out.println("Downloading server jar...");
                Utils.download(downloads.getAsJsonObject("server").get("url").getAsString(), new FileOutputStream(versionJar));
            }
            Mapping mapping = getMappings(versionMappings, downloads.getAsJsonObject("server_mappings").get("url").getAsString(), "server");
            this.out.println("Remapping server jar...");
            mapping.remap(versionJar, versionJarRemap);
        }
    }

    public JsonObject getVersionManifest(String version) throws IOException {
        if (version == null) {
            throw new NullPointerException("Version cannot be null!");
        }
        File versionIndex = new File(cacheDir, "net/minecraft/minecraft/"+version+"/"+version+".json");
        if (versionIndex.exists()) {
            try {
                return (JsonObject) JsonParser.parseString(Utils.readAll(new FileInputStream(versionIndex)));
            } catch (Exception e) {
                versionIndex.delete();
            }
        }

        File verCache = new File(cacheDir, "index-cache.json");
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
        new File(cacheDir ,"net/minecraft/minecraft/"+version).mkdirs();
        Files.write(versionIndex.toPath(), manifest.getBytes(StandardCharsets.UTF_8));
        return (JsonObject) JsonParser.parseString(manifest);
    }

    public Mapping getClientMappings(String version) throws IOException {
        File versionMappings = new File(cacheDir, "net/minecraft/minecraft/"+version+"/client-mappings.txt");
        JsonObject jsonObject = getVersionManifest(version);
        JsonObject downloads = jsonObject.getAsJsonObject("downloads");
        if (!downloads.has("client_mappings")) {
            throw new RepackException("Missing Obfuscation mapping in current version!");
        }
        return getMappings(versionMappings, downloads.getAsJsonObject("client_mappings").get("url").getAsString(), "client");
    }

    public Mapping getServerMappings(String version) throws IOException {
        File versionMappings = new File(cacheDir, "net/minecraft/minecraft/"+version+"/server-mappings.txt");
        JsonObject jsonObject = getVersionManifest(version);
        JsonObject downloads = jsonObject.getAsJsonObject("downloads");
        if (!downloads.has("server_mappings")) {
            throw new RepackException("Missing Obfuscation mapping in current version!");
        }
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
        return new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+"-remaped.jar");
    }

    public File getServerRemappedFile(String version) {
        return new File(cacheDir, "net/minecraft/minecraft/"+version+"/minecraft-"+version+"-server-remaped.jar");
    }
}
