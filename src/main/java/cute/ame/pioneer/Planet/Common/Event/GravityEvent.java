package cute.ame.pioneer.Planet.Common.Event;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public class GravityEvent
{
    private static final ResourceLocation GRAVITY_ID = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "custom_gravity");

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event)
    {
      if (event.getEntity() instanceof LivingEntity living && event.getLevel() instanceof Level level)
        applyGravity(living, level.dimension());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerChangedDimensionEvent event)
    {
      applyGravity(event.getEntity(), event.getTo());
    }

    private static void applyGravity(LivingEntity entity, ResourceKey<Level> dim)
    {
      AttributeInstance attr = entity.getAttribute(Attributes.GRAVITY);
      if (attr == null) return;

      attr.removeModifier(GRAVITY_ID);
      float gravity = PioneerAPI.getGravityFor(dim);
      if (gravity != 1.0f) attr.addPermanentModifier(new AttributeModifier(GRAVITY_ID, gravity - 1.0f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
