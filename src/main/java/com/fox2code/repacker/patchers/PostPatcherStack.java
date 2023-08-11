package com.fox2code.repacker.patchers;

import org.objectweb.asm.ClassVisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class PostPatcherStack implements PostPatcher, Iterable<PostPatcher> {
    private final LinkedList<PostPatcher> linkedList = new LinkedList<>();

    public PostPatcherStack() {}

    public PostPatcherStack(PostPatcher... patchers) {
        for (PostPatcher postPatcher:patchers) {
            this.addPatcher(postPatcher);
        }
    }

    public PostPatcherStack(Iterable<PostPatcher> patchers) {
        for (PostPatcher postPatcher:patchers) {
            this.addPatcher(postPatcher);
        }
    }

    @Override
    public ClassVisitor patch(ClassVisitor classVisitor) {
        for (PostPatcher postPatcher: linkedList) {
            classVisitor = postPatcher.patch(classVisitor);
        }
        return classVisitor;
    }

    @Override
    public void post(Map<String, byte[]> remapJar) {
        for (PostPatcher postPatcher: linkedList) {
            postPatcher.post(remapJar);
        }
    }

    @Override
    public void appendManifest(StringBuilder stringBuilder) {
        for (PostPatcher postPatcher: linkedList) {
            postPatcher.appendManifest(stringBuilder);
        }
    }

    @Override
    public Iterator<PostPatcher> iterator() {
        return linkedList.iterator();
    }

    public void addPatcher(PostPatcher postPatcher) {
        if (postPatcher != null && postPatcher != PostPatcher.NONE) {
            linkedList.add(postPatcher);
        }
    }

    public static PostPatcher stack(PostPatcher... patchers) {
        if (patchers.length == 0) {
            return PostPatcher.NONE;
        } else if (patchers.length == 1) {
            return patchers[0];
        } else {
            return new PostPatcherStack(patchers);
        }
    }
}
