package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.api.item.InitEnchantItem;
import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.spearcore.init.SpearSounds;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpearItem.class, remap = false)
public class SpearItemLungeMixin {

    private static final ResourceKey<Enchantment> LUNGE =
            ResourceKey.create(Registries.ENCHANTMENT,
                    new ResourceLocation("spearcore", "lunge"));

    @Inject(method = "jerotesLungeForwardMaybe", at = @At("HEAD"), cancellable = true)
    private static void checkInitEnchantLunge(LivingEntity user, CallbackInfo ci) {
        ItemStack stack = user.getMainHandItem();
        if (!(stack.getItem() instanceof SpearItem)) return;
        if (!(stack.getItem() instanceof InitEnchantItem initItem)) return;

        // 真实附魔已存在 → 原生流程不受影响。
        // 注意：这里必须读物品 NBT 上的"真实"附魔，不能用 EnchantmentHelper.getItemEnchantmentLevel，
        // 因为那个方法已经被 InitEnchantMixin 挂过钩子、会把"自带附魔"也算进去，否则永远提前 return。
        Holder<Enchantment> holder = user.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(LUNGE);
        if (EnchantmentHelper.getEnchantments(stack).getOrDefault(holder.value(), 0) > 0) return;

        int level = initItem.getInitEnchantLevel(stack, holder);
        if (level <= 0) return;

        // 无尽矛突进不消耗饥饿值（其余矛保持原生消耗）
        boolean freeHunger = stack.getItem() instanceof InfinitySpearItem;

        if (stack.isDamageableItem() && user instanceof Player player) {
            // 1.20.1 的 hurtAndBreak 是 (int, LivingEntity, Consumer)
            stack.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        if (!freeHunger && user instanceof Player player) {
            player.causeFoodExhaustion(4.0f * level);
        }

        SpearItem.rushAttack(user, 0.65f * level);

        SoundEvent sound = SpearSounds.ITEM_SPEAR_LUNGE_1.get();
        if (level > 1) sound = SpearSounds.ITEM_SPEAR_LUNGE_2.get();
        if (level > 2) sound = SpearSounds.ITEM_SPEAR_LUNGE_3.get();
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                sound, user.getSoundSource(), 1.0F, 1.0F);

        ci.cancel(); // 跳过原生流程（原生会因 enchantLevel=0 而 return）
    }
}