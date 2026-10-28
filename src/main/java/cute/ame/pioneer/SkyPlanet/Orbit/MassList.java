package cute.ame.pioneer.SkyPlanet.Orbit;

import cute.ame.pioneer.SkyPlanet.Orbit.MassObject;

import org.joml.Vector3d;
import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record MassList
(
    ArrayList<MassObject> data
)
{
    public ArrayList<Double> getBbox()
    {
        /* initialize lists of x, y, and z */
        int n = this.size();
        ArrayList<Double> x = new ArrayList<Double>(n);
        ArrayList<Double> y = new ArrayList<Double>(n);
        ArrayList<Double> z = new ArrayList<Double>(n);

        /* split array of vector3d into 3 arrays of Doubles */
        Vector3d ri = new Vector3d();
        for (int i=0; i<n; i++)
        {
            ri = this.data().get(i).getR();
            x.set(i, ri.x());
            y.set(i, ri.y());
            z.set(i, ri.x());
        }

        /* sort the new arrays */
        Collections.sort(x);
        Collections.sort(y);
        Collections.sort(z);

        /* extract the max/min */
        Double xMin = x.get(0);
        Double xMax = x.get(n-1);
        Double yMin = y.get(0);
        Double yMax = y.get(n-1);
        Double zMin = z.get(0);
        Double zMax = z.get(n-1);

        ArrayList<Double> bBox = new ArrayList<>(Arrays.asList(xMin, xMax, yMin, yMax, zMin, zMax));
        return bBox;
    }

    public MassObject get(Integer i)
    {
        return this.data().get(i);
    }

    public Integer size()
    {
        return this.data().size();
    }

    public ArrayList<Vector3d> listR()
    {
        int n = this.size();
        ArrayList<Vector3d> out = new ArrayList<Double>(n);

        for (int i=0; i<n; i++)
        {
            out.set(i, this.data().get(i).getR());
        }

        return out;
    }

    public ArrayList<Vector3d> listV()
    {
        int n = this.size();
        ArrayList<Vector3d> out = new ArrayList<Double>(n);

        for (int i=0; i<n; i++)
        {
            out.set(i, this.data().get(i).getV());
        }

        return out;
    }

    public ArrayList<Vector3d> listA()
    {
        int n = this.size();
        ArrayList<Vector3d> out = new ArrayList<Double>(n);

        for (int i=0; i<n; i++)
        {
            out.set(i, this.data().get(i).getA());
        }

        return out;
    }
}
