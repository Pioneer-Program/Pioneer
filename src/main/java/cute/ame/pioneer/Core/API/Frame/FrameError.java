package cute.ame.pioneer.Core.API.Frame;

import java.io.Serial;

public final class FrameError extends RuntimeException
{
    @Serial
    private static final long serialVersionUID = -0x7aB7;

    public FrameError(String message)
    {
        super(message, null, false, false);
    }
}
