package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class ParallelRequirementSupport {

    private ParallelRequirementSupport() {
    }

    static List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        // GuGu resource holders already simulate by mutating only the supplied
        // token when doOperation is false. The component/provider itself is
        // therefore safe to share while MMCE probes the available parallelism.
        return new ArrayList<>(components);
    }

    @SuppressWarnings("unchecked")
    static <V extends IResourceToken> boolean process(List<ProcessingComponent<?>> components,
                                                       V token,
                                                       IOType actionType,
                                                       boolean doOperation) {
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            if (!(provided instanceof ICraftingResourceHolder)) {
                continue;
            }

            ICraftingResourceHolder<V> handler = (ICraftingResourceHolder<V>) provided;
            boolean progressed;
            if (actionType == IOType.INPUT) {
                progressed = handler.canConsume() && handler.consume(token, doOperation);
            } else if (actionType == IOType.OUTPUT) {
                progressed = handler.canGenerate() && handler.generate(token, doOperation);
            } else {
                return false;
            }

            // Empty condition tokens (environment checks) still have to be
            // accepted by a handler; do not succeed solely because they carry
            // no numeric amount.
            if (progressed && token.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static boolean isOutputCheckIgnored(ComponentRequirement<?, ?> requirement) {
        if (requirement.getActionType() != IOType.OUTPUT) {
            return false;
        }
        try {
            Method method = ComponentRequirement.class.getMethod("isIgnoreOutputCheck");
            return Boolean.TRUE.equals(method.invoke(requirement));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Original MM and early MMCE builds do not expose this option.
            return false;
        }
    }
}
