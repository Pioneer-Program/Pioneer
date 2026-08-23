package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class ModAttachmentTypes {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Pioneer.MODID);

    public static final Supplier<AttachmentType<HashSet<BlockPos>>>  ASSEMBLED_BLOCKS = ATTACHMENT_TYPES.register("assembled_block", () ->
            AttachmentType.builder(() -> new HashSet<BlockPos>())
                    .serialize(BlockPos.CODEC.listOf().xmap(
                            HashSet::new,
                            ArrayList::new
                    )).build()
    );

    public static final Supplier<AttachmentType<List<BlockPos>>> THRUSTER_POSITIONS = ATTACHMENT_TYPES.register("thruster_positions", () ->
            AttachmentType.<List<BlockPos>>builder(() -> List.of())
                    .serialize(BlockPos.CODEC.listOf())
                    .build()
    );
}
    public static final Supplier<AttachmentType<BlockPos>> SHIP_CONTROLLER_POS = ATTACHMENT_TYPES.register("ship_controller", () ->
            AttachmentType.builder(() -> BlockPos.ZERO)
                    .serialize(BlockPos.CODEC)
                    .build()
    );

}
