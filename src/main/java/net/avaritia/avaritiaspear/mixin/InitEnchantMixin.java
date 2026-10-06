package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.api.item.InitEnchantItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让"自带附魔"真的生效。
 * <p>
 * 1.21 的 Avaritia 自己有一套机制去读 {@code InitEnchantItem}，1.20.1 那份没有，
 * 所以这里直接挂在原版 {@link EnchantmentHelper#getItemEnchantmentLevel} 上：
 * 真实附魔等级为 0 时，改用物品自带的等级。
 * <p>
 * 这一个 hook 就覆盖了火焰附加（getFireAspect）与抢夺（getMobLooting）等所有走
 * EnchantmentHelper 的逻辑。
 */
@Mixin(EnchantmentHelper.class)
public class InitEnchantMixin {

    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void avaritia_spear$initEnchantLevel(Enchantment enchantment, ItemStack stack,
                                                        CallbackInfoReturnable<Integer> cir) {
        // 真实附魔优先，绝不覆盖
        if (cir.getReturnValue() > 0) return;
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof InitEnchantItem initItem)) return;

        int level = initItem.getInitEnchantLevel(stack, BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment));
        if (level > 0) {
            cir.setReturnValue(level);
        }
    }
}
