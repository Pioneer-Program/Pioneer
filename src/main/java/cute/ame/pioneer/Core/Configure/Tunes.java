package cute.ame.pioneer.Core.Configure;

import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Pioneer;

import java.util.ArrayList;
import java.util.List;

public final class Tunes
{
    public static List<String> speciesChoices()
    {
        String[] keys = FluidSpecies.active().keys();
        List<String> choices = new ArrayList<>(keys.length);
        for (String key : keys) choices.add("gas.pioneer." + key);

        return choices;
    }

    public static List<String> speciesIcons()
    {
        String[] keys = FluidSpecies.active().keys();
        List<String> icons = new ArrayList<>(keys.length);
        for (String key : keys) icons.add(icon("gas/" + key));

        return icons;
    }

    public static String icon(String path)
    {
        return Pioneer.MODID + ":textures/gui/tune/" + path + ".png";
    }

    public static int speciesIndex(String key)
    {
        String[] keys = FluidSpecies.active().keys();
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return i;

        return 0;
    }

    public static String speciesAt(int index)
    {
        String[] keys = FluidSpecies.active().keys();
        return keys.length == 0 ? "" : keys[Math.clamp(index, 0, keys.length - 1)];
    }
}
