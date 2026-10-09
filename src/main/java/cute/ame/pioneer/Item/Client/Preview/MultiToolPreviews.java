package cute.ame.pioneer.Item.Client.Preview;

import cute.ame.pioneer.Item.Module.MultiToolModule;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModMultiToolModules;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class MultiToolPreviews
{
    private static final Map<MultiToolModule, MultiToolPreview> PREVIEWS = new IdentityHashMap<>();
    private static final PreviewFrame FRAME = new PreviewFrame();

    public static void register(MultiToolModule module, MultiToolPreview preview)
    {
        PREVIEWS.put(module, preview);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> register(ModMultiToolModules.ROTATE.get(), new RotatePreview()));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.options.hideGui) return;

        ItemStack stack = MultiToolItem.held(player);
        if (stack.isEmpty()) return;

        MultiToolPreview preview = PREVIEWS.get(MultiToolItem.moduleOf(stack));
        if (preview == null) return;

        FRAME.minecraft = minecraft;
        FRAME.level = level;
        FRAME.player = player;
        FRAME.stack = stack;
        FRAME.pose = event.getPoseStack();
        FRAME.camera = event.getCamera().getPosition();
        FRAME.now = Util.getMillis();
        FRAME.hit = minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit : null;

        preview.render(FRAME);
        FRAME.flush();
    }
}
