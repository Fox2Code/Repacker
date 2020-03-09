package com.fox2code.repacker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.CONSTRUCTOR,ElementType.METHOD,ElementType.FIELD,ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface ClientJarOnly {
// TODO Automacily move the class in /repacker.ClientJarOnly.class.res on build
}
