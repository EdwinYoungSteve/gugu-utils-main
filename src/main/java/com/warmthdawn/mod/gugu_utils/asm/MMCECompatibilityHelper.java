package com.warmthdawn.mod.gugu_utils.asm;

import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;

/**
 * Helper class for ASM-injected compatibility checks
 */
public class MMCECompatibilityHelper {
    
    /**
     * Check if a processing component is a GuGu Utils environment component
     * that can handle MMCE Addons requirements
     */
    public static boolean isEnvironmentComponentValid(ProcessingComponent<?> component) {
        MachineComponent<?> cmp = component.getComponent();
        ComponentType envType = (ComponentType) MMCompoments.COMPONENT_ENVIRONMENT;
        
        // Check if it's our environment component
        if (cmp.getComponentType() == envType) {
            // Check if the container provider has our compatibility wrapper
            Object containerProvider = cmp.getContainerProvider();
            // The CompatibilityWrapper should have the canHandle method for MMCE requirements
            return hasCanHandleMethod(containerProvider);
        }
        
        return false;
    }
    
    private static boolean hasCanHandleMethod(Object obj) {
        try {
            // Check if the object has the canHandle method that MMCE Addons expects
            obj.getClass().getMethod("canHandle", Object.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
