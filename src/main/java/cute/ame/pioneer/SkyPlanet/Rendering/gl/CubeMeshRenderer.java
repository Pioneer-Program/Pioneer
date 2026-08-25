package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

public final class CubeMeshRenderer
{
    private static final float[][][] FACES =
    {
        {{-1, 1, 1},{1, 1, 1},{1, 1,-1},{-1, 1,-1}},
        {{-1,-1,-1},{1,-1,-1},{1,-1, 1},{-1,-1, 1}},
        {{ 1, 1, 1},{-1,1, 1},{-1,-1,1},{ 1,-1, 1}},
        {{ 1, 1,-1},{-1,1,-1},{-1,-1,-1},{1,-1,-1}},
        {{ 1, 1,-1},{1, 1, 1},{1,-1, 1},{1,-1,-1}},
        {{-1, 1, 1},{-1,1,-1},{-1,-1,-1},{-1,-1,1}},
    };

    public static void renderColoredCube(PoseStack ps, float r, float g, float b, float a)
    {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();
        for (float[][] face : FACES)
            for (float[] v : face)
                buf.addVertex(m, v[0]*0.5f, v[1]*0.5f, v[2]*0.5f).setColor(r, g, b, a);

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }
}