package cute.ame.pioneer.Fluid.Vessel;

import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FluidVesselBlockEntity extends BlockEntity
{
    private static final String K_NODE = "node";

    private long node = -1L;

    public FluidVesselBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FLUID_VESSEL.get(), pos, state);
    }

    public long getNodeHandle() { return node; }

    public void setNodeHandle(long handle)
    {
        this.node = handle;
        setChanged();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();

        if (level instanceof ServerLevel serverLevel) VesselNodes.onLoaded(serverLevel, worldPosition, this);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        node = tag.getLong(K_NODE);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putLong(K_NODE, node);
    }
}
