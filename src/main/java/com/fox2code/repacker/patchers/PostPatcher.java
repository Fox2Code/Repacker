package com.fox2code.repacker.patchers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public interface PostPatcher extends Opcodes {
    PostPatcher NONE = new PostPatcher() {
        @Override
        public ClassVisitor patch(ClassVisitor classVisitor) {
            return classVisitor;
        }

        @Override
        public void post(Map<String, byte[]> remapJar) {}
    };

    ClassVisitor patch(ClassVisitor classVisitor);
    void post(Map<String,byte[]> remapJar);
}
