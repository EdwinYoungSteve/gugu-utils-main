package com.warmthdawn.mod.gugu_utils.modularmachenary;

import com.warmthdawn.mod.gugu_utils.common.IRestorableTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 兼容原版和MMCE的基础机器组件TileEntity
 * 通过反射和接口实现来同时支持两个版本的模块化机械
 */
public class UniversalMMTile extends TileEntity implements IColorableTileEntity {
    public static final String TAG_MACHINE_COLOR = "machine_color";
    protected int machineColor;
    
    private static final boolean MMCE_AVAILABLE = checkMMCEAvailable();
    
    static {
        // 尝试在运行时为这个类添加MMCE接口的实现
        if (MMCE_AVAILABLE) {
            try {
                implementMMCEInterface();
            } catch (Exception e) {
                // 静默处理异常
            }
        }
    }
    
    public UniversalMMTile() {
        super();
        this.machineColor = getDefaultMachineColor();
    }
    
    private static boolean checkMMCEAvailable() {
        try {
            Class.forName("hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static void implementMMCEInterface() {
        // 这里我们会在运行时修改类定义，但这在Java中很复杂
        // 更简单的方法是让子类直接实现接口
    }
    
    private static int getDefaultMachineColor() {
        try {
            Class<?> configClass = Class.forName("hellfirepvp.modularmachinery.common.data.Config");
            return configClass.getField("machineColor").getInt(null);
        } catch (Exception e) {
            return 0x313131; // 默认颜色
        }
    }

    @Override
    public int getMachineColor() {
        return this.machineColor;
    }

    @Override
    public void setMachineColor(int newColor) {
        this.machineColor = newColor;
        //同步
        IBlockState state = world.getBlockState(this.getPos());
        world.notifyBlockUpdate(this.getPos(), state, state, 1 | 2);
        this.markDirty();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock();
    }

    public void readNBT(NBTTagCompound compound) {
        if (compound.hasKey(TAG_MACHINE_COLOR))
            this.machineColor = compound.getInteger(TAG_MACHINE_COLOR);
    }

    public void writeNBT(NBTTagCompound compound) {
        compound.setInteger(TAG_MACHINE_COLOR, this.getMachineColor());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if(this instanceof IRestorableTileEntity){
            ((IRestorableTileEntity) this).readRestorableFromNBT(compound);
        }
        this.readNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.writeNBT(compound);
        if(this instanceof IRestorableTileEntity){
            ((IRestorableTileEntity) this).writeRestorableToNBT(compound);
        }
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound updateTag = super.getUpdateTag();
        writeNBT(updateTag);
        return updateTag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readNBT(packet.getNbtCompound());
    }
}
