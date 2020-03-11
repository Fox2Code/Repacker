package com.fox2code.repacker.patchers;

import com.fox2code.repacker.rebuild.ClassDataProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.Map;

/**
 * Inline opcodes and cache hash to boost performances
 * Compensate loss of perf due to class names being longer
 */
public class MthOptimiser implements Opcodes {
    private static final ClassDataProvider cdp = new ClassDataProvider(null);
    private static final String[] hashCache = new String[]{
            "net/minecraft/core/Vec3i.class",
            "net/minecraft/world/phys/Vec3.class",
            "net/minecraft/world/phys/AABB.class"
    };

    public static void postOptimise(Map<String, byte[]> remapJar) {
        byte[] mth = remapJar.get("net/minecraft/util/Mth.class");
        if (mth != null) {
            remapJar.put("net/minecraft/util/Mth.class", mthOptimise(mth));
        }
        for (String cl:hashCache) {
            mth = remapJar.get(cl);
            if (mth != null) {
                remapJar.put(cl, hashCache(mth));
            }
        }
    }

    public static byte[] mthOptimise(byte[] mth) {
        ClassReader classReader = new ClassReader(mth);
        ClassWriter classWriter = cdp.newClassWriter();
        classReader.accept(new ClassVisitor(ASM7, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(ASM7, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == INVOKESTATIC && (owner.equals("java/lang/Math") || owner.equals("java/lang/StrictMath") || owner.equals("net/minecraft/util/Mth"))) {
                            if (owner.equals("java/lang/Math") && (name.equals("sqrt") || name.equals("sin") || name.equals("cos") || name.equals("asin") || name.equals("acos"))) {
                                owner = "java/lang/StrictMath";
                            }
                            if (!inline0(this, opcode, owner, name, descriptor, isInterface)) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        }, 0);
        return classWriter.toByteArray();
    }

    public static byte[] hashCache(byte[] cl) {
        ClassReader classReader = new ClassReader(cl);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        classNode.fields.add(new FieldNode(ACC_PRIVATE, "hash", "I", null, null));
        classNode.methods.forEach(methodNode -> {
            if (methodNode.name.equals("hashCode")) {
                methodNode.name = "hashCode0";
                methodNode.access = ACC_PRIVATE;
            }
        });
        MethodNode methodNode;
        classNode.methods.add(methodNode = new MethodNode(ACC_PUBLIC, "hashCode", "()I", null, null));
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new FieldInsnNode(GETFIELD, classNode.name, "hash", "I"));
        methodNode.instructions.add(new InsnNode(DUP));
        LabelNode labelNode = new LabelNode();
        methodNode.instructions.add(new JumpInsnNode(IFEQ, labelNode));
        methodNode.instructions.add(new InsnNode(IRETURN));
        methodNode.instructions.add(labelNode);
        methodNode.instructions.add(new InsnNode(POP));
        methodNode.instructions.add(new VarInsnNode(ALOAD, 0));
        methodNode.instructions.add(new InsnNode(DUP));
        methodNode.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name, "hashCode0", "()I"));
        methodNode.instructions.add(new InsnNode(DUP_X1));
        methodNode.instructions.add(new FieldInsnNode(PUTFIELD, classNode.name, "hash", "I"));
        methodNode.instructions.add(new InsnNode(IRETURN));
        ClassWriter classWriter = cdp.newClassWriter();
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    // Taken from UDK see UDK CodeFixer for more up to date implementation
    private static boolean inline0(MethodVisitor methodVisitor, int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (descriptor.indexOf('I') != -1) {
            switch (name) {
                default:
                    return false;
                case "abs": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP);
                    methodVisitor.visitJumpInsn(IFGE, label);
                    methodVisitor.visitInsn(INEG);
                    methodVisitor.visitLabel(label);
                    break;
                }
                case "max": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP2);
                    methodVisitor.visitJumpInsn(IF_ICMPGE, label);
                    methodVisitor.visitInsn(SWAP);
                    methodVisitor.visitLabel(label);
                    methodVisitor.visitInsn(DUP);
                    break;
                }
                case "min": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP2);
                    methodVisitor.visitJumpInsn(IF_ICMPLE, label);
                    methodVisitor.visitInsn(SWAP);
                    methodVisitor.visitLabel(label);
                    methodVisitor.visitInsn(DUP);
                    break;
                }
            }
        } else if (descriptor.indexOf('D') != -1) {
            switch (name) {
                default:
                    return false;
                case "toRadians":
                    methodVisitor.visitLdcInsn(180D);
                    methodVisitor.visitInsn(DDIV);
                    methodVisitor.visitLdcInsn(Math.PI);
                    methodVisitor.visitInsn(DMUL);
                    break;
                case "toDegrees":
                    methodVisitor.visitLdcInsn(180D);
                    methodVisitor.visitInsn(DMUL);
                    methodVisitor.visitLdcInsn(Math.PI);
                    methodVisitor.visitInsn(DDIV);
                    break;
                case "abs": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP2);
                    methodVisitor.visitInsn(DCONST_0);
                    methodVisitor.visitInsn(DCMPG);
                    methodVisitor.visitJumpInsn(IFGE, label);
                    methodVisitor.visitInsn(DNEG);
                    methodVisitor.visitLabel(label);
                    break;
                }
            }
        } else if (descriptor.indexOf('F') != -1) {
            switch (name) {
                default:
                    return false;
                case "abs": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP);
                    methodVisitor.visitInsn(FCONST_0);
                    methodVisitor.visitInsn(FCMPG);
                    methodVisitor.visitJumpInsn(IFGE, label);
                    methodVisitor.visitInsn(FNEG);
                    methodVisitor.visitLabel(label);
                    break;
                }
                case "max": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP2);
                    methodVisitor.visitInsn(FCMPL);
                    methodVisitor.visitJumpInsn(IFGE, label);
                    methodVisitor.visitInsn(SWAP);
                    methodVisitor.visitLabel(label);
                    methodVisitor.visitInsn(POP);
                    break;
                }
                case "min": {
                    Label label = new Label();
                    methodVisitor.visitInsn(DUP2);
                    methodVisitor.visitInsn(FCMPL);
                    methodVisitor.visitJumpInsn(IFLE, label);
                    methodVisitor.visitInsn(SWAP);
                    methodVisitor.visitLabel(label);
                    methodVisitor.visitInsn(POP);
                    break;
                }
            }
        }
        return true;
    }
}
