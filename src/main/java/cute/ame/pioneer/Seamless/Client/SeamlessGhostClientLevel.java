package cute.ame.pioneer.Seamless.Client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.function.Supplier;

public final class SeamlessGhostClientLevel extends ClientLevel
{
    private static final int FULL_BRIGHT = 15;

    public SeamlessGhostClientLevel(ClientPacketListener connection, ClientLevelData clientLevelData, ResourceKey<Level> dimension, Holder<DimensionType> dimensionTypeHolder, int viewDistance, int simulationDistance, Supplier<ProfilerFiller> profiler, LevelRenderer levelRenderer, boolean isDebug, long biomeZoomSeed)
    {
        super(connection, clientLevelData, dimension, dimensionTypeHolder, viewDistance, simulationDistance, profiler, levelRenderer, isDebug, biomeZoomSeed);
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos)
    {
        return FULL_BRIGHT;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount)
    {
        return 7;
    }

    @Override
    public boolean canSeeSky(BlockPos pos)
    {
        return true;
    }
}
