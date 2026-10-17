package cute.ame.pioneer.Item.Name;

import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.Network.NameLabelsPayload;
import cute.ame.pioneer.Registrie.ModMultiToolModules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NameServer
{
    public static final int MAX_LENGTH = 32;
    public static final int RADIUS = 32;

    public static void request(ServerPlayer player)
    {
        if (!holdsNameTool(player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        double reach = (double) RADIUS * RADIUS;
        List<NameLabelsPayload.Entry> entries = new ArrayList<>();

        int minX = (center.getX() - RADIUS) >> 4, maxX = (center.getX() + RADIUS) >> 4;
        int minZ = (center.getZ() - RADIUS) >> 4, maxZ = (center.getZ() + RADIUS) >> 4;

        for (int cx = minX; cx <= maxX; cx++)
        {
            for (int cz = minZ; cz <= maxZ; cz++)
            {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity entity : chunk.getBlockEntities().values())
                {
                    if (!nameable(entity) || ((ComputerPeripheral) entity).getLabel().isEmpty()) continue;
                    if (entity.getBlockPos().distSqr(center) > reach) continue;

                    entries.add(new NameLabelsPayload.Entry(entity.getBlockPos().asLong(), ((ComputerPeripheral) entity).getLabel()));
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new NameLabelsPayload(true, entries));
    }

    public static void set(ServerPlayer player, long packed, String raw)
    {
        if (!holdsNameTool(player)) return;

        BlockPos pos = BlockPos.of(packed);
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!nameable(entity)) return;

        ComputerPeripheral peripheral = (ComputerPeripheral) entity;

        String label = clean(raw);
        if (label.equals(peripheral.getLabel())) return;

        peripheral.setLabel(label);
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new NameLabelsPayload(false, List.of(new NameLabelsPayload.Entry(packed, label))));
    }

    public static boolean nameable(@Nullable BlockEntity entity)
    {
        return entity instanceof ComputerPeripheral && !entity.getBlockState().is(MultiToolColors.UNNAMEABLE);
    }

    public static String clean(String raw)
    {
        String label = StringUtil.filterText(raw == null ? "" : raw).strip();
        return label.length() > MAX_LENGTH ? label.substring(0, MAX_LENGTH).strip() : label;
    }

    private static boolean holdsNameTool(ServerPlayer player)
    {
        ItemStack stack = MultiToolItem.held(player);
        return !stack.isEmpty() && MultiToolItem.moduleOf(stack) == ModMultiToolModules.NAME.get();
    }
}
