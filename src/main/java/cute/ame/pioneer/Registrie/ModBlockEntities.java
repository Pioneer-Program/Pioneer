package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Entity.ShipControllerEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Pioneer.MODID);

    public static final Supplier<BlockEntityType<ShipControllerEntity>> SHIP_CONTROLLER_ENTITY = BLOCK_ENTITIES.register(
            "ship_controller_entity",
            () -> BlockEntityType.Builder.of(
                    ShipControllerEntity::new,
                    ModBlocks.SHIP_CONTROLLER.get()
            ).build(null)
    );
}
