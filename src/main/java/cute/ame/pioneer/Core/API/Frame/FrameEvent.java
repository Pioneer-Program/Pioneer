package cute.ame.pioneer.Core.API.Frame;

import cute.ame.pioneer.Frame.LocalFrame;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public abstract class FrameEvent extends Event
{
    private final ServerLevel level;
    private final LocalFrame frame;

    protected FrameEvent(ServerLevel level, LocalFrame frame)
    {
        this.level = level;
        this.frame = frame;
    }

    public ServerLevel level()
    {
        return level;
    }

    public LocalFrame frame()
    {
        return frame;
    }

    public enum Cause
    {
        MOTION,
        POSITION,
        ORIENTATION,
        PARENT,
        RECENTER,
        STATE
    }

    public static final class Created extends FrameEvent
    {
        public Created(ServerLevel level, LocalFrame frame)
        {
            super(level, frame);
        }
    }

    public static final class Released extends FrameEvent
    {
        public Released(ServerLevel level, LocalFrame frame)
        {
            super(level, frame);
        }
    }

    public static final class Changed extends FrameEvent
    {
        private final Cause cause;

        public Changed(ServerLevel level, LocalFrame frame, Cause cause)
        {
            super(level, frame);
            this.cause = cause;
        }

        public Cause cause()
        {
            return cause;
        }
    }

    public static final class Joined extends FrameEvent
    {
        private final ServerSubLevel ship;

        public Joined(ServerLevel level, LocalFrame frame, ServerSubLevel ship)
        {
            super(level, frame);
            this.ship = ship;
        }

        public ServerSubLevel ship()
        {
            return ship;
        }
    }
}
