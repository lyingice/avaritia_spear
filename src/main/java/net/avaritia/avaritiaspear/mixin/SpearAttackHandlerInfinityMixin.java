package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.spearcore.event.SpearAttackHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
@Mixin(SpearAttackHandler.class)
public class SpearAttackHandlerInfinityMixin {

    @Inject(method = "onPlayerAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onPlayerAttackInfinity(AttackEntityEvent event, CallbackInfo ci) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof InfinitySpearItem)) return;

        event.setCanceled(true);
        Entity target = event.getTarget();
        if (target instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, player.damageSources().mobAttack(player), ModConfig.isSwordAttackEndless.get() ? Float.MAX_VALUE : ModToolTiers.INFINITY.getAttackDamageBonus());
            if (!dragon.isDeadOrDying()) dragon.setHealth(0);
        } else if (target instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            parent.hurt(player.damageSources().mobAttack(player), ModConfig.isSwordAttackEndless.get() ? Float.MAX_VALUE : ModToolTiers.INFINITY.getAttackDamageBonus());
            if (parent instanceof LivingEntity lp && !lp.isDeadOrDying()) {
                lp.setHealth(0);
            }
        } else if (target instanceof LivingEntity victim) {
            victim.hurt(player.damageSources().mobAttack(player), ModConfig.isSwordAttackEndless.get() ? Float.MAX_VALUE : ModToolTiers.INFINITY.getAttackDamageBonus());
            if (!victim.isDeadOrDying()) victim.setHealth(0);
        }
    }
}