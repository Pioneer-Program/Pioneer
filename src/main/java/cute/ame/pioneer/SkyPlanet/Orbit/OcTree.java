package cute.ame.pioneer.SkyPlanet.Orbit;

import cute.ame.pioneer.SkyPlanet.Orbit.MassList;
import cute.ame.pioneer.SkyPlanet.Orbit.TreeNode;

import org.joml.Vector3d;
import java.lang.String;
import java.lang.Integer;
import java.lang.Double;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

public class OcTree {

    public HashMap<String, TreeNode> tree;

    public OcTree(MassList m)
    {
        /* creates an octree object by recursively calling createOcTree */
        
        /* create a new empty map to store the octree */
        this.tree = new HashMap<String, TreeNode>(m.size());

        /* set up bounding box to contain all locations in the input list */
        ArrayList<Double> bBox = m.getBbox();

        /* invoke recursive octree partitioning */
        this.populateOcTree(m, bBox, 0);
    }

    public ArrayList<String> populateOcTree(MassList m, ArrayList<Double> bBox, int depth)
    {
        /* partitions a list of masses into an octree node or leaf */
        
        if (m.size() > 1)
        {
        
            // this node has multiple leaves, recursively partition
            ArrayList<ArrayList<Double>> octants = splitOctants(bBox);
            ArrayList<String> n = new ArrayList<>();
            
            for (ArrayList<Double> ocBox : octants)
            {
                ArrayList<MassList> inout = findInliers(m, ocBox);
                MassList inliers = inout.get(0);
                MassList outliers = inout.get(1);

                ArrayList<String> newnodes = this.populateOcTree(inliers, ocBox, depth+1);

                if (newnodes.size() > 1)
                {
                    // this octant was a node, add it to the tree
                    // if newnodes.size = 1 the subcall already added the leaf so we don't have to

                    // get masses and centers of mass of children
                    // cemter of mass = sum(m * r) / sum(m)
                    Double mtot = 0.0d;
                    Vector3d comtot = new Vector3d(0.0d, 0.0d, 0.0d);
                    
                    for (String childId : newnodes)
                    {
                        TreeNode child = this.tree.get(childId);
                        mtot += child.mu();
                        comtot += child.mu() * child.com();
                    }
                    comtot = comtot / mtot;

                    // create new id and add it to the list
                    String nodeId = "node" + Integer.toString(this.tree.size());
                    n.add(nodeId);

                    // add octree entry for this new node
                    this.tree.put(
                        nodeId,
                        new TreeNode(
                            ocBox,
                            mtot,
                            comtot,
                            newnodes
                        )
                    );
                }
                else if (newnodes.size() == 1)
                {
                    // this octant was a leaf, add its id
                    n.add(newnodes.get(0));
                }

                m = outliers;
            }

            if (depth == 0)
            {
                // create root node
                // get masses and centers of mass of children
                // cemter of mass = sum(m * r) / sum(m)
                Double mtot = 0.0d;
                Vector3d comtot = new Vector3d(0.0d, 0.0d, 0.0d);
                    
                for (String childId : n)
                {
                    TreeNode child = this.tree.get(childId);
                    mtot += child.mu();
                    comtot += child.mu() * child.com();
                }
                comtot = comtot / mtot;

                // assign root id
                Strimg nodeId = "root";

                // add octree entry for this new node
                this.tree.put(
                    nodeId,
                    new TreeNode(
                        bBox,
                        mtot,
                        comtot,
                        n
                    )
                );

                return new ArrayList<String>(Arrays.asList(nodeId));
            }

            return n;
        }
        else if (m.size() == 1)
        {
            // we've reached a leaf, add to list
            MassObject mi = m.get(0);
            this.tree.put(
                mi.getId(),
                new TreeNode(
                    bBox,
                    mi.getMu(),
                    mi.getR(),
                    new ArrayList<String>()
                )
            );

            return new ArrayList<String>(Arrays.asList(mi.getId()));
        }

        // if massList was empty, return empty list
        return new ArrayList<String>();
    }

