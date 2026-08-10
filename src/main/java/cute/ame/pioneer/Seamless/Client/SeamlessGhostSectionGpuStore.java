package cute.ame.pioneer.Seamless.Client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SeamlessGhostSectionGpuStore
{
    private static final Map<ResourceKey<Level>, Map<Long, VertexBuffer>> BUFFERS = new ConcurrentHashMap<>();

    public static VertexBuffer getOrUpload(ResourceKey<Level> dimension, long key, SeamlessGhostSectionMesher.MeshedSection section)
    {
        Map<Long, VertexBuffer> forDim = BUFFERS.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        VertexBuffer existing = forDim.get(key);
        if (existing != null) return existing;

        if (section.quads.isEmpty()) return null;

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        for (SeamlessGhostSectionMesher.Quad quad : section.quads)
        {
            addQuad(buf, quad);
        }
        MeshData mesh = buf.buildOrThrow();

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();

        forDim.put(key, buffer);
        return buffer;
    }

    private static void addQuad(BufferBuilder buffer, SeamlessGhostSectionMesher.Quad quad)
    {
        float nx = quad.normal().getStepX(), ny = quad.normal().getStepY(), nz = quad.normal().getStepZ();
        float shade = quad.shade();
        int light = quad.packedLight();
        int tint = quad.tint();
        float tr = shade * ((tint >> 16 & 0xFF) / 255.0f);
        float tg = shade * ((tint >> 8 & 0xFF) / 255.0f);
        float tb = shade * ((tint & 0xFF) / 255.0f);
        float[][] c = quad.corners();
        float[] us = {quad.u0(), quad.u0(), quad.u1(), quad.u1()};
        float[] vs = {quad.v0(), quad.v1(), quad.v1(), quad.v0()};
        for (int i = 0; i < 4; i++)
        {
            buffer.addVertex(c[i][0], c[i][1], c[i][2]).setColor(tr, tg, tb, 1.0f).setUv(us[i], vs[i]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        }
    }

    public static void invalidate(ResourceKey<Level> dimension, long key)
    {
        Map<Long, VertexBuffer> forDim = BUFFERS.get(dimension);
        if (forDim == null) return;
        VertexBuffer removed = forDim.remove(key);
        if (removed != null) removed.close();
    }

    public static void clearDimension(ResourceKey<Level> dimension)
    {
        Map<Long, VertexBuffer> forDim = BUFFERS.remove(dimension);
        if (forDim == null) return;
        for (VertexBuffer buffer : forDim.values()) buffer.close();
    }
}