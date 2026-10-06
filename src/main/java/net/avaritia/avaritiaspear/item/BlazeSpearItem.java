package net.avaritia.avaritiaspear.item;

import net.avaritia.avaritiaspear.api.enchant.InitEnchantment;
import committee.nova.mods.avaritia.api.iface.ITooltip;
import net.avaritia.avaritiaspear.api.item.ISwitchable;
import net.avaritia.avaritiaspear.api.item.InitEnchantItem;
import committee.nova.mods.avaritia.init.registry.ModDamageTypes;
import committee.nova.mods.avaritia.init.registry.ModRarities;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.spearcore.init.SpearSounds;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.spearcore.util.SpearCondition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


import java.util.List;
import java.util.Optional;

public class BlazeSpearItem extends SpearItem implements ITooltip, InitEnchantItem , ISwitchable {
    public BlazeSpearItem() {
        super(new Properties()   // ← 不传 Tier
                .rarity(ModRarities.EPIC)
                .stacksTo(1)
                .fireResistant()
                .durability(7777)
        );
        this.initEnchantment = new InitEnchantment(Enchantments.FIRE_ASPECT, 10);
    }
    private static final String MODE_BLAST = "blaze_spear_blast";
    /** 计算"附近着火单位"的半径 */
    private static final int BURNING_COUNT_RANGE = 8;
    /** 炎爆爆炸半径，与苦力怕一致 */
    private static final float BLAST_RADIUS = 3.0F;
    /** 攻击距离加成的修饰符 ID：1.20.1 的属性修饰符用 UUID，不用 ResourceLocation */
    private static final java.util.UUID SPEAR_RANGE_ID =
            java.util.UUID.fromString("6b1f4d0e-2a3b-4c5d-8e9f-0a1b2c3d4e5f");

