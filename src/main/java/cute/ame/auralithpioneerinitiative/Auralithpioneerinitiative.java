package cute.ame.auralithpioneerinitiative;

import com.mojang.logging.LogUtils;
import cute.ame.auralithpioneerinitiative.Command.PioneerDebugCommand;
import cute.ame.auralithpioneerinitiative.Registrie.*;
import cute.ame.auralithpioneerinitiative.Seamless.Network.SeamlessNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Auralithpioneerinitiative.MODID)
public class Auralithpioneerinitiative
{
  public static final String MODID = "auralithpioneerinitiative";
  public static final Logger LOGGER = LogUtils.getLogger();

  public Auralithpioneerinitiative(IEventBus modEventBus, ModContainer modContainer)
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

    NeoForge.EVENT_BUS.addListener(Auralithpioneerinitiative::onRegisterCommands);
    modEventBus.addListener(SeamlessNetworking::register);
  }

  private static void onRegisterCommands(RegisterCommandsEvent event)
  {
    PioneerDebugCommand.register(event.getDispatcher());
  }
}