package net.rustcraft.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.rustcraft.Marker;
import net.rustcraft.RustCraft;
import net.rustcraft.RustState;
import net.rustcraft.TreeBaseBlock;
import net.rustcraft.TreeBlockEntity;

import java.util.List;

/** Рисует 3D-модель дерева и жёлтый крест на стволе. */
public class TreeRenderer implements BlockEntityRenderer<TreeBlockEntity, TreeRenderer.State> {
    private static final String[] NAMES = {"acacia", "tree1", "tree2", "tree3"};
    private static final Identifier CROSS = Identifier.of(RustCraft.MOD_ID, "textures/mesh/cross.png");
    private static final int FULL_LIGHT = 15728880;

    public static class State extends BlockEntityRenderState {
        public int variant;
        public int light = FULL_LIGHT;
        public boolean showCross;
        public int crossY;
        public int faceX;   // -1, 0, 1
        public int faceZ;   // -1, 0, 1
        public float crossOffset = 0.2f;
    }

    public TreeRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public boolean rendersOutsideBoundingBox() {
        return true; // крона шире блока, иначе дерево пропадает у края экрана
    }

    @Override
    public int getRenderDistance() {
        return 48;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(TreeBlockEntity be, State state, float tickProgress, Vec3d cameraPos,
                                  ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        BlockEntityRenderer.super.updateRenderState(be, state, tickProgress, cameraPos, crumbling);
        state.variant = be.getCachedState().get(TreeBaseBlock.VARIANT);
        var world = be.getWorld();
        BlockPos pos = be.getPos();
        state.showCross = false;
        if (world == null) return;

        BlockPos above = pos.up(4);
        state.light = (world.getLightLevel(LightType.BLOCK, above) << 4) | (world.getLightLevel(LightType.SKY, above) << 20);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (RustState.rust && mc.player != null) {
            double dx = mc.player.getX() - (pos.getX() + 0.5);
            double dz = mc.player.getZ() - (pos.getZ() + 0.5);
            if (dx * dx + dz * dz < 12 * 12) {
                int count = RustCraftClient.COUNTS.getOrDefault(pos, 0);
                BlockPos marker = Marker.at(world, pos, count);
                state.crossY = marker.getY() - pos.getY();
                if (Math.abs(dx) > Math.abs(dz)) {
                    state.faceX = dx > 0 ? 1 : -1;
                    state.faceZ = 0;
                } else {
                    state.faceX = 0;
                    state.faceZ = dz > 0 ? 1 : -1;
                }
                state.crossOffset = TreeMesh.trunkSurface(NAMES[state.variant], state.crossY + 0.5f, state.faceX, state.faceZ) + 0.01f;
                state.showCross = true;
            }
        }
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        List<TreeMesh.Part> parts = TreeMesh.get(NAMES[state.variant]);
        final int light = state.light;

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        for (TreeMesh.Part part : parts) {
            RenderLayer layer = RenderLayers.entityCutoutNoCull(part.texture());
            queue.submitCustom(matrices, layer, (entry, vc) -> {
                float[] v = part.verts();
                int[] ix = part.idx();
                for (int i = 0; i + 2 < ix.length; i += 3) {
                    for (int k = 0; k < 4; k++) {
                        int j = ix[i + Math.min(k, 2)] * 8;
                        vc.vertex(entry, v[j], v[j + 1], v[j + 2])
                                .color(255, 255, 255, 255)
                                .texture(v[j + 3], v[j + 4])
                                .overlay(OverlayTexture.DEFAULT_UV)
                                .light(light)
                                .normal(entry, v[j + 5], v[j + 6], v[j + 7]);
                    }
                }
            });
        }
        matrices.pop();

        if (state.showCross) {
            final int fx = state.faceX;
            final int fz = state.faceZ;
            final float cy = state.crossY + 0.5f;
            final float off = state.crossOffset;
            matrices.push();
            matrices.translate(0.5, 0.0, 0.5);
            queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(CROSS), (entry, vc) -> {
                float o = off, h = 0.18f;
                float[][] c = new float[4][3];
                float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
                for (int i = 0; i < 4; i++) {
                    float a = (i == 0 || i == 3) ? -h : h;
                    float y = (i < 2) ? cy - h : cy + h;
                    if (fx != 0) {
                        c[i] = new float[]{o * fx, y, a};
                    } else {
                        c[i] = new float[]{a, y, o * fz};
                    }
                }
                for (int i = 0; i < 4; i++) {
                    vc.vertex(entry, c[i][0], c[i][1], c[i][2])
                            .color(255, 255, 255, 255)
                            .texture(uv[i][0], uv[i][1])
                            .overlay(OverlayTexture.DEFAULT_UV)
                            .light(FULL_LIGHT)
                            .normal(entry, fx, 0f, fz);
                }
            });
            matrices.pop();
        }
    }
}
