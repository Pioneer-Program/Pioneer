package cute.ame.auralithpioneerinitiative.Planet.Arid.Event;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Block.AridDust;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Block.AridRock;
import cute.ame.auralithpioneerinitiative.Registrie.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID, value = Dist.CLIENT)
public final class AridGroundScatterEvent
{
  private static final double WALK_STEP = 0.48;
  private static final double SPRINT_STEP = 0.34;
  private static final double MIN_SPEED = 0.038;

  private static final int WALK_COUNT = 7;
  private static final int SPRINT_COUNT = 11;
  private static final double FAN_HALF_DEG = 70.0;

  private static final double WALK_SPD = 0.095;
  private static final double SPRINT_SPD = 0.140;

  private static final double WALK_UP = 0.048;
  private static final double SPRINT_UP  = 0.068;

  private static double distAccum = 0.0;
  private static final RandomSource RNG = RandomSource.create();

  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event)
  {
    Minecraft mc = Minecraft.getInstance();
    if (mc.isPaused() || mc.level == null) return;

    LocalPlayer player = mc.player;
    if (player == null) return;
    if (!player.onGround()) { distAccum = 0; return; }
    if (player.isInWater() || player.isInLava() || player.isFallFlying()) return;

    double speed = player.getDeltaMovement().horizontalDistance();
    if (speed < MIN_SPEED) { distAccum = 0; return; }

    BlockPos below = player.blockPosition().below();
    BlockState state = mc.level.getBlockState(below);
    if (!isAridSurface(state.getBlock())) { distAccum = 0; return; }

    distAccum += speed;
    boolean sprinting = player.isSprinting();
    double threshold = sprinting ? SPRINT_STEP : WALK_STEP;

    if (distAccum >= threshold)
    {
      distAccum -= threshold;
      spawnBurst(player, mc.level, sprinting, speed);
    }
  }

  private static boolean isAridSurface(Block block)
  {
    return block instanceof AridDust || block instanceof AridRock;
  }

  private static void spawnBurst(LocalPlayer player, ClientLevel level, boolean sprinting, double speed)
  {
    double vx = player.getDeltaMovement().x;
    double vz = player.getDeltaMovement().z;
    double len = Math.sqrt(vx * vx + vz * vz);
    if (len < 1e-6) return;

    double bx = -vx / len;
    double bz = -vz / len;

    int count = (sprinting ? SPRINT_COUNT : WALK_COUNT) * 16;
    double baseSpd = sprinting ? SPRINT_SPD : WALK_SPD;
    double upBase = sprinting ? SPRINT_UP : WALK_UP;

    double halfRad  = Math.toRadians(FAN_HALF_DEG);

    for (int i = 0; i < count; i++)
    {
      double t     = count == 1 ? 0.5 : (double) i / (count - 1);
      double angle = -halfRad + t * 2.0 * halfRad;

      double cosA = Math.cos(angle);
      double sinA = Math.sin(angle);
      double dvx  = (bx * cosA - bz * sinA) * baseSpd;
      double dvz  = (bx * sinA + bz * cosA) * baseSpd;

      double centerBias = 1.0 - Math.abs(t - 0.5) * 2.0;
      double dvy  = upBase + centerBias * 0.022 + RNG.nextDouble() * 0.018;

      dvx += (RNG.nextDouble() - 0.5) * 0.022;
      dvz += (RNG.nextDouble() - 0.5) * 0.022;

      double ox = (RNG.nextDouble() - 0.5) * 0.28;
      double oz = (RNG.nextDouble() - 0.5) * 0.28;

      level.addParticle(ModParticles.ARID_GROUND_SCATTER.get(), player.getX() + ox, player.getY() + 0.03, player.getZ() + oz, dvx, dvy, dvz);
    }
  }
}