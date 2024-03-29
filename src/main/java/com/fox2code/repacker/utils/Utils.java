package com.fox2code.repacker.utils;

import com.fox2code.repacker.patchers.BytecodeFixer;
import com.fox2code.repacker.patchers.PostPatcher;
import com.fox2code.repacker.rebuild.ClassData;
import com.fox2code.repacker.rebuild.ClassDataProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static final int ASM_BUILD = Opcodes.ASM9;
    public static final int REPACK_REVISION = 7;
    private static final int THREADS = Math.max(2,
            ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
    public static boolean debugRemapping = "true".equalsIgnoreCase(
            System.getProperty("repacker.debug.remap", System.getProperty("repacker.debug")));
    private static final String charset = "UTF-8";
    public static byte[] cjo;

    static {
        try {
            InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream("ClientJarOnly.class.repacker");
            if (inputStream == null) {
                System.err.println(ConsoleColors.RED_BRIGHT +
                        "Err: missing /ClientJarOnly.class.repacker" + ConsoleColors.RESET);
            } else {
                cjo = readAllBytes(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        connection.setRequestProperty("Connection", "keep-alive");

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

        return buffer.toString(charset);
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while (( nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        inputStream.close();

        return buffer.toByteArray();
    }

    public static void download(String url,OutputStream outputStream) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Connection", "keep-alive");

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
        remap(in, out, mapping, null);
    }

    public static void remap(File in, File out, Remapper mapping, PostPatcher postPatcher) throws IOException {
        if (postPatcher == null) {
            postPatcher = PostPatcher.NONE;
        }
        Map<String,byte[]> orig = Utils.readZIP(Files.newInputStream(in.toPath()));
        ClassDataProvider origCDP = new ClassDataProvider(null);
        origCDP.addClasses(orig);
        mapping = new CtxRemapper(mapping, origCDP);
        Map<String,byte[]> remap = new ConcurrentHashMap<>();
        Thread[] threads = new Thread[THREADS];
        int t = 0;
        final PostPatcher _postPatcher = postPatcher;
        final Remapper _mapping = mapping;
        String base = Thread.currentThread().getName()+" -> Repacker tId:";
        for (final Map.Entry<String,byte[]> entry:orig.entrySet()) {
            if (entry.getKey().endsWith(".class")) {
                if (threads[t] != null) try {
                    threads[t].join();
                } catch (InterruptedException ie) {
                    throw new RepackException("Interupted", ie);
                }
                (threads[t] = new Thread(() -> {
                    ClassReader classReader = new ClassReader(entry.getValue());
                    ClassWriter classWriter = new ClassWriter(classReader, 0);
                    classReader.accept(new ClassRemapper(new BytecodeFixer(_postPatcher.patch(classWriter)), _mapping), 0);
                    String name = entry.getKey();
                    remap.put(_mapping.map(name.substring(0, name.length()-6))+".class", classWriter.toByteArray());
                }, base+t)).start();
                t = (t+1) % threads.length;
            } else if (!entry.getKey().startsWith("META-INF/")) {
                remap.put(entry.getKey(), entry.getValue());
            }
        }
        for (Thread thread:threads) if (thread != null) {
            try {
                threads[t].join();
            } catch (InterruptedException ie) {
                throw new RepackException("Interupted", ie);
            }
        }
        StringBuilder stringBuilder = new StringBuilder(
                "Manifest-Version: 1.0\nRepack-Revision: "+
                        REPACK_REVISION + "\n");
        postPatcher.appendManifest(stringBuilder);
        String text = stringBuilder.toString();
        if (!text.endsWith("\n")) text += "\n";
        remap.put("META-INF/MANIFEST.MF", text.getBytes(StandardCharsets.UTF_8));
        postPatcher.post(remap);
        Utils.writeZIP(remap, Files.newOutputStream(out.toPath()));
    }

    private static class CtxRemapper extends Remapper {
        private final Remapper parent;
        private final ClassDataProvider cdp;

        public CtxRemapper(Remapper remapper,ClassDataProvider classDataProvider) {
            this.parent = remapper;
            this.cdp = classDataProvider;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            return this.mapMethodName(owner, name, descriptor, true);
        }

        public String mapMethodName(String owner, String name, String descriptor,boolean root) {
            if (name.equals("<init>") || name.equals("<clinit>") || owner.startsWith("java/") || owner.startsWith("[")) {
                return name;
            }
            final String oldOwner = debugRemapping ? owner : null;
            if (owner.endsWith(";")) {
                System.out.println("ERROR: "+owner+"."+name+descriptor+" is an invalid method!");
            }
            String newName = parent.mapMethodName(owner, name, descriptor);
            if (newName.equals(name)) {
                for (ClassData classData:cdp.getClassData(owner).getInterfaces()) {
                    newName = this.mapMethodName(classData.getName(), name, descriptor, false);
                    if (!newName.equals(name)) {
                        break;
                    }
                }
            }
            while (newName.equals(name) && !owner.equals("java/lang/Object")) {
                owner = cdp.getClassData(owner).getSuperclass().getName();
                newName = this.mapMethodName(owner, name, descriptor, false);
            }
            if (oldOwner != null && name.equals(newName) && root) {
                System.out.println(ConsoleColors.YELLOW_BRIGHT + "DEBUG: Method resolution failed for -> ("+oldOwner+") "+this.mapType(oldOwner)+"."+name+this.mapDesc(descriptor)+
                        (cdp.getClassData(oldOwner).getSuperclass().getName().equals("java/lang/Object") ? " with no parent": "") + ConsoleColors.RESET);
            }
            return newName;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            return this.mapFieldName(owner, name, descriptor, true);
        }

        public String mapFieldName(String owner, String name, String descriptor,boolean root) {
            if (owner.startsWith("java/")) {
                return name;
            }
            final String oldOwner = debugRemapping ? owner : null;
            String newName = parent.mapFieldName(owner, name, descriptor);
            if (newName.equals(name)) {
                for (ClassData classData:cdp.getClassData(owner).getInterfaces()) {
                    newName = this.mapFieldName(classData.getName(), name, descriptor, false);
                    if (!newName.equals(name)) {
                        break;
                    }
                }
            }
            while (newName.equals(name) && !owner.equals("java/lang/Object")) {
                owner = cdp.getClassData(owner).getSuperclass().getName();
                newName = this.mapFieldName(owner, name, descriptor, false);
            }
            if (oldOwner != null && name.equals(newName) && root) {
                System.out.println(ConsoleColors.YELLOW_BRIGHT + "DEBUG: Field resolution failed for "+this.mapType(oldOwner)+"#"+name+" "+this.mapDesc(descriptor)+
                        (cdp.getClassData(oldOwner).getSuperclass().getName().equals("java/lang/Object") ? " with no parent": "") + ConsoleColors.RESET);
            }
            return newName;
        }

        @Override
        public String mapDesc(String descriptor) {
            return parent.mapDesc(descriptor);
        }

        @Override
        public String mapType(String internalName) {
            return parent.mapType(internalName);
        }

        @Override
        public String map(String internalName) {
            return parent.map(internalName);
        }
    }

    /**
     * Optimised function to count parameters
     * Return -1 if input is invalid
     */
    public static int countParms(String desc)
    {
        int i = 0, p = 0,s = 0;
        boolean valid;
        char c;
        if (desc.charAt(i) == '<') {
            i++;
            while (i < desc.length()) {
                if (desc.charAt(i) == '(') {
                    break;
                }
                i++;
            }
        }
        if (desc.charAt(i) != '(') {
            return -1;
        }
        i++;
        while ((valid = (desc.length() != i)) && (c = desc.charAt(i)) != ')')
        {
            if (c != '[') {
                p++;
                if (c == 'L' || c == 'T') {
                    i++;
                    while ((valid = (desc.length() != i)) && ((c = desc.charAt(i)) != ';' || s != 0)) {
                        if ('<' == c) {
                            s++;
                        }
                        if ('>' == c) {
                            s--;
                            if (s < 0) {
                                return -1;
                            }
                        }
                        i++;
                    }
                    if (!valid) {
                        return -1;
                    }
                }
            }
            i++;
        }
        return valid ? p : -1;
    }

    public static int countIndexParms(String desc)
    {
        int i = 0, in = 0,s = 0;
        boolean valid;
        char c;
        if (desc.charAt(i) == '<') {
            i++;
            while (i < desc.length()) {
                if (desc.charAt(i) == '(') {
                    break;
                }
                i++;
            }
        }
        if (desc.charAt(i) != '(') {
            return -1;
        }
        i++;
        while ((valid = (desc.length() != i)) && (c = desc.charAt(i)) != ')')
        {
            if (c != '[') {
                in++;
                if (c == 'L' || c == 'T') {
                    i++;
                    while ((valid = (desc.length() != i)) && ((c = desc.charAt(i)) != ';' || s != 0)) {
                        if ('<' == c) {
                            s++;
                        }
                        if ('>' == c) {
                            s--;
                            if (s < 0) {
                                return -1;
                            }
                        }
                        i++;
                    }
                    if (!valid) {
                        return -1;
                    }
                } else if (c == 'J' || c == 'D'){
                    in++;
                }
            }
            i++;
        }
        return valid ? in : -1;
    }

    public static int indexForParm(String desc,int parm)
    {
        if (parm == 0) return 0;
        int i = 1, p = 0,s = 0, in = 0;
        boolean valid;
        char c;
        if (desc.charAt(0) != '(') {
            return -1;
        }
        while ((valid = (desc.length() != i)) && (c = desc.charAt(i)) != ')')
        {
            if (c != '[') {
                p++;
                in++;
                if (c == 'L') {
                    i++;
                    while ((valid = (desc.length() != i)) && ((c = desc.charAt(i)) != ';' || s != 0)) {
                        if ('<' == c) {
                            s++;
                        }
                        if ('>' == c) {
                            s--;
                            if (s < 0) {
                                return -1;
                            }
                        }
                        i++;
                    }
                    if (!valid) {
                        return -1;
                    }
                } else if (c == 'J' || c == 'D'){
                    in++;
                }
                if (p == parm) {
                    return in;
                }
            }
            i++;
        }
        return valid ? in : -1;
    }
}
