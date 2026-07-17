package net.avaritia.avaritiaspear.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.avaritia.avaritiaspear.client.AvaritiaSpearClient;
import net.avaritia.avaritiaspear.client.model.CosmicSpBakedModel;
import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无尽矛星空渲染 Mixin：手持时替换基础模型，并用 cosmic_sp loader 烘焙出的 mask 模型叠加 Avaritia cosmic shader。
 */
@Mixin(ItemRenderer.class)
public class ItemRendererCosmicMixin {
    private static boolean renderingSpearInHandModel;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void useInHandModel(ItemStack stack, ItemDisplayContext transformType, boolean leftHand,
                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                               int packedOverlay, BakedModel model, CallbackInfo ci) {
        if (renderingSpearInHandModel || !(stack.getItem() instanceof InfinitySpearItem)) return;
        if (transformType == ItemDisplayContext.GUI
                || transformType == ItemDisplayContext.GROUND
                || transformType == ItemDisplayContext.FIXED) return;

        BakedModel inHandModel = Minecraft.getInstance()
                .getModelManager()
                .getModel(AvaritiaSpearClient.SPEAR_IN_HAND_MODEL);

        renderingSpearInHandModel = true;
        try {
            ((ItemRenderer) (Object) this).render(stack, transformType, leftHand, poseStack, bufferSource,
                    packedLight, packedOverlay, inHandModel);
            if (inHandModel instanceof CosmicSpBakedModel cosmicModel) {
                poseStack.pushPose();
                inHandModel.applyTransform(transformType, poseStack, leftHand);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                cosmicModel.renderCosmicLayer(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
        } finally {
            renderingSpearInHandModel = false;
        }
        ci.cancel();
    }
}
