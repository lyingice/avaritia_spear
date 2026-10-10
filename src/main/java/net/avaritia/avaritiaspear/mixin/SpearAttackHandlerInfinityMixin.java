package net.avaritia.avaritiaspear.mixin;

import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.avaritia.avaritiaspear.util.InfinitySpearDamage;
import net.minecraft.spearcore.event.SpearAttackHandler;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpearAttackHandler.class)
public class SpearAttackHandlerInfinityMixin {

    @Inject(method = "onPlayerAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onPlayerAttackInfinity(AttackEntityEvent event, CallbackInfo ci) {
        Player player = event.getEntity();
        // 只在服务端结算：客户端也会触发 AttackEntityEvent，若在此改血量会造成客户端/服务端不同步。
        if (player.level().isClientSide()) return;
        if (!(player.getMainHandItem().getItem() instanceof InfinitySpearItem)) return;

        event.setCanceled(true);
        InfinitySpearDamage.kill(player, event.getTarget());
        // 不取消原方法：其后的 performStabAttack 负责突进、挥矛动画与音效，需要保留。
        // 目标此时已死，会被 getHitEntitiesAlong 的 isAlive() 过滤掉，不会重复命中。
    }
}
