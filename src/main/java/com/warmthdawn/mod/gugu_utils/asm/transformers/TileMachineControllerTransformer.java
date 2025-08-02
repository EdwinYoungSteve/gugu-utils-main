package com.warmthdawn.mod.gugu_utils.asm.transformers;

import com.warmthdawn.mod.gugu_utils.asm.common.MyTransformer;
import com.warmthdawn.mod.gugu_utils.asm.utils.AsmUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.Optional;

public class TileMachineControllerTransformer implements MyTransformer {
    @Override
    public void transform(ClassNode classNode) {
        Optional<MethodNode> tryColorize = AsmUtils.findMethod(classNode, "tryColorize", null);
        if (!tryColorize.isPresent()) {
            // 如果没有找到tryColorize方法，尝试在distributeCasingColor方法中找到lambda调用
            Optional<MethodNode> distributeCasingColor = AsmUtils.findMethod(classNode, "distributeCasingColor", null);
            if (distributeCasingColor.isPresent()) {
                transformDistributeCasingColor(distributeCasingColor.get());
                return;
            }
            return;
        }

        transformTryColorize(tryColorize.get());
    }
    
    private void transformTryColorize(MethodNode tryColorizeMethod) {
        InsnList ins = tryColorizeMethod.instructions;
        ListIterator<AbstractInsnNode> inserator = ins.iterator();
        while (inserator.hasNext()) {
            AbstractInsnNode in = inserator.next();

            if (!AsmUtils.matchMethodInsn(in, Opcodes.INVOKEVIRTUAL, "getTileEntity", "func_175625_s", null, null, null)) {
                continue;
            }
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 3));
            MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/warmthdawn/mod/gugu_utils/asm/mixin/MixinModularMachinery",
                "inject_tryColorize",
                "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/tileentity/TileEntity;)V", false);
            hook.add(call);

            ins.insert(in.getNext(), hook);
            return;
        }
    }
    
    private void transformDistributeCasingColor(MethodNode distributeCasingColorMethod) {
        // 在MMCE中，tryColorize是通过lambda表达式调用的
        // 我们需要找到lambda中的逻辑并注入我们的代码
        InsnList ins = distributeCasingColorMethod.instructions;
        ListIterator<AbstractInsnNode> inserator = ins.iterator();
        while (inserator.hasNext()) {
            AbstractInsnNode in = inserator.next();

            // 寻找对forEach的调用
            if (AsmUtils.matchMethodInsn(in, Opcodes.INVOKEINTERFACE, "forEach", null, null, null, null)) {
                // 我们需要创建一个新的lambda，它包含我们的额外逻辑
                // 这里比较复杂，暂时跳过
                return;
            }
        }
    }
}
