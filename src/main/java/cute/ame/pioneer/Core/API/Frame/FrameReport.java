package cute.ame.pioneer.Core.API.Frame;

public final class FrameReport
{
    public int subLevels, riders, seated, free, entities, tick;

    public int players()
    {
        return riders + seated + free;
    }
}
