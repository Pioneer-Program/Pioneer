package cute.ame.pioneer.Core.Render.Helper;

import com.mojang.blaze3d.vertex.BufferBuilder;

public final class CubeGeometry
{
    private CubeGeometry() {}

    private static final float[] FACE_VERTS = new float[72];

    static
    {
        final int[] quads =
        {
            1, 5, 7, 3,   // +X
            0, 2, 6, 4,   // -X
            2, 3, 7, 6,   // +Y
            0, 4, 5, 1,   // -Y
            4, 6, 7, 5,   // +Z
            0, 1, 3, 2,   // -Z
        };

        for (int i = 0; i < 24; i++)
        {
            final int c = quads[i], o = i * 3;
            FACE_VERTS[o    ] = ((c     ) & 1) - 0.5f;
            FACE_VERTS[o + 1] = ((c >> 1) & 1) - 0.5f;
            FACE_VERTS[o + 2] = ((c >> 2) & 1) - 0.5f;
        }
    }

    public static float vert(int i)
    {
        return FACE_VERTS[i];
    }

    public static void emit(BufferBuilder buf, float scale)
    {
        final float[] v = FACE_VERTS;
        for (int i = 0; i < 72; i += 3)
        {
            final float x = v[i], y = v[i + 1], z = v[i + 2];
            buf.addVertex(x * scale, y * scale, z * scale)
               .setColor(1f, 1f, 1f, 1f)
               .setNormal(x, y, z);
        }
    }
}
