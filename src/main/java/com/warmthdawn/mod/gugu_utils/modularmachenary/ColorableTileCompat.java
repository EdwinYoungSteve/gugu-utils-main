package com.warmthdawn.mod.gugu_utils.modularmachenary;

import net.minecraft.tileentity.TileEntity;

/**
 * 兼容性类，用于处理原版和MMCE的颜色同步接口差异
 */
public class ColorableTileCompat {
    
    private static final boolean MMCE_AVAILABLE = checkMMCEAvailable();
    
    private static boolean checkMMCEAvailable() {
        try {
            Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 检查TileEntity是否支持颜色设置（兼容原版和MMCE）
     */
    public static boolean isColorable(TileEntity te) {
        if (te instanceof IColorableTileEntity) {
            return true;
        }
        
        if (MMCE_AVAILABLE) {
            return isMMCEColorable(te);
        }
        
        // 额外检查：如果TileEntity有getMachineColor和setMachineColor方法，也认为它是可着色的
        return hasColorMethods(te);
    }
    
    /**
     * 设置TileEntity的颜色（兼容原版和MMCE）
     */
    public static void setMachineColor(TileEntity te, int color) {
        if (te instanceof IColorableTileEntity) {
            ((IColorableTileEntity) te).setMachineColor(color);
            return;
        }
        
        if (MMCE_AVAILABLE) {
            setMMCEMachineColor(te, color);
            return;
        }
        
        // 通过反射尝试调用setMachineColor方法
        setColorByReflection(te, color);
    }
    
    /**
     * 获取TileEntity的颜色（兼容原版和MMCE）
     */
    public static int getMachineColor(TileEntity te) {
        if (te instanceof IColorableTileEntity) {
            return ((IColorableTileEntity) te).getMachineColor();
        }
        
        if (MMCE_AVAILABLE) {
            return getMMCEMachineColor(te);
        }
        
        // 通过反射尝试获取颜色
        return getColorByReflection(te);
    }
    
    /**
     * 检查TileEntity是否有颜色相关的方法
     */
    private static boolean hasColorMethods(TileEntity te) {
        try {
            te.getClass().getMethod("getMachineColor");
            te.getClass().getMethod("setMachineColor", int.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * 通过反射设置颜色
     */
    private static void setColorByReflection(TileEntity te, int color) {
        try {
            te.getClass().getMethod("setMachineColor", int.class).invoke(te, color);
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    /**
     * 通过反射获取颜色
     */
    private static int getColorByReflection(TileEntity te) {
        try {
            return (Integer) te.getClass().getMethod("getMachineColor").invoke(te);
        } catch (Exception e) {
            return -1;
        }
    }
    
    // 通过反射处理MMCE接口，避免直接依赖
    private static boolean isMMCEColorable(TileEntity te) {
        try {
            Class<?> colorableClass = Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            return colorableClass.isInstance(te);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void setMMCEMachineColor(TileEntity te, int color) {
        try {
            Class<?> colorableClass = Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            if (colorableClass.isInstance(te)) {
                colorableClass.getMethod("setMachineColor", int.class).invoke(te, color);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    private static int getMMCEMachineColor(TileEntity te) {
        try {
            Class<?> colorableClass = Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            if (colorableClass.isInstance(te)) {
                return (Integer) colorableClass.getMethod("getMachineColor").invoke(te);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
        return -1;
    }
}
