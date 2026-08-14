package cute.ame.pioneer.Core.Render.Debug;

import com.mojang.blaze3d.systems.RenderSystem;
import cute.ame.pioneer.Pioneer;
import net.neoforged.fml.loading.FMLLoader;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GPUProfiler
{
    public static final boolean DEBUG = !FMLLoader.isProduction() || Boolean.getBoolean("pioneer.gpuprofiler");
    private static final int RING = 3;

    private static final class Section
    {
        final int[] startQueries = new int[RING];
        final int[] endQueries = new int[RING];
        final boolean[] inFlight = new boolean[RING];
        double emaMillis;
    }

    private static final Map<String, Section> SECTIONS = new LinkedHashMap<>();

    private static boolean enabled = DEBUG;
    private static int frame = 0;
    private static String active = null;
    private static Section activeSection = null;
    private static int activeSlot = -1;

    static
    {
        if (DEBUG) Pioneer.LOGGER.info("[Pioneer] GPUProfiler active (DEBUG=true)");
    }

    public static void setEnabled(boolean value)
    {
        if (!DEBUG) return;
        RenderSystem.assertOnRenderThreadOrInit();
        enabled = value;
        if (!value) clear();
    }

    public static boolean isEnabled()
    {
        return DEBUG && enabled;
    }

    public static void begin(String name)
    {
        if (!DEBUG || !enabled) return;
        RenderSystem.assertOnRenderThreadOrInit();

        if (active != null) return;

        Section s = SECTIONS.computeIfAbsent(name, k ->
        {
            Section created = new Section();
            for (int i = 0; i < RING; i++)
            {
                created.startQueries[i] = GL15.glGenQueries();
                created.endQueries[i] = GL15.glGenQueries();
            }
            return created;
        });

        int slot = frame % RING;
        if (s.inFlight[slot]) return;

        GL33.glQueryCounter(s.startQueries[slot], GL33.GL_TIMESTAMP);
        activeSection = s;
        activeSlot = slot;
        active = name;
    }

    public static void end()
    {
        if (!DEBUG || !enabled || active == null) return;

        GL33.glQueryCounter(activeSection.endQueries[activeSlot], GL33.GL_TIMESTAMP);
        activeSection.inFlight[activeSlot] = true;

        activeSection = null;
        activeSlot = -1;
        active = null;
    }

    public static void endFrame()
    {
        if (!DEBUG || !enabled) return;
        RenderSystem.assertOnRenderThreadOrInit();

        if (active != null)
        {
            Pioneer.LOGGER.warn("[Pioneer] GPUProfiler: section '{}' left open at endFrame()", active);
            activeSection = null;
            activeSlot = -1;
            active = null;
        }

        int readSlot = (frame + 1) % RING;

        for (Section s : SECTIONS.values())
        {
            if (!s.inFlight[readSlot]) continue;
            if (GL15.glGetQueryObjecti(s.endQueries[readSlot], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) continue;

            long t0 = GL33.glGetQueryObjectui64(s.startQueries[readSlot], GL15.GL_QUERY_RESULT);
            long t1 = GL33.glGetQueryObjectui64(s.endQueries[readSlot], GL15.GL_QUERY_RESULT);

            s.inFlight[readSlot] = false;
            double millis = (t1 - t0) / 1_000_000.0;
            s.emaMillis = s.emaMillis == 0.0 ? millis : s.emaMillis * 0.9 + millis * 0.1;
        }

        frame++;
    }

    public static Map<String, Double> snapshot()
    {
        if (!DEBUG || SECTIONS.isEmpty()) return Collections.emptyMap();

        Map<String, Double> out = new LinkedHashMap<>(SECTIONS.size());
        for (Map.Entry<String, Section> e : SECTIONS.entrySet())
            out.put(e.getKey(), e.getValue().emaMillis);

        return out;
    }

    public static double totalMillis()
    {
        if (!DEBUG) return 0.0;

        double total = 0.0;
        for (Section s : SECTIONS.values()) total += s.emaMillis;

        return total;
    }

    public static double millis(String name)
    {
        if (!DEBUG) return 0.0;
        Section s = SECTIONS.get(name);
        return s == null ? 0.0 : s.emaMillis;
    }

    public static void clear()
    {
        if (!DEBUG) return;
        RenderSystem.assertOnRenderThreadOrInit();

        activeSection = null;
        activeSlot = -1;
        active = null;

        for (Section s : SECTIONS.values())
            for (int i = 0; i < RING; i++)
            {
                GL15.glDeleteQueries(s.startQueries[i]);
                GL15.glDeleteQueries(s.endQueries[i]);
            }

        SECTIONS.clear();
        frame = 0;
    }
}