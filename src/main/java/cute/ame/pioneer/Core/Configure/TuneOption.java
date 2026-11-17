package cute.ame.pioneer.Core.Configure;

import cute.ame.pioneer.Core.Readout.ReadoutUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public record TuneOption(String key, byte kind, double value, double min, double max, double step, byte unit, List<String> choices, List<String> icons, boolean primary)
{
    public static final byte TOGGLE = 0;
    public static final byte RANGE = 1;
    public static final byte CHOICE = 2;

    public int steps()
    {
        return switch (kind)
        {
            case RANGE -> step > 0.0 ? (int) Math.round((max - min) / step) + 1 : 1;
            case CHOICE -> choices.size();
            default -> 2;
        };
    }

    public int index(double raw)
    {
        int index = kind == RANGE ? (int) Math.round(step > 0.0 ? (raw - min) / step : 0.0) : (int) Math.round(raw);
        return Mth.clamp(index, 0, Math.max(steps() - 1, 0));
    }

    public double valueAt(int index)
    {
        int clamped = Mth.clamp(index, 0, Math.max(steps() - 1, 0));
        return kind == RANGE ? Math.min(min + clamped * step, max) : clamped;
    }

    public double sanitize(double raw)
    {
        return valueAt(index(raw));
    }

    public float fraction(double raw)
    {
        int steps = steps();
        return steps <= 1 ? 0.0f : index(raw) / (float) (steps - 1);
    }

    public double valueAtFraction(double fraction)
    {
        return valueAt((int) Math.round(Mth.clamp(fraction, 0.0, 1.0) * (steps() - 1)));
    }

    public boolean isChoice()
    {
        return kind == CHOICE;
    }

    public String icon(int index)
    {
        return index >= 0 && index < icons.size() ? icons.get(index) : "";
    }

    public boolean isToggle()
    {
        return kind == TOGGLE;
    }

    public Component label(double raw)
    {
        if (kind == RANGE) return Component.literal(ReadoutUnit.byIndex(unit).format(sanitize(raw)));

        int index = index(raw);
        return index < choices.size() ? Component.translatable(choices.get(index)) : Component.literal(Integer.toString(index));
    }
}
