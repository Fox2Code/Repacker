package com.fox2code.repacker.utils;

import org.objectweb.asm.ClassVisitor;

import java.util.Map;

public interface PostPatcher {
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
