package com.fox2code.repacker.rebuild;

public interface ClassData {
    String getName();
    ClassData getSuperclass();
    ClassData[] getInterfaces();
    boolean isAssignableFrom(ClassData clData);
    boolean isInterface();
    boolean isFinal();
    boolean isPublic();
    boolean isCustom();
}
