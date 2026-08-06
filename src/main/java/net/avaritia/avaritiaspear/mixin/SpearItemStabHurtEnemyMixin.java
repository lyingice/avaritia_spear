package net.avaritia.avaritiaspear.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpearItem.class, remap = false)
public class SpearItemStabHurtEnemyMixin {

    @WrapOperation(
            method = "performStabAttack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private static boolean avaritiaSpear$stabHurtEnemy(Entity target, DamageSource source, float amount,
                                                       Operation<Boolean> original) {
        boolean result = original.call(target, source, amount);
        // 左键 stab 路径原生不调用 hurtEnemy，这里补上（与蓄力路径 stabAttack 的语义一致）
        if (target instanceof LivingEntity livingTarget && source.getDirectEntity() instanceof Player player) {
            player.getMainHandItem().hurtEnemy(livingTarget, player);
        }
        return result;
    }
}