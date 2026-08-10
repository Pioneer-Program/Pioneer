package cute.ame.pioneer.SkyPlanet.Rendering.PostProcess;

import cute.ame.pioneer.SkyPlanet.Data.BlackHoleDefinition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BlackHolePostProcessManager
{
    private static volatile Vec3 activePos = null;
    private static volatile BlackHoleDefinition activeDef = null;

    public static void report(Vec3 worldPos, BlackHoleDefinition definition)
    {
        activePos = worldPos;
        activeDef = definition;
    }

    public static @Nullable Active consume()
    {
        Vec3 pos = activePos;
        BlackHoleDefinition def = activeDef;
        if (pos == null || def == null) return null;
        return new Active(pos, def);
    }

    public record Active(Vec3 worldPosition, BlackHoleDefinition definition) {}
}
