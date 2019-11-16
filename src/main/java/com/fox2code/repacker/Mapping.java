package com.fox2code.repacker;

import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Mapping extends Remapper {
    HashMap<String, String> map;
    HashMap<String, String> methods;
    HashMap<String, String> fields;

    public Mapping(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public Mapping(InputStream is) throws IOException {
        this(Utils.readAll(is));
    }

    public Mapping(String raw) {
        map = new HashMap<>();
        methods = new HashMap<>();
        fields = new HashMap<>();
        HashMap<String, String> reverseMap = new HashMap<>();
        String[] list = raw.split("\\n");
        for (String line:list) {
            if (line.startsWith("#") || line.startsWith(" ") || line.isEmpty()) {
                continue;
            }
            int index = line.indexOf(" -> ");
            String substring = line.substring(index + 4, line.length() - 1);
            reverseMap.put(line.substring(0, index), substring);
            map.put(substring.replace('.','/'), line.substring(0, index).replace('.','/'));
        }
        String context = "";
        for (String line:list) {
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            if (!line.startsWith(" ")) {
                int index = line.indexOf(" -> ");
                context = line.substring(index+4, line.length()-1).replace('.','/');
                continue;
            }
            line = line.substring(4);
            int index1 = line.indexOf(" ");
            int index2 = line.indexOf(" -> ");
            String first = line.substring(index1+1, index2);
            int index3 = first.indexOf('(');
            if (index3 == -1) {
                fields.put(context+":"+line.substring(index2+4), first);
                continue;
            }
            String[] args = first.substring(index3+1, first.length()-1).split(",");
            String output = line.substring(0, index1);
            output = output.substring(output.lastIndexOf(':')+1);
            StringBuilder desc = new StringBuilder();
            desc.append(context).append('.').append(line.substring(index2+4)).append('(');
            for (String arg:args) if (!arg.isEmpty()) {
                desc.append(descify(arg, reverseMap));
            }
            desc.append(')').append(descify(output, reverseMap));
            methods.put(desc.toString(), first.substring(0, index3));
        }
    }

    @Override
    public String map(String internalName) {
        return map.getOrDefault(internalName, internalName);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return fields.getOrDefault(owner+":"+name, name);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        return methods.getOrDefault(owner+"."+name+descriptor, name);
    }

    private static String descify(String str, HashMap<String, String> map) {
        if (str.endsWith("[]")) {
            return "["+descify(str.substring(0, str.length()-2), map);
        }
        switch (str) {
            default:
                str = map.getOrDefault(str, str);
                return "L"+str.replace('.','/')+";";
            case "int":
                return "I";
            case "boolean":
                return "Z";
            case "short":
                return "S";
            case "long":
                return "J";
            case "byte":
                return "B";
            case "float":
                return "F";
            case "void":
                return "V";
            case "char":
                return "C";
            case "double":
                return "D";
        }
    }

    public void remap(File in, File out) throws IOException {
        Utils.remap(in, out, this);
    }
}
