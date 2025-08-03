package com.warmthdawn.mod.gugu_utils.modularmachenary.environment;

import com.warmthdawn.mod.gugu_utils.modularmachenary.CommonMMTile;
import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementEnvironment;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic.CraftingResourceHolder;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic.IConsumable;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.warmthdawn.mod.gugu_utils.common.Constants.STRING_RESOURCE_ENVIRONMENT;

public class TileEnvironmentHatch extends CommonMMTile implements IConsumable<RequirementEnvironment.RT>, MachineComponentTile {

    // Check if MMCE Addons is available at runtime
    private static final boolean MMCE_ADDONS_AVAILABLE = checkMMCEAddonsAvailable();
    
    private static boolean checkMMCEAddonsAvailable() {
        try {
            Class.forName("github.alecsio.mmceaddons.common.crafting.requirement.RequirementDimension");
            Class.forName("github.alecsio.mmceaddons.common.crafting.requirement.RequirementBiome");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Nullable
    @Override
    public MachineComponent provideComponent() {
        return new MachineComponent(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return (ComponentType) MMCompoments.COMPONENT_ENVIRONMENT;
            }

            @Override
            public Object getContainerProvider() {
                return new CompatibilityWrapper();
            }
        };
    }

    @Override
    public boolean consume(RequirementEnvironment.RT outputToken, boolean doOperation) {
        if (outputToken.getType().isMeet(getWorld(), getPos())) {
            return true;
        }
        outputToken.setError("craftcheck.failure." + STRING_RESOURCE_ENVIRONMENT + "." + outputToken.getType().getName());
        return false;
    }

    /**
     * Compatibility wrapper that can handle both GuGu Utils and MMCE Addons requirements
     */
    private class CompatibilityWrapper extends CraftingResourceHolder<RequirementEnvironment.RT> {
        
        public CompatibilityWrapper() {
            super(TileEnvironmentHatch.this);
        }
        
        // MMCE Addons dimension requirement handling (via reflection)
        public CraftCheck canHandle(Object requirement) {
            if (!MMCE_ADDONS_AVAILABLE) return CraftCheck.failure("MMCE Addons not available");
            
            String className = requirement.getClass().getSimpleName();
            
            if ("RequirementDimension".equals(className)) {
                return handleDimensionRequirement(requirement);
            } else if ("RequirementBiome".equals(className)) {
                return handleBiomeRequirement(requirement);
            }
            
            return CraftCheck.failure("Unknown requirement type: " + className);
        }
        
        public void handle(Object requirement) {
            // No-op, just like MMCE Addons implementation
        }
        
        private CraftCheck handleDimensionRequirement(Object requirement) {
            try {
                // Use reflection to get dimension ID from RequirementDimension
                Method getDimensionMethod = requirement.getClass().getMethod("getDimension");
                Object dimension = getDimensionMethod.invoke(requirement);
                Method getIdMethod = dimension.getClass().getMethod("getId");
                int dimensionId = (Integer) getIdMethod.invoke(dimension);
                
                // Check if current dimension matches
                return TileEnvironmentHatch.this.world.provider.getDimension() == dimensionId ? 
                    CraftCheck.success() : 
                    CraftCheck.failure("error.modularmachineryaddons.requirement.missing.dimension");
            } catch (Exception e) {
                return CraftCheck.failure("Error checking dimension requirement: " + e.getMessage());
            }
        }
        
        private CraftCheck handleBiomeRequirement(Object requirement) {
            try {
                // Use reflection to get biome registry name from RequirementBiome
                Method getBiomeMethod = requirement.getClass().getMethod("getBiome");
                Object biome = getBiomeMethod.invoke(requirement);
                Method getRegistryNameMethod = biome.getClass().getMethod("getRegistryName");
                String requiredBiomeRegistryName = (String) getRegistryNameMethod.invoke(biome);
                
                // Check if current biome matches
                Biome currentBiome = TileEnvironmentHatch.this.world.getBiome(TileEnvironmentHatch.this.getPos());
                String currentBiomeRegistryName = Objects.requireNonNull(currentBiome.getRegistryName()).toString();
                
                return currentBiomeRegistryName.equalsIgnoreCase(requiredBiomeRegistryName) ? 
                    CraftCheck.success() : 
                    CraftCheck.failure("error.modularmachineryaddons.requirement.missing.biome");
            } catch (Exception e) {
                return CraftCheck.failure("Error checking biome requirement: " + e.getMessage());
            }
        }
    }
}
