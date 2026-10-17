package cute.ame.pioneer.Item.Name;

import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Item.Network.NameLabelsPayload;
import cute.ame.pioneer.Item.ToolBoxItem;
import cute.ame.pioneer.Registrie.ModToolBoxModules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public static final int MAX_LENGTH = 16;
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
        return entity instanceof ComputerPeripheral && !entity.getBlockState().is(ToolBoxColors.UNNAMEABLE);
    }

    public static String clean(@Nullable String raw)
    {
        if (raw == null) return "n";

        StringBuilder label = new StringBuilder(MAX_LENGTH);
        boolean boundary = false;
        char previous = ' ';
        for (int i = 0, n = raw.length(); i < n; i++)
        {
            char c = raw.charAt(i);
            boolean letter = letter(c);
            if (!letter && !digit(c)) boundary = !label.isEmpty();
            else if (!label.isEmpty() || letter)
            {
                if (c >= 'A' && c <= 'Z' && (previous >= 'a' && previous <= 'z' || digit(previous)))
                    boundary = !label.isEmpty();
                if (label.length() + (boundary ? 2 : 1) > MAX_LENGTH) break;
                if (boundary) label.append('_');
                label.append(Character.toLowerCase(c));
                boundary = false;
            }
            previous = c;
        }
        return label.toString();
    }

    public static boolean letter(char c)
    {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static boolean digit(char c)
    {
        return c >= '0' && c <= '9';
    }

    private static boolean holdsNameTool(ServerPlayer player)
    {
        ItemStack stack = ToolBoxItem.held(player);
        return !stack.isEmpty() && ToolBoxItem.moduleOf(stack) == ModToolBoxModules.NAME.get();
    }
}
