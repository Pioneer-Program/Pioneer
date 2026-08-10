package cute.ame.pioneer.Core.Render.Helper;

import net.minecraft.resources.ResourceLocation;

public record CubemapTextures(ResourceLocation front, ResourceLocation back, ResourceLocation left, ResourceLocation right, ResourceLocation top, ResourceLocation bottom)
{
    public static final int FACE_FRONT = 0;
    public static final int FACE_BACK = 1;
    public static final int FACE_LEFT = 2;
    public static final int FACE_RIGHT = 3;
    public static final int FACE_TOP = 4;
    public static final int FACE_BOTTOM = 5;

    public static final String[] FACE_NAMES = { "front", "back", "left", "right", "top", "bottom" };

    public static CubemapTextures of(ResourceLocation[] handles)
    {
        return new CubemapTextures(handles[FACE_FRONT], handles[FACE_BACK], handles[FACE_LEFT], handles[FACE_RIGHT], handles[FACE_TOP], handles[FACE_BOTTOM]);
    }

    public ResourceLocation get(int face)
    {
        return switch (face)
        {
            case FACE_FRONT -> front;
            case FACE_BACK -> back;
            case FACE_LEFT -> left;
            case FACE_RIGHT -> right;
            case FACE_TOP -> top;
            case FACE_BOTTOM -> bottom;
            default -> throw new IllegalArgumentException("Invalid face: " + face);
        };
    }
}
