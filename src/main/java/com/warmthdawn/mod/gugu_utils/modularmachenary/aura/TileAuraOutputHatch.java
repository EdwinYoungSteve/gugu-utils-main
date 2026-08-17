package com.warmthdawn.mod.gugu_utils.modularmachenary.aura;

import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import com.warmthdawn.mod.gugu_utils.modularmachenary.components.GenericMachineCompoment;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementAura;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.basic.IGeneratable;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TileAuraOutputHatch extends TileAuraHatch
        implements ITickable, MachineComponentTile, IGeneratable<RequirementAura.RT> {
    private static final int MAX_RELEASE_PER_TICK = 8000;

    private int lastAura = -1;

    @Override
    public void update() {
        if (this.world == null) {
            return;
        }

        if (this.world.isRemote) {
            spawnIdleParticles();
            return;
        }

        releaseAuraToWorld();
        if (this.world.getTotalWorldTime() % 10 == 0 && this.lastAura != this.container.getStoredAura()) {
            this.lastAura = this.container.getStoredAura();
            syncBlock();
        }
    }

    @Override
    public boolean generate(RequirementAura.RT outputToken, boolean doOperation) {
        int generated = this.container.storeAura(outputToken.getAura(), !doOperation);
        outputToken.setAura(outputToken.getAura() - generated);
        return generated > 0;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public MachineComponent provideComponent() {
        return new GenericMachineCompoment<>(this, (ComponentType) MMCompoments.COMPONENT_AURA);
    }

    private void releaseAuraToWorld() {
        int storedAura = this.container.getStoredAura();
        if (storedAura <= 0 || !this.container.isAcceptableType(IAuraType.forWorld(this.world))) {
            return;
        }

        int toRelease = Math.min(storedAura, MAX_RELEASE_PER_TICK);
        BlockPos spot = IAuraChunk.getHighestSpot(this.world, this.pos, 20, this.pos);
        IAuraChunk chunk = IAuraChunk.getAuraChunk(this.world, spot);
        chunk.storeAura(spot, toRelease);
        this.container.drainAura(toRelease, false);
    }

    private void spawnIdleParticles() {
        if (this.container.getStoredAura() <= 0) {
            return;
        }

        BlockPos above = this.pos.offset(EnumFacing.UP);
        Random random = this.world.rand;
        if (!this.world.getBlockState(above).isOpaqueCube() && random.nextFloat() >= 0.7F) {
            NaturesAuraAPI.instance().spawnMagicParticle(
                    this.pos.getX() + random.nextFloat(),
                    this.pos.getY() + 1F,
                    this.pos.getZ() + random.nextFloat(),
                    0F, 0F, 0F,
                    this.container.getAuraColor(),
                    random.nextFloat() * 3F + 1F,
                    random.nextInt(100) + 50,
                    -0.05F,
                    true,
                    true);
        }
    }

    private void syncBlock() {
        this.markDirty();
        IBlockState state = this.world.getBlockState(this.pos);
        this.world.notifyBlockUpdate(this.pos, state, state, 3);
    }
}
