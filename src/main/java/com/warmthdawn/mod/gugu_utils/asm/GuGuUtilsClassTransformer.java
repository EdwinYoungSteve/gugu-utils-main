package com.warmthdawn.mod.gugu_utils.asm;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * FML Class Transformer to patch MMCE Addons for compatibility
 */
public class GuGuUtilsClassTransformer implements IClassTransformer {
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        
        switch (transformedName) {
            case "github.alecsio.mmceaddons.common.crafting.requirement.RequirementDimension":
                System.out.println("[GuGu Utils] Transforming RequirementDimension for compatibility");
                return MMCERequirementPatcher.transformRequirementDimension(basicClass);
                
            case "github.alecsio.mmceaddons.common.crafting.requirement.RequirementBiome":
                System.out.println("[GuGu Utils] Transforming RequirementBiome for compatibility");
                return MMCERequirementPatcher.transformRequirementBiome(basicClass);
                
            default:
                return basicClass;
        }
    }
}
