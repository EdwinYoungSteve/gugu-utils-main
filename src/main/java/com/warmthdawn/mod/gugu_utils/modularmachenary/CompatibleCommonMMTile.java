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
 * 兼容性基类TileEntity，同时实现原版和MMCE的颜色接口
 */
public class CompatibleCommonMMTile extends TileEntity implements IColorableTileEntity {
    public static final String TAG_MACHINE_COLOR = "machine_color";
    protected int machineColor = getDefaultMachineColor();

    private static int getDefaultMachineColor() {
        try {
            // 尝试获取MMCE的默认颜色
            Class<?> configClass = Class.forName("hellfirepvp.modularmachinery.common.data.Config");
            return configClass.getField("machineColor").getInt(null);
        } catch (Exception e) {
            // 如果失败，返回默认值
            return 0x313131;
        }
    }

    // 实现IColorableTileEntity接口
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
        // Here we get the packet from the server and read it into our client side tile entity
        this.readNBT(packet.getNbtCompound());
    }

    // 动态实现MMCE的ColorableMachineTile接口方法
    // 这些方法会被反射调用
    public void setMachineColor_MMCE(int newColor) {
        setMachineColor(newColor);
    }

    public int getMachineColor_MMCE() {
        return getMachineColor();
    }
}
