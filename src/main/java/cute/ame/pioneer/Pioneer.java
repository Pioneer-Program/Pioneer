package cute.ame.pioneer;

import com.mojang.logging.LogUtils;
import cute.ame.pioneer.Command.PioneerDebugCommand;
import cute.ame.pioneer.Registrie.*;
import cute.ame.pioneer.Seamless.Network.SeamlessNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Pioneer.MODID)
public class Pioneer
{
  public static final String MODID = "pioneer";
  public static final Logger LOGGER = LogUtils.getLogger();

  public Pioneer(IEventBus modEventBus, ModContainer modContainer)
  {
    ModWorldgen.FEATURES.register(modEventBus);
    ModWorldgen.BIOME_SOURCES.register(modEventBus);
    ModWorldgen.CHUNK_GENERATORS.register(modEventBus);
    ModParticles.PARTICLE_TYPES.register(modEventBus);
    ModDataComponents.DATA_COMPONENTS.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);
    ModBlocks.BLOCKS.register(modEventBus);
    ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

    NeoForge.EVENT_BUS.addListener(Pioneer::onRegisterCommands);
    modEventBus.addListener(SeamlessNetworking::register);
  }

  private static void onRegisterCommands(RegisterCommandsEvent event)
  {
    PioneerDebugCommand.register(event.getDispatcher());
  }
}