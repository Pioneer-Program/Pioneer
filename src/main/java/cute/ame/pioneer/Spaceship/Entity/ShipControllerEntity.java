package cute.ame.pioneer.Spaceship.Entity;

import cute.ame.pioneer.Registrie.ModAttachmentTypes;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShipControllerEntity extends BlockEntity {

    private static final UUID NULL_UUID = new UUID(0, 0);

    private UUID subLevelUUID;
    private SubLevelContainer container;

    @Override
    public void onLoad() {
        super.onLoad();
        container = SubLevelContainer.getContainer(level);
    }

    public ShipControllerEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SHIP_CONTROLLER_ENTITY.get(), pos, blockState);
        subLevelUUID = NULL_UUID;
        container = null;
    }

    public Set<BlockPos> getAssemblyBlocks() {
        return this.getData(ModAttachmentTypes.ATTACHED_BLOCk.get());
    }

    public void setAssemblyBlocks(Set<BlockPos> assembledBlocks) {
        this.setData(ModAttachmentTypes.ATTACHED_BLOCk.get(), (ObjectOpenHashSet) assembledBlocks);
    }

    public void assemble(BlockPos anchor, Iterable<BlockPos> blocks, BoundingBox3ic bounds) {
        final ServerLevel level = (ServerLevel) this.level;
        final ServerSubLevelContainer container = (ServerSubLevelContainer) this.container;
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final SubLevel containingSubLevel = Sable.HELPER.getContaining(level, anchor);
        final Pose3d pose = new Pose3d();
        pose.position().set(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5);
        final Vector3d containingAngularVelocity = new Vector3d();
        final Vector3d containingLinearVelocity = new Vector3d();
        final Pose3d containingPose;
        if (containingSubLevel != null) {
            if (containingSubLevel.isRemoved()) {
                throw new RuntimeException("Sub-level assembly attempted inside plot of already removed sub-level");
            }
            containingPose = new Pose3d(containingSubLevel.logicalPose());
            containingPose.transformPosition(pose.position());
            pose.orientation().set(containingPose.orientation());
            final RigidBodyHandle containingHandle = physicsSystem.getPhysicsHandle((ServerSubLevel) containingSubLevel);
            containingHandle.getLinearVelocity(containingLinearVelocity);
            containingHandle.getAngularVelocity(containingAngularVelocity);
        } else {
            containingPose = null;
        }
        final ServerSubLevel subLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
        this.subLevelUUID = subLevel.getUniqueId();
        this.setChanged();
        final LevelPlot plot = subLevel.getPlot();
        plot.newEmptyChunk(plot.getCenterChunk());
        final BlockPos plotAnchor = plot.getCenterBlock();
        final SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(anchor, plotAnchor, 0, Rotation.NONE, level);
        SubLevelAssemblyHelper.moveOtherStuff(level, transform, blocks, bounds);
        SubLevelAssemblyHelper.moveBlocks(level, transform, blocks);
        final Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
        Vec3 subLevelCenter = Vec3.atLowerCornerOf(anchor);
        if (centerOfMass != null) {
            subLevelCenter = subLevelCenter
                    .subtract(Vec3.atLowerCornerOf(plotAnchor))
                    .add(centerOfMass.x(), centerOfMass.y(), centerOfMass.z());
        } else {
            subLevel.logicalPose().rotationPoint()
                    .set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
        }
        subLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
        final PhysicsPipeline pipeline = physicsSystem.getPipeline();
        if (containingSubLevel != null) {
            final Pose3d originalPose = new Pose3d(subLevel.logicalPose());
            containingPose.transformPosition(subLevel.logicalPose().position());
            final Vector3d localPos = subLevel.logicalPose().position().sub(containingPose.position(), new Vector3d());
            if (!subLevel.isRemoved()) {
                pipeline.addLinearAndAngularVelocity(subLevel, containingAngularVelocity.cross(localPos, localPos).add(containingLinearVelocity), containingAngularVelocity);
            }
            if (containingSubLevel != null) {
                subLevel.setSplitFrom((ServerSubLevel) containingSubLevel, originalPose);
            }
        }

        if (!subLevel.isRemoved()) {
            pipeline.teleport(subLevel, subLevel.logicalPose().position(), subLevel.logicalPose().orientation());
        }
        subLevel.updateLastPose();
        SubLevelAssemblyHelper.moveTrackingPoints(level, bounds, subLevel, transform);
    }

    public boolean isAssemble() {
        return subLevelUUID != NULL_UUID;
    }

    public void disassemble() {
        ServerSubLevel subLevel = (ServerSubLevel) getSubLevel();
        this.subLevelUUID = NULL_UUID;
        if (subLevel == null)
            return;
        container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
    }

    public SubLevel getSubLevel() {
        if (subLevelUUID.equals(NULL_UUID))
            return null;
        return this.container.getAllSubLevels().stream().filter((subLevel) -> subLevel.getUniqueId().equals(this.subLevelUUID)).findFirst().orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("subLevelUUID", this.subLevelUUID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.subLevelUUID = tag.getUUID("subLevelUUID");
    }
}
