package cute.ame.pioneer.Spaceship.Entity;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModAttachmentTypes;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import cute.ame.pioneer.Spaceship.Block.ThrusterBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ShipEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final UUID NULL_UUID = new UUID(0, 0);

    private UUID subLevelUUID;
    private SubLevelContainer container;

    @Override
    public void onLoad() {
        super.onLoad();
        container = SubLevelContainer.getContainer(level);
    }

    public ShipEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SHIP_CONTROLLER_ENTITY.get(), pos, blockState);
        subLevelUUID = NULL_UUID;
        container = null;
    }

    public HashSet<BlockPos> getAssembledBlocks() {
        return this.getData(ModAttachmentTypes.ASSEMBLED_BLOCKS.get());
    }

    public void setAssembledBlocks(HashSet<BlockPos> blocks) {
        this.setData(ModAttachmentTypes.ASSEMBLED_BLOCKS.get(), blocks);
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
            if (containingSubLevel.isRemoved())
                throw new RuntimeException("Sub-level assembly attempted inside plot of already removed sub-level");
            containingPose = new Pose3d(containingSubLevel.logicalPose());
            containingPose.transformPosition(pose.position());
            pose.orientation().set(containingPose.orientation());
            final RigidBodyHandle containingHandle = physicsSystem.getPhysicsHandle((ServerSubLevel) containingSubLevel);
            containingHandle.getLinearVelocity(containingLinearVelocity);
            containingHandle.getAngularVelocity(containingAngularVelocity);
        } else
            containingPose = null;
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
            if (!subLevel.isRemoved())
                pipeline.addLinearAndAngularVelocity(subLevel, containingAngularVelocity.cross(localPos, localPos).add(containingLinearVelocity), containingAngularVelocity);
            if (containingSubLevel != null)
                subLevel.setSplitFrom((ServerSubLevel) containingSubLevel, originalPose);
        }
        if (!subLevel.isRemoved())
            pipeline.teleport(subLevel, subLevel.logicalPose().position(), subLevel.logicalPose().orientation());
        subLevel.updateLastPose();
        SubLevelAssemblyHelper.moveTrackingPoints(level, bounds, subLevel, transform);
        BoundingBox3ic newBounds = subLevel.getPlot().getBoundingBox();
        HashSet<BlockPos> assembledBlocks = new HashSet<>();
        ShipEntity newEntity = null;
        for (int x = newBounds.minX(); x <= newBounds.maxX(); x++) {
            for (int y = newBounds.minY(); y <= newBounds.maxY(); y++) {
                for (int z = newBounds.minZ(); z <= newBounds.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!level.getBlockState(pos).isAir())
                        assembledBlocks.add(pos);
                    if (blockEntity instanceof ShipEntity) {
                        if (newEntity != null)
                            Pioneer.LOGGER.warn("Multiple ship controller detected");
                        else
                            newEntity = (ShipEntity) blockEntity;
                    }

                }
            }
        }
        if (newEntity == null)
            Pioneer.LOGGER.error("Ship controller not found!");
        else {
            newEntity.setAssembledBlocks(assembledBlocks);
            newEntity.rebuildThrusterCache();
        }
    }

    public boolean isAssemble() {
        return !subLevelUUID.equals(NULL_UUID);
    }

    public void disassemble() {
        ServerSubLevel subLevel = (ServerSubLevel) getSubLevel();
        this.subLevelUUID = NULL_UUID;
        setData(ModAttachmentTypes.THRUSTER_POSITIONS.get(), List.of());
        this.setChanged();
        if (subLevel == null)
            return;
        ServerLevelPlot plot = subLevel.getPlot();
        Pose3dc pose = subLevel.logicalPose();
        HashSet<BlockPos> assembledBlocks = getAssembledBlocks();
        this.setAssembledBlocks(new HashSet<>());
        assembledBlocks.forEach(localPos -> {
            BlockState state = level.getBlockState(localPos);
            BlockEntity blockEntity = level.getBlockEntity(localPos);
            Vec3 worldPos = pose.transformPosition(new Vec3(
                    localPos.getX() + 0.5, localPos.getY() + 0.5, localPos.getZ() + 0.5
            ));
            BlockPos targetPos = BlockPos.containing(worldPos);
            CompoundTag tag = null;
            if (blockEntity != null) {
                tag = blockEntity.saveWithFullMetadata(level.registryAccess());
                tag.putInt("x", targetPos.getX());
                tag.putInt("y", targetPos.getY());
                tag.putInt("z", targetPos.getZ());
            }
            level.setBlock(targetPos, state, Block.UPDATE_CLIENTS);
            BlockEntity newBlockEntity = level.getBlockEntity(targetPos);
            if (newBlockEntity != null && tag != null)
                newBlockEntity.loadWithComponents(tag, level.registryAccess());
        });
        container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
    }

    public SubLevel getSubLevel() {
        if (subLevelUUID.equals(NULL_UUID))
            return null;
        return container.getSubLevel(subLevelUUID);
    }

    public List<BlockPos> getThrusterPositions() {
        return this.getData(ModAttachmentTypes.THRUSTER_POSITIONS.get());
    }

    private void rebuildThrusterCache() {
        if (this.level == null)
            return;

        final List<BlockPos> next = new ArrayList<>();
        for (BlockPos pos : getAssembledBlocks())
            if (this.level.getBlockState(pos).getBlock() instanceof ThrusterBlock)
                next.add(pos.immutable());

        if (!next.equals(getThrusterPositions())) {
            setData(ModAttachmentTypes.THRUSTER_POSITIONS.get(), List.copyOf(next));
            setChanged();
        }
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        rebuildThrusterCache();
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        final List<BlockPos> thrusters = getThrusterPositions();
        if (thrusters.isEmpty() || this.level == null)
            return;

        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());

        int activeCount = 0;
        for (BlockPos pos : thrusters) {
            final BlockState state = this.level.getBlockState(pos);
            if (!(state.getBlock() instanceof ThrusterBlock))
                continue;
            if (!state.getValue(ThrusterBlock.ACTIVE))
                continue;

            final Direction dir = state.getValue(ThrusterBlock.FACING);
            final Vector3d thrust = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ())
                    .mul(ThrusterBlock.THRUST * timeStep);
            final Vector3d point = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            forceGroup.applyAndRecordPointForce(point, thrust);
            activeCount++;
        }
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
