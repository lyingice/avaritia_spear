package net.avaritia.avaritiaspear.mixin;

import committee.nova.mods.avaritia.client.compat.IrisCompat;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iris 光影兼容：
 * Avaritia 开光影后会把一手/三手星空层 defer 到 AFTER_LEVEL，
 * 导致手持 JSON display 与即时 cosmic 路径不一致。
 * 这里强制关闭 defer，让光影下手持也走即时渲染，再配合
 * CosmicBakeModelDisplayMixin 恢复 JSON display。
 */
@Mixin(value = IrisCompat.class, remap = false)
public class IrisCompatMixin {
    @Inject(method = "shouldDefer", at = @At("HEAD"), cancellable = true)
    private static void avaritiaSpear$keepImmediateCosmicWithDisplay(
            ItemDisplayContext ctx,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // 手持视角下禁用 Iris 延迟队列，避免 display 变换失效
        switch (ctx) {
            case FIRST_PERSON_LEFT_HAND,
                 FIRST_PERSON_RIGHT_HAND,
                 THIRD_PERSON_LEFT_HAND,
                 THIRD_PERSON_RIGHT_HAND -> cir.setReturnValue(false);
            default -> {
            }
        }
    }
}