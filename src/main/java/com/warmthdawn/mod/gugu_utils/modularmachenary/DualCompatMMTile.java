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
 * 兼容原版和MMCE的机器组件基类
 * 
 * 这个类实现了原版的IColorableTileEntity接口
 * 同时通过方法名匹配，让MMCE能够通过反射调用相同的方法
 * 
 * MMCE的ColorableMachineTile接口也有getMachineColor()和setMachineColor(int)方法
 * 所以只要方法签名匹配，MMCE就能通过反射或接口检查识别这个类
 */
public class DualCompatMMTile extends TileEntity implements IColorableTileEntity {
    public static final String TAG_MACHINE_COLOR = "machine_color";
    protected int machineColor;
    
    public DualCompatMMTile() {
        super();
        this.machineColor = getDefaultMachineColor();
    }
    
    private static int getDefaultMachineColor() {
        try {
            Class<?> configClass = Class.forName("hellfirepvp.modularmachinery.common.data.Config");
            return configClass.getField("machineColor").getInt(null);
        } catch (Exception e) {
            return 0x313131; // 默认颜色
        }
    }

    // 实现IColorableTileEntity接口（原版）
    @Override
    public int getMachineColor() {
        return this.machineColor;
    }

    @Override
    public void setMachineColor(int newColor) {
        this.machineColor = newColor;
        //同步
        if (world != null) {
            IBlockState state = world.getBlockState(this.getPos());
            world.notifyBlockUpdate(this.getPos(), state, state, 1 | 2);
            this.markDirty();
        }
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
