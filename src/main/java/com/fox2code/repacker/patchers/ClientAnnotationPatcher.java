package com.fox2code.repacker.patchers;

import com.fox2code.repacker.utils.Utils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class ClientAnnotationPatcher implements PostPatcher, Opcodes {
    private final Mapping.ReverseMapping client;
    private final Mapping.ReverseMapping server;

    private static String repackerCst() {
        return "repacker"; // Hacky way to not get repacked by shadowJar
    }

    public ClientAnnotationPatcher(Mapping client,Mapping server) {
        this.client = client.getReverseMapping();
        this.server = server.getReverseMapping();
    }

    @Override
    public ClassVisitor patch(ClassVisitor classVisitor) {
        return new ClassPatcher(classVisitor);
    }

    @Override
    public void post(Map<String, byte[]> remapJar) {
        if (Utils.cjo != null) {
            remapJar.put("com/fox2code/"+repackerCst()+"/ClientJarOnly.class", Utils.cjo);
        }
    }

    public boolean isClientOnly(String className) {
        return client.cl.contains(className) && !server.cl.contains(className);
    }

    public boolean isClientOnly(String className, String name) {
        return client.f.contains(className+"#"+name) && !server.f.contains(className+"#"+name);
    }

    public boolean isClientOnly(String className, String name, String desc) {
        return client.m.contains(className+"#"+name) && !server.m.contains(className+"#"+name);
    }

    private class ClassPatcher extends ClassVisitor {
        boolean skip;
        String clName;

        public ClassPatcher(ClassVisitor classVisitor) {
            super(ASM7, classVisitor);
            this.skip = false;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.clName = name;
            if (isClientOnly(name)) {
                this.skip = true;
                super.visitAnnotation("Lcom/fox2code/"+repackerCst()+"/ClientJarOnly;", false).visitEnd();
            } else if ((access & ACC_ANNOTATION) != 0) {
                this.skip = true;
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
            if (!skip) {
                if (isClientOnly(this.clName, name)) {
                    fieldVisitor.visitAnnotation("Lcom/fox2code/"+repackerCst()+"/ClientJarOnly;", false).visitEnd();
                }
            }
            return fieldVisitor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!skip) {
                if (isClientOnly(this.clName, name, descriptor)) {
                    methodVisitor.visitAnnotation("Lcom/fox2code/"+repackerCst()+"/ClientJarOnly;", false).visitEnd();
                }
            }
            return methodVisitor;
        }
    }
}
