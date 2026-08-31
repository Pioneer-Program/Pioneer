package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlockEntity;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Entity.ShipEntity;
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

    public static final Supplier<BlockEntityType<FluidVesselBlockEntity>> FLUID_VESSEL = BLOCK_ENTITIES.register(
        "fluid_vessel",
        () -> BlockEntityType.Builder.of(FluidVesselBlockEntity::new, ModBlocks.VACCUM_TANK.get(), ModBlocks.VACCUM_PIPE.get(), ModBlocks.VACCUM_PUMP.get(), ModBlocks.VALVE.get(), ModBlocks.VENT.get()).build(null)
    );
}
