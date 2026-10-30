package cute.ame.pioneer.Core.API.Frame;

import cute.ame.pioneer.Frame.LocalFrame;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

import java.util.List;

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

    public enum Reason
    {
        DISTANCE,
        EDGE,
        DORMANT
    }

    public static final class Split extends FrameEvent
    {
        private final LocalFrame into;
        private final List<ServerSubLevel> ships;
        private final int players;
        private final Reason reason;

        public Split(ServerLevel level, LocalFrame frame, LocalFrame into, List<ServerSubLevel> ships, int players, Reason reason)
        {
            super(level, frame);
            this.into = into;
            this.ships = ships;
            this.players = players;
            this.reason = reason;
        }

        public LocalFrame into()
        {
            return into;
        }

        public List<ServerSubLevel> ships()
        {
            return ships;
        }

        public int players()
        {
            return players;
        }

        public Reason reason()
        {
            return reason;
        }
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
