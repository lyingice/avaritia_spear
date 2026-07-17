package net.avaritia.avaritiaspear.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import committee.nova.mods.avaritia.client.shader.AvaritiaRenderTypes;
import committee.nova.mods.avaritia.client.shader.AvaritiaShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CosmicSpBakedModel implements BakedModel {
    private static final Logger LOGGER = LogManager.getLogger("AvaritiaSpearCosmic");
    private static boolean loggedCosmicState;
    private final BakedModel baseModel;
    private final BakedModel maskModel;

    public CosmicSpBakedModel(BakedModel baseModel, BakedModel maskModel) {
        this.baseModel = baseModel;
        this.maskModel = maskModel;
    }

    public BakedModel maskModel() {
        return maskModel;
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        return baseModel.getRenderPasses(itemStack, fabulous);
    }

    public void renderCosmicLayer(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float yaw = 0.0F;
        float pitch = 0.0F;
        float scale = 1.0F;
        if (transformType == ItemDisplayContext.GUI) {
            scale = 100.0F;
        } else if (mc.player != null) {
            yaw = (float) (mc.player.getYRot() * 2.0F * Math.PI / 360.0F);
            pitch = -(float) (mc.player.getXRot() * 2.0F * Math.PI / 360.0F);
        }

        if (AvaritiaShaders.cosmicTime != null) {
            AvaritiaShaders.cosmicTime.set(mc.level.getGameTime() % Integer.MAX_VALUE);
        }
        if (AvaritiaShaders.cosmicYaw != null) {
            AvaritiaShaders.cosmicYaw.set(yaw);
        }
        if (AvaritiaShaders.cosmicPitch != null) {
            AvaritiaShaders.cosmicPitch.set(pitch);
        }
        if (AvaritiaShaders.cosmicExternalScale != null) {
            AvaritiaShaders.cosmicExternalScale.set(scale);
        }
        if (AvaritiaShaders.cosmicOpacity != null) {
            AvaritiaShaders.cosmicOpacity.set(1.0F);
        }
        if (AvaritiaShaders.cosmicUVs != null) {
            AvaritiaShaders.cosmicUVs.set(AvaritiaShaders.COSMIC_UVS);
        }

        VertexConsumer consumer = bufferSource.getBuffer(AvaritiaRenderTypes.COSMIC);
        TextureAtlasSprite maskSprite = getMaskSprite(maskModel, mc.level.random);
        for (BakedModel pass : baseModel.getRenderPasses(stack, true)) {
            List<BakedQuad> quads = getCosmicMaskQuads(pass, maskSprite, mc.level.random);
            if (!loggedCosmicState) {
                loggedCosmicState = true;
                LOGGER.info("Infinity spear cosmic remap: basePass={}, maskSprite={}, quads={}, shader={}, opacityUniform={}, uvUniform={}",
                        pass.getClass().getName(),
                        maskSprite.contents().name(),
                        quads.size(),
                        AvaritiaShaders.COSMIC_SHADER != null,
                        AvaritiaShaders.cosmicOpacity != null,
                        AvaritiaShaders.cosmicUVs != null);
            }
            mc.getItemRenderer().renderQuadList(
                    poseStack,
                    consumer,
                    quads,
                    stack,
                    packedLight,
                    packedOverlay
            );
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource buffer) {
            buffer.endBatch(AvaritiaRenderTypes.COSMIC);
        }
    }

    private static TextureAtlasSprite getMaskSprite(BakedModel model, RandomSource random) {
        for (BakedModel pass : model.getRenderPasses(ItemStack.EMPTY, true)) {
            for (Direction direction : Direction.values()) {
                List<BakedQuad> quads = pass.getQuads(null, direction, random);
                if (!quads.isEmpty()) {
                    return quads.getFirst().getSprite();
                }
            }
            List<BakedQuad> quads = pass.getQuads(null, null, random);
            if (!quads.isEmpty()) {
                return quads.getFirst().getSprite();
            }
        }
        return model.getParticleIcon();
    }

    private static List<BakedQuad> getCosmicMaskQuads(BakedModel model, TextureAtlasSprite maskSprite, RandomSource random) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            addRemappedQuads(quads, model, direction, maskSprite, random);
        }
        addRemappedQuads(quads, model, null, maskSprite, random);
        return quads;
    }

    private static void addRemappedQuads(List<BakedQuad> quads, BakedModel model, @Nullable Direction direction,
                                         TextureAtlasSprite maskSprite, RandomSource random) {
        for (BakedQuad quad : model.getQuads(null, direction, random)) {
            quads.add(remapQuadSprite(quad, maskSprite));
        }
    }

    private static BakedQuad remapQuadSprite(BakedQuad quad, TextureAtlasSprite maskSprite) {
        TextureAtlasSprite sourceSprite = quad.getSprite();
        int[] vertices = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
        int stride = vertices.length / 4;
        Direction direction = quad.getDirection();
        float normalX = direction.getStepX() * 0.001F;
        float normalY = direction.getStepY() * 0.001F;
        float normalZ = direction.getStepZ() * 0.001F;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            vertices[offset] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset]) + normalX);
            vertices[offset + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset + 1]) + normalY);
            vertices[offset + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset + 2]) + normalZ);
            float sourceU = Float.intBitsToFloat(vertices[offset + 4]);
            float sourceV = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + 4] = Float.floatToRawIntBits(maskSprite.getU(sourceSprite.getUOffset(sourceU)));
            vertices[offset + 5] = Float.floatToRawIntBits(maskSprite.getV(sourceSprite.getVOffset(sourceV)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), maskSprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource randomSource) {
        return baseModel.getQuads(state, direction, randomSource);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return baseModel.isCustomRenderer();
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
