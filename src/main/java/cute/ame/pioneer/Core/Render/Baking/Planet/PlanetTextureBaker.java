package cute.ame.pioneer.Core.Render.Baking.Planet;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;

@FunctionalInterface
public interface PlanetTextureBaker
{
    int FACE_FRONT = CubemapTextures.FACE_FRONT;
    int FACE_BACK = CubemapTextures.FACE_BACK;
    int FACE_LEFT = CubemapTextures.FACE_LEFT;
    int FACE_RIGHT = CubemapTextures.FACE_RIGHT;
    int FACE_TOP = CubemapTextures.FACE_TOP;
    int FACE_BOTTOM = CubemapTextures.FACE_BOTTOM;

    NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face);
}
