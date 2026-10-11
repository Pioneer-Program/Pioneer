package cute.ame.pioneer.Fluid.Helper;

import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import org.jetbrains.annotations.Nullable;

public final class SpeciesCycle
{
    public static @Nullable String next(String current, boolean back)
    {
        String[] keys = FluidSpecies.active().keys();
        if (keys.length == 0) return null;

        int index = 0;
        for (int i = 0; i < keys.length; i++)
        {
            if (keys[i].equals(current))
            {
                index = i;
                break;
            }
        }

        return keys[(index + (back ? keys.length - 1 : 1)) % keys.length];
    }
}
