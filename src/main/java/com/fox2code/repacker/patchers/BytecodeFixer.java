package com.fox2code.repacker.patchers;

import com.fox2code.repacker.utils.Utils;
import org.objectweb.asm.*;

import java.util.HashMap;

/**
 * Fix and Improve remapped Bytecode
 * Mainly help debug
 */
public class BytecodeFixer extends ClassVisitor implements Opcodes {
    public BytecodeFixer(ClassVisitor classVisitor) {
        super(ASM7, classVisitor);
    }

    private String sourceName;
    private String self;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        int i, i$;
        this.sourceName = name.substring((i = name.lastIndexOf('/'))+1, (((i$ = name.indexOf('$')) != -1) && i < i$ )? i$ : name.length())+".java";
        this.self = name;
        super.visit(version, access, name, signature == null || signature.indexOf('<') == -1 ? null : signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource((source == null || source.equals("SourceFile")) ? this.sourceName : source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access,final String mName,final String mDescriptor, String signature, String[] exceptions) {
        final int parms = Utils.countParms(mDescriptor);
        final boolean isStatic = (access & ACC_STATIC) != 0;
        final int limit = Utils.countIndexParms(mDescriptor)+(isStatic?0:1);
        return new MethodVisitor(ASM7, super.visitMethod(access, mName, mDescriptor, (signature == null
                || Utils.countParms(signature) != parms) ? null : signature , exceptions)) {
            int i;
            final HashMap<Integer, String> parmsNames = new HashMap<>();

            @Override
            public void visitVarInsn(int opcode, int var) {
                super.visitVarInsn(opcode, var);
                if (opcode >= 21 && opcode <= 25) {
                    i = var;
                } else {
                    i = 0;
                }
            }

            @Override
            public void visitInsn(int opcode) {
                super.visitInsn(opcode);
                i = 0;
            }

            @Override
            public void visitLdcInsn(Object value) {
                super.visitLdcInsn(value);
                i = 0;
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                super.visitFieldInsn(opcode, owner, name, descriptor);
                if ((opcode == PUTFIELD || opcode == PUTSTATIC) && i != 0 && limit > i && owner.equals(self)) {
                    parmsNames.putIfAbsent(i, name);
                }
                i = 0;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                if ((opcode == INVOKEVIRTUAL || opcode == INVOKESPECIAL) && i != 0 && limit > i && (opcode == INVOKESPECIAL || owner.equals(self))) {
                    if (name.startsWith("set") && name.length() > 3 && Utils.countParms(descriptor) == 1) {
                        parmsNames.putIfAbsent(i, name.substring(3, 4).toLowerCase()+name.substring(4));
                    }
                }
                i = 0;
            }

            private String nameFromDesc(String descriptor) {
                String name = descriptor.substring(descriptor.lastIndexOf('/') + 1, descriptor.lastIndexOf('/') + 2).toLowerCase()
                        + descriptor.substring(descriptor.lastIndexOf('/') + 2, descriptor.length() - 1);
                if (descriptor.indexOf(0) == '[') {
                    if (name.charAt(name.length()-1) == 's') {
                        name = name + "es";
                    } else {
                        name = name + "s";
                    }
                }
                return name;
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                if (index == 0 && !isStatic) {
                    name = "this";
                } else if (name.length() == 1 && name.charAt(0) > 128) {
                    if (parms == 1 && index == (isStatic ? 0 : 1)) {
                        if (descriptor.length() == 1) {
                            switch (descriptor.charAt(0)) {
                                default:
                                    name = descriptor.toLowerCase();
                                    break;
                                case 'Z':
                                    name = "b";
                                    break;
                                case 'J':
                                    name = "l";
                                    break;
                            }
                        } else {
                            name = nameFromDesc(descriptor);
                        }
                        if (mName.startsWith("getBy") && mName.length() > 5) {
                            name = mName.substring(5,6).toLowerCase()+mName.substring(6);
                        } else if (mName.startsWith("from") && mName.length() > 4) {
                            name = mName.substring(4,5).toLowerCase()+mName.substring(5);
                        } else if (mName.startsWith("set") && mName.length() > 3 && Character.isUpperCase(mName.charAt(3))) {
                            name = mName.substring(3,4).toLowerCase()+mName.substring(4);
                        } else if (mName.startsWith("by") && mName.length() > 2 && Character.isUpperCase(mName.charAt(2))) {
                            name = mName.substring(2,3).toLowerCase()+mName.substring(3);
                        } else if (isStatic && descriptor.charAt(0) == '[' && mName.equals("main")) {
                            name = "args";
                        } else {
                            name = parmsNames.getOrDefault(index, name);
                        }
                    } else {
                        name = (descriptor.charAt(0) == '[' ? "vars" : "var")+index;
                        if ((descriptor.charAt(0) == 'L' || descriptor.charAt(0) == '[') &&
                                ! mDescriptor.substring(mDescriptor.indexOf(descriptor)+descriptor.length()).contains(descriptor)) {
                            name = nameFromDesc(descriptor);
                        }
                        int i2 = isStatic ? i + 1 : i;
                        if (parms == 3 && i2 > 0 && i2 <= 5 && mDescriptor.startsWith("(DDD)")) {
                            if (mName.toLowerCase().contains("color")) {
                                switch (i2) {
                                    case 1:
                                        name = "r";
                                        break;
                                    case 3:
                                        name = "g";
                                        break;
                                    case 5:
                                        name = "b";
                                        break;
                                }
                            } else {
                                switch (i2) {
                                    case 1:
                                        name = "x";
                                        break;
                                    case 3:
                                        name = "y";
                                        break;
                                    case 5:
                                        name = "z";
                                        break;
                                }
                            }
                        }
                        name = parmsNames.getOrDefault(index, name);
                    }
                }
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
            }
        };
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, (signature == null || signature.indexOf('<') == -1) ? null : signature, value);
    }
}
