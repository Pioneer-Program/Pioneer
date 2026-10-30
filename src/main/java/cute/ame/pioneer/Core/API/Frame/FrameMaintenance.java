package cute.ame.pioneer.Core.API.Frame;

import java.util.ArrayList;
import java.util.List;

public final class FrameMaintenance
{
    public final List<String> waiting = new ArrayList<>(2);
    public int frames, dormant, swept, adopted, recentered, split, merged;

    public FrameMaintenance clear()
    {
        frames = dormant = swept = adopted = recentered = split = merged = 0;
        waiting.clear();
        return this;
    }
}
