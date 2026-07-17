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

@Mixin(value = CosmicBakeModel.class, remap = false)
public abstract class CosmicBakeModelDisplayMixin extends WrappedItemModel {
    private CosmicBakeModelDisplayMixin(BakedModel wrapped) {
        super(wrapped);
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void avaritiaSpear$restoreJsonDisplay(ItemStack stack, ItemDisplayContext transformType,
                                                  PoseStack pStack, MultiBufferSource source,
                                                  int packedLight, int packedOverlay, CallbackInfo ci) {
        if (stack.getItem() instanceof InfinitySpearItem) {
            this.parentState = TransformUtils.stateFromItemTransforms(this.wrapped.getTransforms());
        }
    }
}
