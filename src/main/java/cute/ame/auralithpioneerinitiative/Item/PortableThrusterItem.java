package cute.ame.auralithpioneerinitiative.Item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PortableThrusterItem extends Item
{
  public static final double ACCELERATION = 0.75;
  public static final double MAX_SPEED = 8;

  public PortableThrusterItem(Properties properties)
  {
    super(properties);
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity)
  {
    return 72_000;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack)
  {
    return UseAnim.SPEAR;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
  {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
  }
}
