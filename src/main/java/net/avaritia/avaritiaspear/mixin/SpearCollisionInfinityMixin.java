package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.avaritia.avaritiaspear.util.InfinitySpearDamage;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(SpearItem.class)
public class SpearCollisionInfinityMixin {

    @Inject(method = "getHitEntitiesAlong(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/spearcore/item/SpearItem;FLjava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"), remap = false)
    private static void onGetHitEntities(LivingEntity attacker, SpearItem spear, float hitboxMargin,
                                         Predicate<Entity> predicate, CallbackInfoReturnable<List<EntityHitResult>> cir) {
        if (!(spear instanceof InfinitySpearItem)) return;
        if (!(attacker instanceof Player player)) return;
        // 该回调在蓄力冲锋的每一 tick 也会走到；只在服务端结算，避免客户端改血量导致不同步。
        if (player.level().isClientSide()) return;

        List<EntityHitResult> hits = cir.getReturnValue();
        if (hits == null || hits.isEmpty()) return;
        for (EntityHitResult hit : hits) {
            InfinitySpearDamage.kill(player, hit.getEntity());
        }
    }
}
