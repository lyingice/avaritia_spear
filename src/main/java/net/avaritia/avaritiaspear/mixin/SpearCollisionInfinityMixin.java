package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import java.util.List;
import java.util.function.Predicate;

@Mixin(SpearItem.class)
public class SpearCollisionInfinityMixin {

    @Inject(method = "getHitEntitiesAlong(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/spearcore/item/SpearItem;FLjava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void onGetHitEntities(LivingEntity attacker, SpearItem spear, float hitboxMargin,
                                         Predicate<Entity> predicate, CallbackInfoReturnable<List<EntityHitResult>> cir) {
        if (!(spear instanceof InfinitySpearItem)) return;
        if (!(attacker instanceof Player player)) return;

        List<EntityHitResult> hits = cir.getReturnValue();
        for (EntityHitResult hit : hits) {
            Entity target = hit.getEntity();
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
}