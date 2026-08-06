package net.avaritia.avaritiaspear.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.avaritia.api.client.model.bakedmodels.WrappedItemModel;
import committee.nova.mods.avaritia.api.client.util.TransformUtils;
import committee.nova.mods.avaritia.client.model.loader.CosmicBakeModel;
import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 恢复 JSON display：
 * CosmicBakeModel.renderItem 会把 parentState 强行改成 DEFAULT_ITEM/TOOL/BOW，
 * 导致 infinity_spear_in_hand.json 的 display 失效。
 * 在真正渲染前强制写回 wrapped 模型的 JSON transforms。
 * 配合 IrisCompatMixin 后，开光影/关光影都走同一条即时 cosmic 路径。
 */
@Mixin(value = CosmicBakeModel.class, remap = false)
public abstract class CosmicBakeModelDisplayMixin extends WrappedItemModel {
    private CosmicBakeModelDisplayMixin(BakedModel wrapped) {
        super(wrapped);
    }

    @Inject(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcommittee/nova/mods/avaritia/client/model/loader/CosmicBakeModel;renderWrapped(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void avaritiaSpear$applyJsonDisplayBeforeRender(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack pStack,
            MultiBufferSource source,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        if (stack.getItem() instanceof InfinitySpearItem) {
            this.parentState = TransformUtils.stateFromItemTransforms(this.wrapped.getTransforms());
        }
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void avaritiaSpear$keepJsonDisplayAfterRender(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack pStack,
            MultiBufferSource source,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        if (stack.getItem() instanceof InfinitySpearItem) {
            this.parentState = TransformUtils.stateFromItemTransforms(this.wrapped.getTransforms());
        }
    }
}