package cute.ame.pioneer.Seamless;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SeamlessFadeState
{
    public enum Phase { NONE, FADING_OUT, HOLDING, FADING_IN }

    public static final int FADE_OUT_TICKS = 3;
    public static final int FADE_IN_TICKS = 6;
    public static final int MAX_HOLD_TICKS = 60;

    private static volatile Phase phase = Phase.NONE;
    private static volatile int ticksInPhase = 0;
    private static volatile int holdTicks = 0;
    private static volatile ResourceKey<Level> expectedTargetDim = null;
    private static volatile float alpha = 0f;

    public static void beginFadeOut(ResourceKey<Level> targetDim)
    {
        phase = Phase.FADING_OUT;
        ticksInPhase = 0;
        holdTicks = 0;
        expectedTargetDim = targetDim;
    }

    public static void beginFadeIn()
    {
        if (phase == Phase.NONE) return;
        phase = Phase.FADING_IN;
        ticksInPhase = 0;
    }

    public static void reset()
    {
        phase = Phase.NONE;
        ticksInPhase = 0;
        holdTicks = 0;
        expectedTargetDim = null;
        alpha = 0f;
    }

    public static Phase phase()
    {
        return phase;
    }

    public static ResourceKey<Level> expectedTargetDim()
    {
        return expectedTargetDim;
    }

    public static float alpha()
    {
        return alpha;
    }

    public static boolean isActive()
    {
        return phase != Phase.NONE;
    }

    public static void tick(boolean playerLevelMatchesTarget)
    {
        switch (phase)
        {
            case NONE -> alpha = 0f;

            case FADING_OUT ->
            {
                ticksInPhase++;
                alpha = Math.min(1f, ticksInPhase / (float) FADE_OUT_TICKS);
                if (alpha >= 1f || playerLevelMatchesTarget)
                {
                    alpha = 1f;
                    phase = Phase.HOLDING;
                    ticksInPhase = 0;
                }
            }

            case HOLDING ->
            {
                alpha = 1f;
                holdTicks++;
                if (holdTicks >= MAX_HOLD_TICKS) beginFadeIn();
            }

            case FADING_IN ->
            {
                ticksInPhase++;
                alpha = Math.max(0f, 1f - ticksInPhase / (float) FADE_IN_TICKS);
                if (alpha <= 0f) reset();
            }
        }
    }
}
