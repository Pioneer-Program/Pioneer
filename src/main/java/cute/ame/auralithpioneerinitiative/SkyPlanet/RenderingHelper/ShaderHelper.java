package cute.ame.auralithpioneerinitiative.SkyPlanet.RenderingHelper;

import net.irisshaders.iris.api.v0.IrisApi;

@Deprecated(forRemoval = true)
public class ShaderHelper
{

	public static boolean shadersActive()
	{
		try { return IrisApi.getInstance().isShaderPackInUse(); }
		catch (Throwable t) { return false; }
	}
}
