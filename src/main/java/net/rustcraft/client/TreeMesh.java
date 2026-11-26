package net.rustcraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.rustcraft.RustCraft;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Загружает 3D-модель из нашего компактного формата RMSH (см. assets/rustcraft/meshes). */
public class TreeMesh {
    /** verts: на каждую вершину 8 чисел (x,y,z, u,v, nx,ny,nz); idx: тройки индексов. */
    public record Part(Identifier texture, float[] verts, int[] idx) {}

    private static final Map<String, List<Part>> CACHE = new HashMap<>();

    public static List<Part> get(String name) {
        return CACHE.computeIfAbsent(name, TreeMesh::load);
    }

    private static final Map<String, Float> SURFACE = new HashMap<>();

    /** Расстояние от оси ствола до его поверхности на высоте y в сторону (fx, fz): крест ложится ровно на кору. */
    public static float trunkSurface(String name, float y, int fx, int fz) {
        String key = name + "|" + Math.round(y * 4) + "|" + fx + "|" + fz;
        Float cached = SURFACE.get(key);
        if (cached != null) return cached;
        float best = 0.12f;
        for (Part p : get(name)) {
            if (!p.texture().getPath().contains("bark")) continue;
            float[] v = p.verts();
            for (int i = 0; i < v.length; i += 8) {
                if (Math.abs(v[i + 1] - y) > 0.35f) continue;
                float along = fx != 0 ? v[i] * fx : v[i + 2] * fz;
                float lateral = fx != 0 ? Math.abs(v[i + 2]) : Math.abs(v[i]);
                if (lateral < 0.35f && along > best) best = along;
            }
        }
        SURFACE.put(key, best);
        return best;
    }

    private static List<Part> load(String name) {
        List<Part> parts = new ArrayList<>();
        try (InputStream in = MinecraftClient.getInstance().getResourceManager()
                .open(Identifier.of(RustCraft.MOD_ID, "meshes/" + name + ".bin"))) {
            ByteBuffer b = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            b.position(4); // "RMSH"
            int n = b.getInt();
            for (int i = 0; i < n; i++) {
                int tl = b.getInt();
                byte[] tb = new byte[tl];
                b.get(tb);
                String tex = new String(tb, StandardCharsets.UTF_8);
                b.get(); // флаг прозрачности (не нужен: используем cutout)
                int nv = b.getInt();
                int ni = b.getInt();
                float[] v = new float[nv * 8];
                for (int k = 0; k < v.length; k++) v[k] = b.getFloat();
                int[] idx = new int[ni];
                for (int k = 0; k < ni; k++) idx[k] = b.getInt();
                parts.add(new Part(Identifier.of(RustCraft.MOD_ID, "textures/mesh/" + tex + ".png"), v, idx));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parts;
    }
}
