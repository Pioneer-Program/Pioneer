package cute.ame.pioneer.Mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    @Accessor("level")
    ServerLevel getLevel();

    @Invoker("getUpdatingChunkIfPresent")
    ChunkHolder pionner$getUpdatingChunkIfPresent(long chunkPos);
}
