package cute.ame.pioneer.SkyPlanet.Orbit;

import org.joml.Vector3d;
import java.lang.String;
import java.util.ArrayList;

public record TreeNode
(
    ArrayList<Double> bBox,
    Double mu,
    Vector3d com,
    ArrayList<String> children
) 
{
    
}
