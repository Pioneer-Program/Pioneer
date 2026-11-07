package cute.ame.pioneer.Core.Configure;

import cute.ame.pioneer.Core.Readout.ReadoutUnit;

import java.util.ArrayList;
import java.util.List;

public final class TuneOptions
{
    private final List<TuneOption> options = new ArrayList<>(4);

    public TuneOptions toggle(String key, boolean on, String offKey, String onKey)
    {
        options.add(new TuneOption(key, TuneOption.TOGGLE, on ? 1.0 : 0.0, 0.0, 1.0, 1.0, (byte) ReadoutUnit.NONE.ordinal(), List.of(offKey, onKey), List.of(), false));
        return this;
    }

    public TuneOptions range(String key, double value, double min, double max, double step, ReadoutUnit unit, boolean primary)
    {
        options.add(new TuneOption(key, TuneOption.RANGE, value, min, max, step, (byte) unit.ordinal(), List.of(), List.of(), primary));
        return this;
    }

    public TuneOptions choice(String key, int index, List<String> choices, List<String> icons, boolean primary)
    {
        options.add(new TuneOption(key, TuneOption.CHOICE, index, 0.0, Math.max(choices.size() - 1, 0), 1.0, (byte) ReadoutUnit.NONE.ordinal(), List.copyOf(choices), List.copyOf(icons), primary));
        return this;
    }

    public List<TuneOption> list()
    {
        return options;
    }
}
