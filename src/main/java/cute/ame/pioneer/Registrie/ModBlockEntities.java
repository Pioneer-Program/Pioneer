package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Fluid.BlockEntity.SensorBlockEntity;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Entity.ShipEntity;
import cute.ame.pioneer.Thermal.BlockEntity.ThermalActuatorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pioneer.MODID);

    public static final Supplier<BlockEntityType<ShipEntity>> SHIP_CONTROLLER_ENTITY = BLOCK_ENTITIES.register(
            "ship_controller_entity",
            () -> BlockEntityType.Builder.of(
                    ShipEntity::new,
                    ModBlocks.SHIP_CONTROLLER.get()
            ).build(null)
    );

    public static final Supplier<BlockEntityType<PioneerVesselBlockEntity>> FLUID_VESSEL = BLOCK_ENTITIES.register(
        "fluid_vessel",
        () -> BlockEntityType.Builder.of(PioneerVesselBlockEntity::new, ModBlocks.VACCUM_TANK.get(), ModBlocks.VACCUM_PIPE.get(), ModBlocks.VACCUM_PUMP.get(), ModBlocks.VALVE.get(), ModBlocks.VENT.get()).build(null)
    );

    public static final Supplier<BlockEntityType<SensorBlockEntity>> SENSOR = BLOCK_ENTITIES.register(
        "sensor",
        () -> BlockEntityType.Builder.of(SensorBlockEntity::new, ModBlocks.BAROMETER.get(), ModBlocks.THERMOMETER.get(), ModBlocks.MASS_SPECTROMETER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<ThermalActuatorBlockEntity>> THERMAL_ACTUATOR = BLOCK_ENTITIES.register(
        "thermal_actuator",
        () -> BlockEntityType.Builder.of(ThermalActuatorBlockEntity::new, ModBlocks.HEATER.get(), ModBlocks.CHILLER.get()).build(null)
    );
}
