package cute.ame.pioneer.Core.Compat;

import cute.ame.pioneer.Pioneer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public final class IrisGbufferSync
{
    private static boolean initialized = false;
    private static boolean available = false;

    private static MethodHandle isShaderPackInUse;
    private static MethodHandle getPipelineNullable;
    private static MethodHandle bindDefault;
    private static Class<?> irisRenderingPipelineClass;

    private static synchronized void init()
    {
        if (initialized) return;
        initialized = true;

        try
        {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = irisApiClass.getMethod("getInstance");
            Object irisApiInstance = getInstance.invoke(null);
            Method isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            isShaderPackInUse = lookup.unreflect(isShaderPackInUseMethod).bindTo(irisApiInstance);

            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Method getPipelineManagerMethod = irisClass.getMethod("getPipelineManager");
            Object pipelineManagerCarrier = null;

            Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            Method getPipelineNullableMethod = pipelineManagerClass.getMethod("getPipelineNullable");

            MethodHandle getPipelineManager = lookup.unreflect(getPipelineManagerMethod);
            MethodHandle getPipelineNullableRaw = lookup.unreflect(getPipelineNullableMethod);
            getPipelineNullable = MethodHandles.filterReturnValue(getPipelineManager, getPipelineNullableRaw);

            irisRenderingPipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
            Method bindDefaultMethod = irisRenderingPipelineClass.getMethod("bindDefault");
            bindDefault = lookup.unreflect(bindDefaultMethod);

            available = true;
            Pioneer.LOGGER.info("Iris gbuffer sync hook initialized");
        } catch (Throwable t)
        {
            available = false;
            Pioneer.LOGGER.debug("Iris gbuffer sync hook unavailable ({}) :3", t.toString());
        }
    }

    public static void syncGbufferTargetIfNeeded()
    {
        if (!initialized) init();
        if (!available) return;

        try
        {
            boolean shaderPackActive = (boolean) isShaderPackInUse.invoke();
            if (!shaderPackActive) return;

            Object pipeline = getPipelineNullable.invoke();
            if (pipeline == null || !irisRenderingPipelineClass.isInstance(pipeline)) return;

            bindDefault.invoke(pipeline);
        }
        catch (Throwable t)
        {
            Pioneer.LOGGER.warn("failed to sync Iris gbuffer target for custom shader draw :cccc", t);
        }
    }
}
