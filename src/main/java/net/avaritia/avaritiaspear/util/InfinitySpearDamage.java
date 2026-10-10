package net.avaritia.avaritiaspear.util;

import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModDamageTypes;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import committee.nova.mods.avaritia.util.ToolUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 无尽矛秒杀的统一收口。
 *
 * <p>修复了两类问题：
 * <ul>
 *   <li>旧实现用 {@code mobAttack} 这类普通伤害类型打秒杀，会被 Avaritia 的无尽甲/无尽事件取消，
 *       随后又对目标 {@code setHealth(0)} —— 只把血量置零却不触发 {@code die()}，目标就停在
 *       “0 血但没死”的假死状态（玩家同理：死亡界面不出现、需重登）。</li>
 *   <li>旧的两个 mixin 没有排除自身/坐骑/友军，也缺少客户端判断，可能把矛的持有者或队友当成目标。</li>
 * </ul>
 *
 * <p>现在统一改用 {@code avaritia:infinity} 伤害类型：它 bypass 无敌帧/护甲/抗性/冷却，
 * 并被 Avaritia 自身的事件处理器豁免，因此 {@code hurt()} 必定结算并自然触发 {@code die()}；
 * 万一仍留下 0 血状态，再补一次真正的 {@code die()}。
 */
public final class InfinitySpearDamage {

    private InfinitySpearDamage() {
    }

    /**
     * 对目标施加无尽秒杀。仅在服务端生效，且绝不会以持有者本人、坐骑、友军或自己的驯服宠物为目标。
     *
     * @param player 矛的持有者
     * @param entity 候选目标，可以为 null
     */
    public static void kill(Player player, @Nullable Entity entity) {
        if (entity == null) return;
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity victim = resolveLivingTarget(entity);
        if (victim == null || !isValidVictim(player, victim)) return;

        // 穿全套无尽甲的玩家：不秒杀，只产生爆炸（与官方无尽剑/无尽矛一致）。
        if (victim instanceof Player pvp && ToolUtils.isInfinite(pvp)) {
            serverLevel.explode(player, pvp.getBlockX(), pvp.getBlockY(), pvp.getBlockZ(),
                    25.0F, Level.ExplosionInteraction.MOB);
            return;
        }

        DamageSource source = ModDamageTypes.causeRandomDamage(player);
        float damage = ModConfig.isSwordAttackEndless.get()
                ? Float.MAX_VALUE
                : ModToolTiers.INFINITY.getAttackDamageBonus();

        victim.invulnerableTime = 0;
        if (victim instanceof WitherBoss wither) {
            wither.setInvulnerableTicks(0);
        }

        if (victim instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, source, damage);
            return;
        }

        victim.hurt(source, damage);

        // 伤害被拦截（mod 无敌 / 死亡流程被取消）时只会留下“0 血但没死”的假死，
        // 这里补一次真正的死亡流程；不再对仍有血的目标 setHealth(0)，那正是旧实现的假死根源。
        if (!victim.isRemoved() && victim.isDeadOrDying()) {
            victim.die(source);
        }
    }

    private static boolean isValidVictim(Player player, LivingEntity victim) {
        if (victim == player) return false;
        if (victim.isSpectator()) return false;
        if (!victim.isAlive()) return false;
        if (victim == player.getVehicle()) return false;
        if (player.isPassengerOfSameVehicle(victim)) return false;
        if (player.isAlliedTo(victim)) return false;
        if (victim instanceof Player other && !player.canHarmPlayer(other)) return false;
        if (victim instanceof TamableAnimal tamable && tamable.isTame()
                && player.getUUID().equals(tamable.getOwnerUUID())) return false;
        if (victim instanceof ArmorStand stand && stand.isMarker()) return false;
        return true;
    }

    @Nullable
    private static LivingEntity resolveLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
