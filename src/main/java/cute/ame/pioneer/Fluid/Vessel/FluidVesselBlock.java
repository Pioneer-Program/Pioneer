package cute.ame.pioneer.Fluid.Vessel;

import cute.ame.pioneer.Fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidVesselBlock extends Block implements EntityBlock
{
    public enum Variant
    {
        TANK(FluidConstants.TANK_VOLUME_L, true, 1.0f, 25.0f),
        PIPE(FluidConstants.PIPE_VOLUME_L, false, 0.5f, 10.0f);

        public final float volumeLitres;
        public final boolean merges;

        public final float conductance;
        public final float burstPressureP;

        Variant(float volumeLitres, boolean merges, float conductance, float burstPressureP)
        {
            this.volumeLitres = volumeLitres;
            this.merges = merges;
            this.conductance = conductance;
            this.burstPressureP = burstPressureP;
        }
    }

    private final Variant variant;

    public FluidVesselBlock(Variant variant)
    {
        super(properties(variant));
        this.variant = variant;
    }

    public Variant getVariant()
    {
        return variant;
    }

    public float getVolumeLitres()
    {
        return variant.volumeLitres;
    }

    public boolean merges()
    {
        return variant.merges;
    }

    public float getConductance()
    {
        return variant.conductance;
    }

    public float getNominalBurstPressure()
    {
        return variant.burstPressureP;
    }

    private static BlockBehaviour.Properties properties(Variant variant)
    {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(variant == Variant.TANK ? 3.0f : 2.0f, 6.0f)
            .sound(SoundType.COPPER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new FluidVesselBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            VesselNodes.onRemoved(level, pos, state);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
