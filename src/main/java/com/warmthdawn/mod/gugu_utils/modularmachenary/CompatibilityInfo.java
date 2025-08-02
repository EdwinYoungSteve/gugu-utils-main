package com.warmthdawn.mod.gugu_utils.modularmachenary;

/**
 * GuGuUtils模块化机械兼容性测试和说明
 * 
 * 本次修复解决了从原版模块化机械到MMCE（ModularMachinery-Community-Edition）
 * 迁移后颜色同步功能失效的问题。
 * 
 * 问题根源：
 * 1. MMCE将tryColorize方法从TileMachineController移动到了TileMultiblockMachineController基类
 * 2. 颜色接口从IColorableTileEntity改名为ColorableMachineTile
 * 3. ASM注入的目标类和方法发生了变化
 * 
 * 解决方案：
 * 1. 更新ASM转换器以同时支持两个类
 * 2. 创建兼容性适配器ColorableTileCompat，通过反射处理接口差异
 * 3. 更新混入代码使用兼容性适配器
 * 4. 保持向后兼容性，同时支持原版和MMCE
 * 
 * 技术细节：
 * - 原版：hellfirepvp.modularmachinery.common.tiles.TileMachineController.tryColorize()
 * - MMCE：hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController.tryColorize()
 * 
 * - 原版接口：com.warmthdawn.mod.gugu_utils.modularmachenary.IColorableTileEntity
 * - MMCE接口：hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile
 * 
 * 兼容性：
 * - 向后兼容原版模块化机械
 * - 向前兼容MMCE
 * - 运行时检测版本并采用相应的策略
 */
public class CompatibilityInfo {
    
    public static final String VERSION = "1.0.0";
    public static final String[] SUPPORTED_MM_VERSIONS = {
        "ModularMachinery-1.12.2", // 原版
        "ModularMachinery-Community-Edition" // 社区版
    };
    
    /**
     * 检查当前环境的兼容性状态
     */
    public static String getCompatibilityStatus() {
        StringBuilder status = new StringBuilder();
        status.append("GuGuUtils Modular Machinery Compatibility Status:\n");
        
        // 检查原版接口
        boolean hasOriginalInterface = checkClass("com.warmthdawn.mod.gugu_utils.modularmachenary.IColorableTileEntity");
        status.append("Original IColorableTileEntity: ").append(hasOriginalInterface ? "✓" : "✗").append("\n");
        
        // 检查MMCE接口
        boolean hasMMCEInterface = checkClass("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
        status.append("MMCE ColorableMachineTile: ").append(hasMMCEInterface ? "✓" : "✗").append("\n");
        
        // 检查原版控制器
        boolean hasOriginalController = checkClass("hellfirepvp.modularmachinery.common.tiles.TileMachineController");
        status.append("Original TileMachineController: ").append(hasOriginalController ? "✓" : "✗").append("\n");
        
        // 检查MMCE基类控制器
        boolean hasMMCEController = checkClass("hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController");
        status.append("MMCE TileMultiblockMachineController: ").append(hasMMCEController ? "✓" : "✗").append("\n");
        
        // 检查配置类
        boolean hasConfig = checkClass("hellfirepvp.modularmachinery.common.data.Config");
        status.append("MM Config class: ").append(hasConfig ? "✓" : "✗").append("\n");
        
        return status.toString();
    }
    
    private static boolean checkClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
