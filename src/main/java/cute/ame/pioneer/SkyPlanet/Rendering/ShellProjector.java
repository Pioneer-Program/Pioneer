package cute.ame.pioneer.SkyPlanet.Rendering;

public final class ShellProjector
{
    public static final float SHELL_RADIUS = 900_000.0f;
    public static final float MIN_APPARENT = 0.3f;

    public static final class Projected
    {
        public final float dx, dy, dz, size;

        Projected(float dx, float dy, float dz, float size)
        {
            this.dx = dx; this.dy = dy; this.dz = dz; this.size = size;
        }
    }

    public static Projected projectToSafeShell(double realDx, double realDy, double realDz, double realDist, float realSize)
    {
        if (realDist <= SHELL_RADIUS) return new Projected((float) realDx, (float) realDy, (float) realDz, Math.max(realSize, MIN_APPARENT));

        float nx = (float) (realDx / realDist), ny = (float) (realDy / realDist), nz = (float) (realDz / realDist);
        float renderSize = Math.max((float) (SHELL_RADIUS * (realSize / realDist)), MIN_APPARENT);
        return new Projected(nx * SHELL_RADIUS, ny * SHELL_RADIUS, nz * SHELL_RADIUS, renderSize);
    }
}
