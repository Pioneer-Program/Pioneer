package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Planet.Arid.Item.AridCrystalShard;
import cute.ame.pioneer.Item.PortableThrusterItem;
import cute.ame.pioneer.Item.SpaceNavigatorItemDebug;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems
{
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pioneer.MODID);

  public static final DeferredItem<PortableThrusterItem> PORTABLE_THRUSTER = ITEMS.register("portable_thruster", () -> new PortableThrusterItem(new Properties().stacksTo(1)));

  public static final DeferredItem<SpaceNavigatorItemDebug> SPACE_NAVIGATOR = ITEMS.register("space_navigator", () -> new SpaceNavigatorItemDebug(new Properties().stacksTo(1)));

  public static final DeferredItem<AridCrystalShard> ARID_CRYSTAL_SHARD = ITEMS.register("arid_crystal_shard", AridCrystalShard::new);

  public static final DeferredItem<BlockItem> VACCUM_TANK = ITEMS.register("vaccum_tank", () -> new BlockItem(ModBlocks.VACCUM_TANK.get(), new Properties()));

  public static final DeferredItem<BlockItem> VACCUM_PUMP = ITEMS.register("vaccum_pump", () -> new BlockItem(ModBlocks.VACCUM_PUMP.get(), new Properties()));

  public static final DeferredItem<BlockItem> VALVE = ITEMS.register("valve", () -> new BlockItem(ModBlocks.VALVE.get(), new Properties()));

  public static final DeferredItem<BlockItem> VENT = ITEMS.register("vent", () -> new BlockItem(ModBlocks.VENT.get(), new Properties()));

  public static final DeferredItem<BlockItem> VACCUM_PIPE = ITEMS.register("vaccum_pipe", () -> new BlockItem(ModBlocks.VACCUM_PIPE.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_DUST = ITEMS.register("arid_dust", () -> new BlockItem(ModBlocks.ARID_DUST.get(), new Properties()));

  public static final DeferredItem<BlockItem> CERAMIC_TILES = ITEMS.register("ceramic_tiles", () -> new BlockItem(ModBlocks.CERAMIC_TILES.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_ROCK = ITEMS.register("arid_rock", () -> new BlockItem(ModBlocks.ARID_ROCK.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_ROCK_CRACKED = ITEMS.register("arid_rock_cracked", () -> new BlockItem(ModBlocks.ARID_ROCK_CRACKED.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_ROCK_POLISHED = ITEMS.register("arid_rock_polished", () -> new BlockItem(ModBlocks.ARID_ROCK_POLISHED.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_STRATA_RED = ITEMS.register("arid_strata_red", () -> new BlockItem(ModBlocks.ARID_STRATA_RED.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_STRATA_ORANGE = ITEMS.register("arid_strata_orange", () -> new BlockItem(ModBlocks.ARID_STRATA_ORANGE.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_STRATA_WHITE = ITEMS.register("arid_strata_white", () -> new BlockItem(ModBlocks.ARID_STRATA_WHITE.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_STRATA_MAROON = ITEMS.register("arid_strata_maroon", () -> new BlockItem(ModBlocks.ARID_STRATA_MAROON.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_CRYSTAL_SMALL = ITEMS.register("arid_crystal_small", () -> new BlockItem(ModBlocks.ARID_CRYSTAL_SMALL.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_CRYSTAL_MEDIUM = ITEMS.register("arid_crystal_medium", () -> new BlockItem(ModBlocks.ARID_CRYSTAL_MEDIUM.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_CRYSTAL_LARGE = ITEMS.register("arid_crystal_large", () -> new BlockItem(ModBlocks.ARID_CRYSTAL_LARGE.get(), new Properties()));

  public static final DeferredItem<BlockItem> ARID_GLYPH = ITEMS.register("arid_glyph", () -> new BlockItem(ModBlocks.ARID_GLYPH.get(), new Properties()));

  public static final DeferredItem<BlockItem> BAROMETER = ITEMS.register("barometer", () -> new BlockItem(ModBlocks.BAROMETER.get(), new Properties()));

  public static final DeferredItem<BlockItem> THERMOMETER = ITEMS.register("thermometer", () -> new BlockItem(ModBlocks.THERMOMETER.get(), new Properties()));

  public static final DeferredItem<BlockItem> MASS_SPECTROMETER = ITEMS.register("mass_spectrometer", () -> new BlockItem(ModBlocks.MASS_SPECTROMETER.get(), new Properties()));

  public static final DeferredItem<BlockItem> SHIP_CONTROLLER = ITEMS.register("ship_controller",  () -> new BlockItem(ModBlocks.SHIP_CONTROLLER.get(), new Properties()));

  public static final DeferredItem<BlockItem> THRUSTER = ITEMS.register("thruster", () -> new BlockItem(ModBlocks.THRUSTER.get(), new Properties()));
}
