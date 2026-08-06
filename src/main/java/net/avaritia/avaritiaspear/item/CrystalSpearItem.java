package net.avaritia.avaritiaspear.item;

import committee.nova.mods.avaritia.api.common.enchant.InitEnchantment;
import committee.nova.mods.avaritia.api.iface.ITooltip;
import committee.nova.mods.avaritia.api.iface.item.ISwitchable;
import committee.nova.mods.avaritia.api.iface.item.InitEnchantItem;
import committee.nova.mods.avaritia.init.registry.ModDataComponents;
import committee.nova.mods.avaritia.init.registry.ModRarities;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import committee.nova.mods.avaritia.init.registry.modes.ToolMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.spearcore.init.SpearSounds;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.spearcore.util.SpearCondition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CrystalSpearItem extends SpearItem implements ITooltip, InitEnchantItem,ISwitchable {
    public CrystalSpearItem() {
        super(new Properties()   // ← 不传 Tier
                .rarity(ModRarities.EPIC)
                .stacksTo(1)
                .fireResistant()
                .durability(8888)
        );
    }
    private static final ResourceLocation LUNGE_ID = ResourceLocation.fromNamespaceAndPath("spearcore", "lunge");
    private static final String MODE_SHATTER = "crystal_shatter";
    /** 每点护甲值增加的伤害倍率 */
    private static final float ARMOR_BONUS = 0.25F;
    /** 每点盔甲韧性增加的伤害倍率 */
    private static final float TOUGHNESS_BONUS = 0.40F;
    /** 晶爆 AOE 半径（格） */
    private static final double SHATTER_RADIUS = 3.0D;
    /** 晶爆溅射倍率（相对主目标基础伤害） */
    private static final float SHATTER_SPLASH = 0.5F;
    /** 破盾冷却时长（tick） */
    private static final int SHIELD_BREAK_COOLDOWN = 1200;
    private final InitEnchantment lungeEnchant = new InitEnchantment(
            ResourceKey.create(Registries.ENCHANTMENT, LUNGE_ID), 3);
    @Override
    public int getInitEnchantLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(ResourceKey.create(Registries.ENCHANTMENT, LUNGE_ID)) ? 3 : 0;
    }
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        this.lungeEnchant.appendHoverText(context, tooltipComponents);
        if (isActive(stack, MODE_SHATTER)) {
            tooltipComponents.add(Component.translatable("tooltip.avaritia.crystal_shatter.active")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }
    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                ModToolTiers.CRYSTAL.getAttackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID,
                                ModToolTiers.CRYSTAL.getSpeed(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("spear_range"),
                                4.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
    // ==================== 模式切换：shift+右键 ====================
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(level, player, hand, MODE_SHATTER);
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }
    // ==================== Crystal 特性：护甲/韧性虚空伤害 + 晶爆 + 破盾 ====================
    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (attacker instanceof Player player && target.level() instanceof ServerLevel serverLevel && target.isAlive()) {
            // 破盾：强制停用 + 1200 tick 冷却 + 破盾动画（不再削减盾牌耐久）
            if (target instanceof ServerPlayer serverPlayer) {
                ItemStack using = serverPlayer.getUseItem();
                serverPlayer.stopUsingItem();
                if (!using.isEmpty()) {
                    serverPlayer.getCooldowns().addCooldown(using.getItem(), SHIELD_BREAK_COOLDOWN);
                }
                serverPlayer.level().broadcastEntityEvent(serverPlayer, (byte) 30);
            }

            // 基础伤害 = 武器攻击伤害（ATTACK_DAMAGE 属性）
            float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // 每点护甲 +25%、每点韧性 +40%，可叠加
            float multiplier = 1.0F + target.getArmorValue() * ARMOR_BONUS
                    + (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * TOUGHNESS_BONUS;

            DamageSource voidDamage = serverLevel.damageSources().fellOutOfWorld();

            // 重置无敌帧，防止虚空伤害丢失
            target.invulnerableTime = 0;
            target.hurt(voidDamage, baseDamage * multiplier);

            // 水晶粒子
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    8, 0.5, 0.5, 0.5, 0.1);

            // 晶爆模式：3 格 AOE 虚空溅射（50%），按每个目标自己的护甲/韧性加成
            if (isActive(stack, MODE_SHATTER)) {
                AABB aoeBox = target.getBoundingBox().inflate(SHATTER_RADIUS);
                List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, aoeBox,
                        e -> e != target && e != player && e.isAlive());
                for (LivingEntity e : nearby) {
                    float eMultiplier = 1.0F + e.getArmorValue() * ARMOR_BONUS
                            + (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * TOUGHNESS_BONUS;
                    e.invulnerableTime = 0;
                    e.hurt(voidDamage, baseDamage * SHATTER_SPLASH * eMultiplier);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
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
        return 0.5f;
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
