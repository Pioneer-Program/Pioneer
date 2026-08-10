package cute.ame.pioneer.Core.Compat;

import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public final class IrisVeilCompatHook
{
    private static final String[] VFX_SHADER_PATHS = {};
    private static boolean initialized = false;
    private static boolean available = false;
    private static MethodHandle excludeShaderReplacement;

    private static synchronized void init()
    {
        if (initialized) return;
        initialized = true;

        try
        {
            Class<?> registryClass = Class.forName("top.leonx.irisveil.compat.veil.VeilCompatRegistry");
            Method method = registryClass.getMethod("excludeShaderReplacement", ResourceLocation.class);
            excludeShaderReplacement = MethodHandles.lookup().unreflect(method);
            available = true;
        }
        catch (ClassNotFoundException e)
        {
            Pioneer.LOGGER.debug("iris-veil-compat not present, skipping shader exclusion hook");
            available = false;
        }
        catch (ReflectiveOperationException e)
        {
            Pioneer.LOGGER.warn("Failed to bind iris-veil-compat exclusion API", e);
            available = false;
        }
    }

    public static void tryExcludeShaderReplacement(ResourceLocation shaderPath)
    {
        if (!initialized) init();
        if (!available) return;

        try
        {
            excludeShaderReplacement.invoke(shaderPath);
        }
        catch (Throwable t)
        {
            Pioneer.LOGGER.warn("Failed to register shader exclusion for '{}' with iris-veil-compat", shaderPath, t);
        }
    }

    public static void tryExcludePioneerVfxShaders()
    {
        int count = 0;
        for (String path : VFX_SHADER_PATHS)
        {
            tryExcludeShaderReplacement(ResourceLocation.parse(path));
            count++;
        }
        if (available) Pioneer.LOGGER.info("Registered {} VFX shader(s) with iris-veil-compat exclusion list", count);
    }
}
