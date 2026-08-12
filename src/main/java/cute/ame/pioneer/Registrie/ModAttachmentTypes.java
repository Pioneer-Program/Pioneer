package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModAttachmentTypes {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Pioneer.MODID);

    public static final Supplier<AttachmentType<ObjectOpenHashSet<BlockPos>>> ATTACHED_BLOCk = ATTACHMENT_TYPES.register(
            "attached_block", () -> AttachmentType.builder(() -> new ObjectOpenHashSet<BlockPos>())
                    .serialize(BlockPos.CODEC.listOf().xmap(
                            ObjectOpenHashSet::new,
                            ArrayList::new
                    )).build()
    );


}
