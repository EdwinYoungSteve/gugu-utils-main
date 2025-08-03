package com.warmthdawn.mod.gugu_utils.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * FML Loading Plugin to register our class transformer
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("GuGu Utils Core")
@IFMLLoadingPlugin.TransformerExclusions("com.warmthdawn.mod.gugu_utils.asm")
public class GuGuUtilsCorePlugin implements IFMLLoadingPlugin {
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.warmthdawn.mod.gugu_utils.asm.GuGuUtilsClassTransformer"};
    }
    
    @Override
    public String getModContainerClass() {
        return null;
    }
    
    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
        // No additional data injection needed
    }
    
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
