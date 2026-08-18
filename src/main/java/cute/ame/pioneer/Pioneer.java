package cute.ame.pioneer;

import com.mojang.logging.LogUtils;
import cute.ame.pioneer.Command.PioneerDebugCommand;
import cute.ame.pioneer.Registrie.*;
import cute.ame.pioneer.Seamless.Network.SeamlessNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod(Pioneer.MODID)
public class Pioneer
{
  public static final String MODID = "pioneer";
  public static final Logger LOGGER = LogUtils.getLogger();

  public static Map<Long, ServerPlayer> PLAYER_MAP = new HashMap<>();
  public static Map<ServerPlayer, Double> SPEED_MAP =  new HashMap<>();

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

  public static Set<ServerPlayer> getPlayerLoading(ChunkPos pos) {
    Set<ServerPlayer> players = new HashSet<>();
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server == null)
      return players;
    int viewDistanceSquared = (int) Math.pow(server.getPlayerList().getViewDistance(), 2);
    for (Map.Entry<Long, ServerPlayer> entry : PLAYER_MAP.entrySet()) {
      ChunkPos center = new ChunkPos(entry.getKey());
      if (center.distanceSquared(pos) <= viewDistanceSquared && SPEED_MAP.getOrDefault(entry.getValue(), 0.0D) >= 300.D)
        players.add(entry.getValue());
    }
    return players;
  }

  public static boolean isPlayerLoadingTooFast(ChunkPos pos) {
    return !getPlayerLoading(pos).isEmpty();
  }

  private static void onRegisterCommands(RegisterCommandsEvent event)
  {
    PioneerDebugCommand.register(event.getDispatcher());
  }
}