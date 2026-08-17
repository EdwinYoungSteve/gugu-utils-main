package com.warmthdawn.mod.gugu_utils.asm.transformers;

import com.warmthdawn.mod.gugu_utils.asm.common.MyTransformer;
import org.objectweb.asm.tree.ClassNode;

public class RemoveInterfaceTransformer implements MyTransformer {

    private final String interfaceName;

    public RemoveInterfaceTransformer(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public void transform(ClassNode classNode) {
        classNode.interfaces.remove(interfaceName);
    }
}
