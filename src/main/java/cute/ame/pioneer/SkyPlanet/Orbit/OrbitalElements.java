package cute.ame.pioneer.SkyPlanet.Orbit;

import cute.ame.pioneer.SkyPlanet.Data.OrbitDefinition;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record OrbitalElements
(
    double semiMajorAxis,
    double eccentricity,
    double inclination,
    double ascendingNode,
    double argPeriapsis,
    double meanAnomalyAtEpoch,
    long epochTick,
    double mu
)
{
    private static final double EPS = 1.0e-12;
    private static final short KEPLER_MAX_ITER = 12;
    private static final double KEPLER_TOLERANCE = 1.0e-12;

    static Vector3d toMath(Vector3dc mc, Vector3d dest)
    {
        return dest.set(mc.x(), -mc.z(), mc.y());
    }

    static Vector3d toMc(Vector3dc m, Vector3d dest)
    {
        return dest.set(m.x(), m.z(), -m.y());
    }

    private static double wrapTau(double angle)
    {
        return ((angle % Math.TAU) + Math.TAU) % Math.TAU;
    }

    /**
     * recovers elements from a pos and velocity, this is the function a rebase needs, a ship that has been thrusting inside its frame has a state vector, not elements.
     * @param rMc position relative to the parent body
     * @param vMc velocity relative to the parent body
     * @param tick the epoch these value describe
     * @param mu
     * @return
     */
    public static OrbitalElements fromStateVector(Vector3dc rMc, Vector3dc vMc, long tick, double mu)
    {
        Vector3d r = toMath(rMc, new Vector3d());
        Vector3d v = toMath(vMc, new Vector3d());

        double rLen = r.length();
        double v2 = v.lengthSquared();

        Vector3d h = r.cross(v, new Vector3d());
        Vector3d n = new Vector3d(0, 0, 1).cross(h, new Vector3d());
        Vector3d e = new Vector3d(r).mul(v2 - mu / rLen).sub(new Vector3d(v).mul(r.dot(v))).div(mu);

        double ecc = e.length();
        double energy = v2 / 2.0 - mu / rLen;

        double a = (Math.abs(ecc - 1.0) < 1.0e-9) ? Double.POSITIVE_INFINITY : -mu / (2.0 * energy);
        double i = Math.acos(Math.clamp(h.z / h.length(), -1.0, 1.0));

        double nLen = n.length();
        double raan = (nLen < EPS) ? 0.0 : Math.acos(Math.clamp(n.x / nLen, -1.0, 1.0));
        if (n.y < 0) raan = Math.TAU - raan;

        double argP;
        if (nLen < EPS)
        {
            argP = Math.atan2(e.y, e.x);
            if (h.z < 0) argP = Math.TAU - argP;
        }
        else if (ecc < EPS) argP = 0.0;
        else
        {
            argP = Math.acos(Math.clamp(n.dot(e) / (nLen * ecc), -1.0, 1.0));
            if (e.z < 0) argP = Math.TAU - argP;
        }

        double nu;
        if (ecc < EPS)
        {
            Vector3d ref = (nLen < EPS) ? new Vector3d(1, 0, 0) : new Vector3d(n).normalize();
            nu = Math.acos(Math.clamp(ref.dot(r) / rLen, -1.0, 1.0));
            if (r.z < 0) nu = Math.TAU - nu;
        }
        else
        {
            nu = Math.acos(Math.clamp(e.dot(r) / (ecc * rLen), -1.0, 1.0));
            if (r.dot(v) < 0) nu = Math.TAU - nu;
        }

        return new OrbitalElements(a, ecc, i, raan, argP, meanFromTrue(nu, ecc), tick, mu);
    }

    public static OrbitalElements fromOrbitDefinition(OrbitDefinition def, double mu)
    {
        double n = Math.TAU / (def.periodDays() * 24_000.0);
        double a = Math.cbrt(mu / (n * n));

        return new OrbitalElements(a, def.eccentricity(), def.inclination(), def.ascendingNode(), def.argPeriapsis(), def.startAngle(), 0L, mu);
    }

    public static OrbitalElements fromOrbitDefinition(OrbitDefinition def)
    {
        return fromOrbitDefinition(def, muFromOrbit(def.radius(), def.periodDays()));
    }

    public static double muFromOrbit(double radiusBlocks, double periodDays)
    {
        double n = Math.TAU / (periodDays * 24_000.0);
        return n * n * radiusBlocks * radiusBlocks * radiusBlocks;
    }

    //WARN: might be useless, not really sure of if it's practical in our system or not
    public static double semiMajorFromPeriod(double periodDays, double mu)
    {
        double n = Math.TAU / (periodDays * 24_000.0);
        return Math.cbrt(mu / (n * n));
    }

    /**
     * WARN: this is intentionally unimplemented, nothing execises it yet, fromOrbitDefinition only ever produces bound orbits, 
     * planet JSON has no escape trajectories by construction, and the only place a real vessel could go hyperbolix is when we implement thrusting, which is not yet implemented. so this is a placeholder for now.
     * 
     * so ship elliptic first, then we can implement hyperbolic later, but for now this is a placeholder to make sure we don't accidentally call this on an unbound orbit.
     */
    private void requireElliptic(String operation)
    {
        if (isEscaping()) throw new IllegalStateException(operation + " is elliptic-only; this trajectory is escaping (e = " + eccentricity + ", a = " + semiMajorAxis + "). to be implemented");
    }

    public double meanMotion()
    {
        return Math.sqrt(mu / (semiMajorAxis * semiMajorAxis * semiMajorAxis));
    }

    public double meanAnomalyAt(long tick, double partialTick)
    {
        return wrapTau(meanAnomalyAtEpoch + meanMotion() * (tick + partialTick - epochTick));
    }

    public double eccentricAnomalyAt(long tick, double partialTick)
    {
        return solveKepler(meanAnomalyAt(tick, partialTick), eccentricity);
    }

    public Vector3d positionAt(long tick, double partialTick, Vector3d dest)
    {
        requireElliptic("positionAt");
        double E = eccentricAnomalyAt(tick, partialTick);

        double cosE = Math.cos(E), sinE = Math.sin(E);
        double px = semiMajorAxis * (cosE - eccentricity);
        double py = semiMajorAxis * Math.sqrt(1.0 - eccentricity * eccentricity) * sinE;

        Vector3d p = new Vector3d(px, py, 0.0);
        p.rotateZ(argPeriapsis).rotateX(inclination).rotateZ(ascendingNode);

        return toMc(p, dest);
    }

    public Vector3d velocityAt(long tick, double partialTick, Vector3d dest)
    {
        requireElliptic("velocityAt");
        double E = eccentricAnomalyAt(tick, partialTick);
        double cosE = Math.cos(E), sinE = Math.sin(E);
        double r = semiMajorAxis * (1.0 - eccentricity * cosE);
        double k = Math.sqrt(mu * semiMajorAxis) / r;

        Vector3d v = new Vector3d(-k * sinE, k * Math.sqrt(1.0 - eccentricity * eccentricity) * cosE, 0.0);
        v.rotateZ(argPeriapsis).rotateX(inclination).rotateZ(ascendingNode);
        return toMc(v, dest);
    }

    public double radiusAt(long tick)
    {
        requireElliptic("radiusAt");
        return semiMajorAxis * (1.0 - eccentricity * Math.cos(eccentricAnomalyAt(tick, 0.0)));
    }

    public double speedAt(long tick)
    {
        requireElliptic("speedAt");
        return Math.sqrt(mu * (2.0 / radiusAt(tick) - 1.0 / semiMajorAxis));
    }

    public double period()
    {
        if (isEscaping()) return Double.NaN;
        return Math.TAU * Math.sqrt(semiMajorAxis * semiMajorAxis * semiMajorAxis / mu);
    }

    public boolean isEscaping()
    {
        return eccentricity >= 1.0 || !Double.isFinite(semiMajorAxis) || semiMajorAxis <= 0.0;
    }

    public static double solveKepler(double m, double e)
    {
        m = wrapTau(m);

        double E = (e < 0.8) ? m : Math.PI;
        for (int k = 0; k < KEPLER_MAX_ITER; k++)
        {
            double d = (E - e * Math.sin(E) - m) / (1.0 - e * Math.cos(E));
            E -= d;

            if (Math.abs(d) < KEPLER_TOLERANCE) break;
        }

        return E;
    }

    public static double meanFromTrue(double nu, double e)
    {
        if (e < EPS) return wrapTau(nu);

        double cosNu = Math.cos(nu);
        double E = Math.atan2(Math.sqrt(1.0 - e * e) * Math.sin(nu), e + cosNu);
        return wrapTau(E - e * Math.sin(E));
    }

    public static double trueFromMean(double m, double e)
    {
        double E = solveKepler(m, e);
        return wrapTau(Math.atan2(Math.sqrt(1.0 - e * e) * Math.sin(E), Math.cos(E) - e));
    }

    public double trueAnomalyAt(long tick, double partialTick)
    {
        double E = eccentricAnomalyAt(tick, partialTick);
        return wrapTau(Math.atan2(Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(E), Math.cos(E) - eccentricity));
    }

}