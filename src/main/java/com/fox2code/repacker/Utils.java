package com.fox2code.repacker;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static final String charset = "UTF-8";

    public static String post(String url, String param ) throws IOException {

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true); // Triggers POST.
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(param.getBytes(charset));
        }

        InputStream inputStream = connection.getInputStream();

        return readAll(inputStream);
    }

    public static String get(String url) throws IOException {

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);

        InputStream inputStream = connection.getInputStream();

        return readAll(inputStream);
    }


    public static String readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        inputStream.close();

        return new String(buffer.toByteArray(), charset);
    }

    public static void download(String url,OutputStream outputStream) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);

        InputStream inputStream = connection.getInputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, nRead);
        }

        outputStream.flush();
        outputStream.close();
    }

    public static Map<String,byte[]> readZIP(InputStream in) throws IOException {
        ZipInputStream inputStream = new ZipInputStream(in);
        Map<String,byte[]> items = new HashMap<>();
        ZipEntry entry;
        while (null!=(entry=inputStream.getNextEntry())) {
            if (!entry.isDirectory()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[2048];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    baos.write(data, 0, nRead);
                }
                items.put(entry.getName(), baos.toByteArray());
            }
        }
        in.close();
        return items;
    }

    public static void writeZIP(Map<String, byte[]> items, final OutputStream out) throws IOException {
        final ZipOutputStream zip = new ZipOutputStream(out);
        for (final String path : items.keySet()) {
            final byte[] data = items.get(path);
            final ZipEntry entry = new ZipEntry(path);
            zip.putNextEntry(entry);
            zip.write(data);
        }
        zip.flush();
        zip.close();
    }

    public static void remap(File in, File out, Remapper mapping) throws IOException {
        Map<String,byte[]> orig = Utils.readZIP(new FileInputStream(in));
        Map<String,byte[]> remap = new HashMap<>();
        for (Map.Entry<String,byte[]> entry:orig.entrySet()) {
            if (entry.getKey().endsWith(".class")) {
                ClassReader classReader = new ClassReader(entry.getValue());
                ClassWriter classWriter = new ClassWriter(classReader, 0);
                classReader.accept(new ClassRemapper(classWriter, mapping), 0);
                String name = entry.getKey();
                remap.put(mapping.map(name.substring(0, name.length()-6))+".class", classWriter.toByteArray());
            } else if (!entry.getKey().startsWith("META-INF/")) {
                remap.put(entry.getKey(), entry.getValue());
            }
        }
        Utils.writeZIP(remap, new FileOutputStream(out));
    }
}
