package cute.ame.pioneer.Item.ToolBox;

import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ToolBoxBlockEntity extends BlockEntity
{
    private ItemStack stored = ItemStack.EMPTY;

    public ToolBoxBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.TOOLBOX.get(), pos, state);
    }

    public void store(ItemStack stack)
    {
        stored = stack;
        setChanged();
    }

    public ItemStack take()
    {
        ItemStack stack = stored;
        stored = ItemStack.EMPTY;
        setChanged();
        return stack;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        if (!stored.isEmpty()) tag.put("tool", stored.save(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        stored = ItemStack.parseOptional(registries, tag.getCompound("tool"));
    }
}
