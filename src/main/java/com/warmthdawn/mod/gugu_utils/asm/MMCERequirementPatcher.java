package com.warmthdawn.mod.gugu_utils.asm;

import org.objectweb.asm.*;

/**
 * ASM transformer to patch MMCE Addons requirement validation
 * to accept GuGu Utils environment components
 */
public class MMCERequirementPatcher implements Opcodes {
    
    /**
     * Transform RequirementDimension.isValidComponent to also accept environment components
     */
    public static byte[] transformRequirementDimension(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new RequirementDimensionVisitor(writer);
        
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
    
    /**
     * Transform RequirementBiome.isValidComponent to also accept environment components
     */
    public static byte[] transformRequirementBiome(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new RequirementBiomeVisitor(writer);
        
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
    
    private static class RequirementDimensionVisitor extends ClassVisitor {
        public RequirementDimensionVisitor(ClassVisitor cv) {
            super(ASM5, cv);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            
            if ("isValidComponent".equals(name) && "(Lhellfirepvp/modularmachinery/common/crafting/helper/ProcessingComponent;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Z".equals(desc)) {
                return new IsValidComponentTransformer(mv);
            }
            
            return mv;
        }
    }
    
    private static class RequirementBiomeVisitor extends ClassVisitor {
        public RequirementBiomeVisitor(ClassVisitor cv) {
            super(ASM5, cv);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            
            if ("isValidComponent".equals(name) && "(Lhellfirepvp/modularmachinery/common/crafting/helper/ProcessingComponent;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Z".equals(desc)) {
                return new IsValidComponentTransformer(mv);
            }
            
            return mv;
        }
    }
    
    private static class IsValidComponentTransformer extends MethodVisitor {
        public IsValidComponentTransformer(MethodVisitor mv) {
            super(ASM5, mv);
        }
        
        @Override
        public void visitCode() {
            super.visitCode();
            
            // Inject our compatibility check at the beginning of the method
            // if (MMCECompatibilityHelper.isEnvironmentComponentValid(component)) return true;
            mv.visitVarInsn(ALOAD, 1); // Load ProcessingComponent parameter
            mv.visitMethodInsn(INVOKESTATIC, 
                "com/warmthdawn/mod/gugu_utils/asm/MMCECompatibilityHelper", 
                "isEnvironmentComponentValid", 
                "(Lhellfirepvp/modularmachinery/common/crafting/helper/ProcessingComponent;)Z", 
                false);
            
            Label continueOriginal = new Label();
            mv.visitJumpInsn(IFEQ, continueOriginal);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(continueOriginal);
        }
    }
}