    static public ArrayList<ArrayList<Double>> splitOctants(ArrayList<Double> bBox)
    {
        /* splits a 3d rectangular prism into 8 octants */
        /* bBox = [xMin, xMax; yMin yMax; zMin, zMax] */
        
        Double xMin = bBox.get(0);
        Double xMax = bBox.get(1);
        Double yMin = bBox.get(2);
        Double yMax = bBox.get(3);
        Double zMin = bBox.get(4);
        Double zMax = bBox.get(5);
        Double halfX = (xMax - xMin) / 2.;
        Double halfY = (yMax - yMin) / 2.;
        Double halfZ = (zMax - zMin) / 2.;
        Double xMid = xMin + halfX;
        Double yMid = yMin + halfY;
        Double zMid = zMin + halfZ;

        // octant 0 +++
        ArrayList<Double> bBox0 = new ArrayList<>(Arrays.asList(
            xMid, xMax,
            yMid, yMax,
            zMid, zMax
        ));

        // octant 1 ++-
        ArrayList<Double> bBox1 = new ArrayList<>(Arrays.asList(
            xMid, xMax,
            yMid, yMax,
            zMin, zMid
        ));

        // octant 2 +-+
        ArrayList<Double> bBox2 = new ArrayList<>(Arrays.asList(
            xMid, xMax,
            yMin, yMid,
            zMid, zMax
        ));

        // octant 3 +--
        ArrayList<Double> bBox3 = new ArrayList<>(Arrays.asList(
            xMid, xMax,
            yMin, yMid,
            zMin, zMid
        ));

        // octant 4 -++
        ArrayList<Double> bBox4 = new ArrayList<>(Arrays.asList(
            xMin, xMid,
            yMid, yMax,
            zMid, zMax
        ));

        // octant 5 -+-
        ArrayList<Double> bBox5 = new ArrayList<>(Arrays.asList(
            xMin, xMid,
            yMid, yMax,
            zMin, zMid
        ));

        // octant 6 --+
        ArrayList<Double> bBox6 = new ArrayList<>(Arrays.asList(
            xMin, xMid,
            yMin, yMid,
            zMid, zMax
        ));

        // octant 7 ---
        ArrayList<Double> bBox7 = new ArrayList<>(Arrays.asList(
            xMin, xMid,
            yMin, yMid,
            zMin, zMid
        ));

        return new ArrayList<ArrayList<Double>>(Arrays.asList(bBox0, bBox1, bBox2, bBox3, bBox4, bBox5, bBox6, bBox7));
    }

    static public ArrayList<MassList> findInliers(MassList m, ArrayList<Double> bBox)
    {
        /* splits the input list by location: elements within the bounding box, and those outside */
        Double xMin = bBox.get(0);
        Double xMax = bBox.get(1);
        Double yMin = bBox.get(2);
        Double yMax = bBox.get(3);
        Double zMin = bBox.get(4);
        Double zMax = bBox.get(5);

        MassList inliers = new MassList();
        MassList outliers = new MassList();

        int n = m.size();

        Vector3d r;
        Boolean xIn, yIn, zIn;
        for (MassObject mi : m.data())
        {
            r = mi.getR();

            xIn = (r.x() >= xMin) & (r.x() <= xMax);
            yIn = (r.y() >= yMin) & (r.y() <= yMax);
            zIn = (r.z() >= zMin) & (r.z() <= zMax);

            if (xIn & yIn & zIn)
            {
                inliers.data().add(mi);
            }
            else
            {
                outliers.data().add(mi);
            }
        }

        return new ArrayList<MassList>(Arrays.asList(inliers, outliers));
    }

    public Vector3d nBodyGravity(MassObject point, String root, Double theta)
    {
        /* get gravitational acceleration for a single location due to the whole tree */
    }

    public ArrayList<Vector3d> ensembleGravity(MassList m, Double theta)
    {
        /* calculate gravitational acceleration at all locations in the input list */
    }
}

