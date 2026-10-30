package cute.ame.pioneer.SkyPlanet.Dimension;

import cute.ame.pioneer.Pioneer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class SpaceChunkSaves
{
    private static final Map<ResourceKey<Level>, LongOpenHashSet> HELD = new HashMap<>();
    private static long unloading = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !isSpace(level)) return;

        ChunkAccess chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        if (untouched(chunk) || inPlotGrid(level, pos.x, pos.z)) return;

        hold(level, pos.toLong());
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !isSpace(level)) return;

        ChunkPos pos = event.getChunk().getPos();
        if (pos.toLong() == unloading || untouched(event.getChunk()) || inPlotGrid(level, pos.x, pos.z)) return;

        hold(level, pos.toLong());
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !isSpace(level)) return;

        ChunkAccess chunk = event.getChunk();
        unloading = chunk.getPos().toLong();
        LongOpenHashSet held = HELD.get(level.dimension());
        if (held != null && !untouched(chunk)) held.remove(unloading);
    }

    public static void beforeSave(ServerLevel level, ChunkAccess chunk)
    {
        if (!chunk.isUnsaved() || !isSpace(level) || !untouched(chunk)) return;

        ChunkPos pos = chunk.getPos();
        if (inPlotGrid(level, pos.x, pos.z)) return;

        LongOpenHashSet held = HELD.get(level.dimension());
        if (held != null && held.remove(pos.toLong())) return;

        chunk.setUnsaved(false);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if (event.getLevel() instanceof ServerLevel level) HELD.remove(level.dimension());
    }

    private static void hold(ServerLevel level, long key)
    {
        HELD.computeIfAbsent(level.dimension(), k -> new LongOpenHashSet()).add(key);
    }

    public static boolean isSpace(ServerLevel level)
    {
        return level.getChunkSource().getGenerator() instanceof SpaceDimensionChunkGenerator;
    }

    public static boolean untouched(ChunkAccess chunk)
    {
        if (!chunk.getBlockEntitiesPos().isEmpty()) return false;

        for (LevelChunkSection section : chunk.getSections()) if (!section.hasOnlyAir()) return false;

        return true;
    }

    public static boolean inPlotGrid(ServerLevel level, int chunkX, int chunkZ)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return false;

        int shift = container.getLogPlotSize(), side = 1 << container.getLogSideLength();
        Vector2i origin = container.getOrigin();
        int plotX = chunkX >> shift, plotZ = chunkZ >> shift;
        return plotX >= origin.x && plotX < origin.x + side && plotZ >= origin.y && plotZ < origin.y + side;
    }
}
