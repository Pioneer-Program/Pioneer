package cute.ame.pioneer.Fluid.Life;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Fluid.FluidLevels;
import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.FluidSpecies;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Room.RoomLevelData;
import cute.ame.pioneer.Fluid.SpeciesTable;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.Nullable;

public final class LifeSupport
{
    public static final ResourceKey<DamageType> ASPHYXIATION = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "asphyxiation"));

    public static int nodeOf(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms == null) return FluidNodeStore.INVALID;

        FluidLevels.Located at = FluidLevels.resolve(level, player.getEyePosition());
        return rooms.nodeAt(at.pos());
    }

    public static Breathing.@Nullable Result evaluate(ServerPlayer player)
    {
        int nodeId = nodeOf(player);
        if (nodeId == FluidNodeStore.INVALID) return null;

        FluidLevelData data = FluidLevelData.getIfPresent(player.serverLevel());
        if (data == null) return null;

        FluidNodeStore store = data.store();
        if (!store.alive(nodeId)) return null;

        return Breathing.evaluate(store, nodeId, FluidSpecies.active(), Config.BREATHING_MIN_PRESSURE_P.get().floatValue());
    }

    public static void tick(ServerPlayer player)
    {
        if (player.isSpectator() || player.isCreative()) return;

        boolean breathable;
        int nodeId = nodeOf(player);
        FluidLevelData data = FluidLevelData.getIfPresent(player.serverLevel());
        FluidNodeStore store = data == null ? null : data.store();

        if (store != null && nodeId != FluidNodeStore.INVALID && store.alive(nodeId))
        {
            SpeciesTable table = FluidSpecies.active();
            Breathing.Result result = Breathing.evaluate(store, nodeId, table, Config.BREATHING_MIN_PRESSURE_P.get().floatValue());

            breathable = result.breathable();
            if (breathable) metabolise(store, nodeId, table, data);
        }
        else
        {
            breathable = PioneerAPI.isBreathable(player.level().dimension());
        }

        if (breathable) return;
        if (player.tickCount % Config.ASPHYXIATION_DAMAGE_PERIOD.get() != 0) return;

        player.hurt(player.damageSources().source(ASPHYXIATION), Config.ASPHYXIATION_DAMAGE.get().floatValue());
    }

    private static void metabolise(FluidNodeStore store, int nodeId, SpeciesTable table, FluidLevelData data)
    {
        int o2 = FluidSpecies.O2;
        int co2 = FluidSpecies.CO2;
        if (!table.isValid(o2) || !table.isValid(co2)) return;

        float consumed = Config.BREATHING_MOL_PER_TICK.get().floatValue();
        float available = store.amount(nodeId, o2);
        if (available <= 0.0f) return;

        float actual = Math.min(consumed, available);
        store.add(nodeId, o2, -actual);
        store.add(nodeId, co2, actual);

        data.setDirty();
    }
}
