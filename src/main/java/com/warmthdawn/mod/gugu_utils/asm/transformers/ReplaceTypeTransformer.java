package com.warmthdawn.mod.gugu_utils.asm.transformers;

import com.warmthdawn.mod.gugu_utils.asm.common.MyTransformer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ListIterator;

/** Rewrites an optional API type before the JVM resolves the transformed class. */
public class ReplaceTypeTransformer implements MyTransformer {

    private final String source;
    private final String target;

    public ReplaceTypeTransformer(String source, String target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void transform(ClassNode classNode) {
        classNode.interfaces.replaceAll(name -> name.equals(source) ? target : name);
        if (classNode.signature != null) {
            classNode.signature = replace(classNode.signature);
        }
        for (FieldNode field : classNode.fields) {
            field.desc = replace(field.desc);
            if (field.signature != null) {
                field.signature = replace(field.signature);
            }
        }
        for (MethodNode method : classNode.methods) {
            method.desc = replace(method.desc);
            if (method.signature != null) {
                method.signature = replace(method.signature);
            }
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                    if (methodInsn.owner.equals(source)) {
                        methodInsn.owner = target;
                    }
                    methodInsn.desc = replace(methodInsn.desc);
                } else if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                    if (fieldInsn.owner.equals(source)) {
                        fieldInsn.owner = target;
                    }
                    fieldInsn.desc = replace(fieldInsn.desc);
                } else if (instruction instanceof TypeInsnNode) {
                    TypeInsnNode typeInsn = (TypeInsnNode) instruction;
                    if (typeInsn.desc.equals(source)) {
                        typeInsn.desc = target;
                    }
                }
            }
        }
    }

    private String replace(String descriptor) {
        return descriptor.replace(source, target);
    }
}
