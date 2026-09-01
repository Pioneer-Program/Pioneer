package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Fluid.Block.SensorBlock;
import cute.ame.pioneer.Fluid.Block.BarometerBlock;
import cute.ame.pioneer.Fluid.Block.MassSpectrometerBlock;
import cute.ame.pioneer.Fluid.Block.PipeBlock;
import cute.ame.pioneer.Fluid.Block.TankBlock;
import cute.ame.pioneer.Fluid.Block.ThermometerBlock;
import cute.ame.pioneer.Fluid.Block.PumpBlock;
import cute.ame.pioneer.Fluid.Block.ValveBlock;
import cute.ame.pioneer.Fluid.Block.VentBlock;
import cute.ame.pioneer.Planet.Arid.Block.*;
import cute.ame.pioneer.Spaceship.Block.ShipController;
import cute.ame.pioneer.Spaceship.Block.ThrusterBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pioneer.MODID);

    public static final DeferredBlock<TankBlock> VACCUM_TANK = BLOCKS.register("vaccum_tank", TankBlock::new);

    public static final DeferredBlock<PumpBlock> VACCUM_PUMP = BLOCKS.register("vaccum_pump", PumpBlock::new);

    public static final DeferredBlock<PipeBlock> VACCUM_PIPE = BLOCKS.register("vaccum_pipe", PipeBlock::new);

    public static final DeferredBlock<ValveBlock> VALVE = BLOCKS.register("valve", ValveBlock::new);

    public static final DeferredBlock<VentBlock> VENT = BLOCKS.register("vent", VentBlock::new);

    public static final DeferredBlock<AridDust> ARID_DUST = BLOCKS.register(
        "arid_dust",
        AridDust::new
    );

    public static final DeferredBlock<AridRock> ARID_ROCK = BLOCKS.register(
        "arid_rock",
        () -> new AridRock(AridRock.Variant.RAW)
    );

    public static final DeferredBlock<AridRock> ARID_ROCK_CRACKED = BLOCKS.register(
        "arid_rock_cracked",
        () -> new AridRock(AridRock.Variant.CRACKED)
    );

    public static final DeferredBlock<AridRock> ARID_ROCK_POLISHED = BLOCKS.register(
        "arid_rock_polished",
        () -> new AridRock(AridRock.Variant.POLISHED)
    );

    public static final DeferredBlock<AridStrata> ARID_STRATA_RED = BLOCKS.register(
        "arid_strata_red",
        () -> new AridStrata(AridStrata.Variant.RED)
    );

    public static final DeferredBlock<AridStrata> ARID_STRATA_ORANGE = BLOCKS.register(
        "arid_strata_orange",
        () -> new AridStrata(AridStrata.Variant.ORANGE)
    );

    public static final DeferredBlock<AridStrata> ARID_STRATA_WHITE = BLOCKS.register(
        "arid_strata_white",
        () -> new AridStrata(AridStrata.Variant.WHITE)
    );

    public static final DeferredBlock<AridStrata> ARID_STRATA_MAROON = BLOCKS.register(
        "arid_strata_maroon",
        () -> new AridStrata(AridStrata.Variant.MAROON)
    );

    public static final DeferredBlock<AridCrystal> ARID_CRYSTAL_SMALL = BLOCKS.register(
        "arid_crystal_small",
        () -> new AridCrystal(AridCrystal.Size.SMALL)
    );

    public static final DeferredBlock<AridCrystal> ARID_CRYSTAL_MEDIUM = BLOCKS.register(
        "arid_crystal_medium",
        () -> new AridCrystal(AridCrystal.Size.MEDIUM)
    );

    public static final DeferredBlock<AridCrystal> ARID_CRYSTAL_LARGE = BLOCKS.register(
        "arid_crystal_large",
        () -> new AridCrystal(AridCrystal.Size.LARGE)
    );

    public static final DeferredBlock<AridGlyph> ARID_GLYPH = BLOCKS.register(
        "arid_glyph",
        AridGlyph::new
    );

    public static final DeferredBlock<BarometerBlock> BAROMETER = BLOCKS.register(
        "barometer",
        () -> new BarometerBlock()
    );

    public static final DeferredBlock<ThermometerBlock> THERMOMETER = BLOCKS.register(
        "thermometer",
        () -> new ThermometerBlock()
    );

    public static final DeferredBlock<MassSpectrometerBlock> MASS_SPECTROMETER = BLOCKS.register(
        "mass_spectrometer",
        () -> new MassSpectrometerBlock()
    );

    public static final DeferredBlock<ShipController> SHIP_CONTROLLER = BLOCKS.register(
            "ship_controller",
            ShipController::new
    );

    public static final DeferredBlock<ThrusterBlock> THRUSTER = BLOCKS.register(
            "thruster",
            ThrusterBlock::new
    );
}