    /**
     * 1.21 是 {@code getDefaultAttributeModifiers(ItemStack)} + {@code ItemAttributeModifiers} builder；
     * 1.20.1 是 {@code getDefaultAttributeModifiers(EquipmentSlot)} + {@code Multimap}，
     * 且 {@code Attributes.ENTITY_INTERACTION_RANGE} 在这里叫 {@code ForgeMod.ENTITY_REACH}。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        // 1.20.1 的 super.getDefaultAttributeModifiers 返回的是 ImmutableMultimap，
        // 直接 put 会抛 UnsupportedOperationException —— 打开创造栏构建搜索树时会走到这里（会崩）。
        // 必须自己建一个 builder。
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "Weapon modifier", ModToolTiers.BLAZE.getAttackDamageBonus(),
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Weapon modifier", ModToolTiers.BLAZE.getSpeed(),
                    AttributeModifier.Operation.ADDITION));
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(SPEAR_RANGE_ID,
                    "Spear range", 2.5, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }
    // ==================== 模式切换：shift+右键 ====================
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(world, player, hand, MODE_BLAST);
            return InteractionResultHolder.success(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // ==================== 攻击结算：着火数量加成 + 点燃 + 炎爆 ====================
    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker instanceof Player player && !target.level().isClientSide && target.isAlive()) {
            // 进入结算时目标是否已着火（必须最先记录，不能被本方法的伤害/点燃影响）
            boolean wasBurning = target.getRemainingFireTicks() > 0;

            // 基础伤害 = 武器攻击伤害（ATTACK_DAMAGE 属性）
            float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // 附近着火单位 N 个 → 本次伤害 × (1 + 50%×N)
            int burningCount = countBurningNearby(player);
            float finalDamage = baseDamage * (1.0F + 0.5F * burningCount);

            DamageSource source = ModDamageTypes.causeRandomDamage(player.level(), player);
            target.invulnerableTime = 0; // 重置无敌帧，防止伤害丢失
            target.hurt(source, finalDamage);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SpearSounds.ITEM_SPEAR_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.2F);

            // 火焰粒子
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        10, target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.05);
            }

            // 烈焰迸发：点燃主目标 15 秒（300 tick），周围 3 格敌人 10 秒（200 tick）
            igniteTargetAndNearby(target, player);

            // 炎爆模式：只有命中前就已着火的生物，才会在其位置爆炸
            if (isActive(stack, MODE_BLAST) && wasBurning && target.level() instanceof ServerLevel serverLevel) {
                blast(serverLevel, player, target, finalDamage, burningCount);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** 统计玩家周围 BURNING_COUNT_RANGE 格内着火的存活单位数量 */
    private static int countBurningNearby(Player player) {
        AABB box = player.getBoundingBox().inflate(BURNING_COUNT_RANGE);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.getRemainingFireTicks() > 0).size();
    }
    /** 点燃主目标 15 秒（300 tick），周围 3 格敌人 10 秒（200 tick） */
    private static void igniteTargetAndNearby(LivingEntity target, LivingEntity attacker) {
        target.setRemainingFireTicks(300);
        AABB aoeBox = target.getBoundingBox().inflate(3.0);
        List<LivingEntity> nearby = target.level().getEntitiesOfClass(
                LivingEntity.class, aoeBox,
                e -> e != target && e != attacker && e.isAlive());
        for (LivingEntity e : nearby) {
            e.setRemainingFireTicks(200);
        }
    }

    /** 在目标位置产生爆炸：伤害=本次攻击伤害，范围随附近着火数增长并封顶（苦力怕基准 3.0） */
    private static void blast(ServerLevel serverLevel, Player player, LivingEntity target,
                              float finalDamage, int burningCount) {
        float explosionRadius = Math.min(6.0F, BLAST_RADIUS * (1.0F + 0.15F * burningCount));
        Vec3 pos = target.position();
        DamageSource source = ModDamageTypes.causeRandomDamage(serverLevel, player);

        // 1.20.1 的 ExplosionDamageCalculator 没有 getEntityDamageAmount（那是 1.20.2+ 的 API），
        // 没法像 1.21 那样用自定义计算器把爆炸对实体的伤害钉死成"本次攻击伤害"。
        // 这里改成等价的自己结算：范围内的实体按距离衰减吃 finalDamage，主目标全额。
        // 副作用（已记录）：不再像原版那样破坏地形/给爆炸击退。
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.5, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);

        AABB area = new AABB(pos, pos).inflate(explosionRadius);
        for (Entity entity : serverLevel.getEntities(player, area)) {
            if (entity == player) continue;
            double distance = entity.position().distanceTo(pos);
            float damage = distance <= 0.5D
                    ? finalDamage
                    : (float) (finalDamage * Math.max(0.0D, 1.0D - distance / explosionRadius));
            if (damage > 0.0F) {
                entity.hurt(source, damage);
            }
        }
    }
    public int getInitEnchantLevel(ItemStack stack, Holder<Enchantment> enchantmentHolder) {
        return enchantmentHolder.value() == Enchantments.FIRE_ASPECT ? 10 : 0;
    }
    private final InitEnchantment initEnchantment;
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        this.initEnchantment.appendHoverText(level, tooltipComponents);
        if (isActive(stack, MODE_BLAST)) {
            tooltipComponents.add(Component.translatable("tooltip.avaritia.blaze_spear_blast.active")
                    .withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
    public boolean hasDescTooltip() {
        return true;
    }

    public boolean isFoil(@NotNull ItemStack pStack) {
        return false;
    }

    @Override
    public float getAttackDuration() {
        return 54f;
    }

    @Override
    public float getDamageMultiplier() {
        return 10.f;
    }

    @Override
    public SoundEvent getUseSound() {
        return SpearSounds.ITEM_SPEAR_USE.get();
    }

    @Override
    public SoundEvent getHitSound() {
        return SpearSounds.ITEM_SPEAR_HIT.get();
    }

    @Override
    public SoundEvent getAttackSound() {
        return SpearSounds.ITEM_SPEAR_ATTACK.get();
    }

    // ==================== 冲刺三阶段时长 ====================
    @Override
    public int getDelayTicks() {
        return 8;
    }

    @Override
    public int getDismountEndTick() {
        return 8 + (16 * 3);  // 56
    }

    @Override
    public int getKnockbackEndTick() {
        return 8 + (24 * 3);  // 80
    }

    @Override
    public int getDamageEndTick() {
        return 8 + (34 * 3);  // 110
    }

    // ==================== 冲刺三阶段判定 ====================
    @Override
    public Optional<SpearCondition> getDismountConditions() {
        return Optional.of(new SpearCondition(48, 1.7f, 0));
    }

    @Override
    public Optional<SpearCondition> getKnockbackConditions() {
        return Optional.of(new SpearCondition(72, 1.7f, 0));
    }

    @Override
    public Optional<SpearCondition> getDamageConditions() {
        return Optional.of(new SpearCondition(102, 0, 1.6f));
    }

    // ==================== 射程与判定框 ====================
    @Override
    public float getForwardMovement() {
        return 0.12f;
    }

    @Override
    public float getMinRange() {
        return 2.0f;
    }

    @Override
    public float getMaxRange() {
        return 6.5f;
    }

    @Override
    public float getHitboxMargin() {
        return 0.1f;
    }

    @Override
    public float getHitboxMargin2() {
        return 0.05f;
    }

    @Override
    public int getContactCooldownTicks() {
        return 3;
    }

    @Override
    public float getSwingTimes() {
        return 0.6f;
    }

    // ==================== 创造模式 ====================
    @Override
    public float getMinCreativeRange() {
        return 2.0f;
    }

    @Override
    public float getMaxCreativeRange() {
        return 6.5f;
    }

    @Override
    public float getMobFactor() {
        return 0.5f;
    }

    @Override
    public boolean dealsKnockback() {
        return true;
    }

    @Override
    public boolean dismounts() {
        return false;
    }
}
