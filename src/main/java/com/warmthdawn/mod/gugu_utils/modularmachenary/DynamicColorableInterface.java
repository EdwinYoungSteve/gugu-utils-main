package com.warmthdawn.mod.gugu_utils.modularmachenary;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 动态接口实现工具，用于在运行时让TileEntity实现MMCE的ColorableMachineTile接口
 */
public class DynamicColorableInterface {
    
    private static final boolean MMCE_AVAILABLE = Loader.isModLoaded("modularmachinery") && checkMMCEColorableInterface();
    private static Class<?> mmceColorableInterface;
    
    static {
        if (MMCE_AVAILABLE) {
            try {
                mmceColorableInterface = Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            } catch (ClassNotFoundException e) {
                // 静默处理
            }
        }
    }
    
    private static boolean checkMMCEColorableInterface() {
        try {
            Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 创建一个代理对象，让给定的TileEntity同时实现原版和MMCE的颜色接口
     */
    public static Object createColorableProxy(TileEntity originalTile) {
        if (!MMCE_AVAILABLE || !(originalTile instanceof IColorableTileEntity)) {
            return originalTile;
        }
        
        IColorableTileEntity colorableTile = (IColorableTileEntity) originalTile;
        
        return Proxy.newProxyInstance(
            originalTile.getClass().getClassLoader(),
            new Class<?>[]{mmceColorableInterface},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "getMachineColor":
                            return colorableTile.getMachineColor();
                        case "setMachineColor":
                            if (args.length == 1 && args[0] instanceof Integer) {
                                colorableTile.setMachineColor((Integer) args[0]);
                                return null;
                            }
                            break;
                    }
                    
                    // 对于其他方法，尝试调用原始对象的方法
                    try {
                        return method.invoke(originalTile, args);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke method: " + methodName, e);
                    }
                }
            }
        );
    }
    
    /**
     * 检查是否支持MMCE接口
     */
    public static boolean isMMCEAvailable() {
        return MMCE_AVAILABLE;
    }
}
