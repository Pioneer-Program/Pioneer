package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(StructureManager.class)
public class StructureManagerMixin {

    @Shadow
    @Final
    private LevelAccessor level;

    @Inject(method = "startsForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Ljava/util/List;", at = @At(value = "HEAD"), cancellable = true)
    private void cute$startsForStructure(SectionPos sectionPos, Structure structure, CallbackInfoReturnable<List<StructureStart>> cir) {
        if (this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES) == null) {
            cir.setReturnValue(List.of());
            cir.cancel();
        }
    }

    @Inject(method = "startsForStructure(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At(value = "HEAD"), cancellable = true)
    private void cute$startsForStructure(ChunkPos chunkPos, Predicate<Structure> structurePredicate, CallbackInfoReturnable<List<StructureStart>> cir) {
        if (this.level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_REFERENCES) == null) {
            cir.setReturnValue(List.of());
            cir.cancel();
        }
    }

}
