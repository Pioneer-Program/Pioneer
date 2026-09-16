package cute.ame.pioneer.Thermal.Event;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Fluid.Helper.FluidLevels;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Data.ThermalStore;
import cute.ame.pioneer.Thermal.Helper.ThermalBreakdown;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Physics.ThermalConduction;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ThermalTickEvents
{
    private static final Direction[] FACES = Direction.values();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        int period = Math.max(Config.THERMAL_PERIOD.get(), 1);
        if (level.getGameTime() % period != 0) return;

        ThermalLevelData data = ThermalLevelData.getIfPresent(level);
        if (data == null || data.count() == 0) return;

        pass(level, data, period);
    }

    public static int pass(ServerLevel level, ThermalLevelData data, int period)
    {
        ThermalStore store = data.store();
        int count = store.count();
        if (count == 0) return 0;

        MaterialTable table = ThermalMaterials.table();
        float[] conductivity = table.conductivityRaw();
        float[] heat = table.volumetricHeatRaw();
        float[] breakdown = table.breakdownRaw();
        int materials = conductivity.length;
        float ambient = BlockTemperature.dimensionDefault(level);
        float dt = (float) (period / 20.0 * Config.THERMAL_TIME_SCALE.get());
        float maxStep = Config.THERMAL_MAX_STEP.get().floatValue();
        float epsilon = Config.THERMAL_EPSILON_K.get().floatValue();
        float attachDelta = Config.THERMAL_ATTACH_DELTA_K.get().floatValue();
        int limit = Config.THERMAL_MAX_ENTRIES.get();
        int budget = Math.min(Config.THERMAL_UPDATES_PER_TICK.get(), count);
        int start = store.cursor();

        BlockPos.MutableBlockPos centre = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        boolean breaks = Config.THERMAL_BREAKDOWN.get();
        int breakBudget = Config.THERMAL_BREAKDOWN_PER_PASS.get();
        LongArrayList failing = null;
        int visited = 0;

        for (int n = 0; n < budget; n++)
        {
            int slot = start + n;
            if (slot >= count) slot -= count;

            long packed = store.position(slot);
            centre.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (!FluidLevels.isLoaded(level, centre)) continue;

            int ma = store.material(slot);
            if (ma == ThermalStore.UNRESOLVED)
            {
                BlockState state = level.getBlockState(centre);
                if (state.isAir())
                {
                    store.markDetached(packed);
                    continue;
                }

                ma = ThermalMaterials.indexOf(state);
                store.setMaterial(slot, ma);
            }

            if (ma >= materials) ma = MaterialTable.DEFAULT;

            float ka = conductivity[ma];
            float ca = heat[ma];
            float ta = store.kelvin(slot);
            visited++;

            boolean canRecruit = Math.abs(ta - ambient) > attachDelta;
            if (breaks && ThermalBreakdown.exceeded(ta, breakdown[ma]))
            {
                if (failing == null) failing = new LongArrayList(16);
                if (failing.size() < breakBudget) failing.add(packed);
            }

            for (Direction face : FACES)
            {
                probe.setWithOffset(centre, face);
                long neighbourPos = probe.asLong();
                int nslot = store.slot(neighbourPos);
                int mb;
                boolean recruit = false;
                if (nslot != ThermalStore.INVALID)
                {
                    mb = store.material(nslot);
                    if (mb == ThermalStore.UNRESOLVED)
                    {
                        if (!FluidLevels.isLoaded(level, probe)) continue;

                        mb = ThermalMaterials.indexOf(level.getBlockState(probe));
                        store.setMaterial(nslot, mb);
                    }
                } else
                {
                    if (!FluidLevels.isLoaded(level, probe)) continue;

                    BlockState neighbour = level.getBlockState(probe);
                    mb = ThermalMaterials.indexOf(neighbour);
                    recruit = canRecruit && !neighbour.isAir();
                }

                if (mb >= materials) mb = MaterialTable.DEFAULT;

                float coupled = MaterialTable.couple(ka, conductivity[mb]);
                if (coupled <= 0.0f) continue;

                float tb = nslot != ThermalStore.INVALID ? store.kelvin(nslot) : ambient;
                float change = ThermalConduction.delta(coupled, ca, heat[mb], ta, tb, dt, maxStep);
                if (change > -epsilon && change < epsilon) continue;

                store.addDelta(slot, change);
                if (!recruit) continue;

                int created = store.attach(neighbourPos, ambient, limit);
                if (created == ThermalStore.INVALID) continue;

                store.setMaterial(created, mb);
                store.keepAlive(created);
            }
        }

        int applied = store.applyDeltas();
        store.advance(budget);

        int removed = 0;
        int grace = Math.max(Config.THERMAL_SETTLE_GRACE.get(), 1);
        if (store.pass() % grace == 0) removed = data.settle(level);

        int broken = failing == null ? 0 : ThermalBreakdown.apply(level, data, failing);
        if (applied > 0 || removed > 0 || broken > 0) data.setDirty();

        return visited;
    }
}
