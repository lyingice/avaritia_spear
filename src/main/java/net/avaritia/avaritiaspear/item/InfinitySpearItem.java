package net.avaritia.avaritiaspear.item;

import committee.nova.mods.avaritia.api.iface.item.IUndamageable;
import committee.nova.mods.avaritia.api.iface.item.InitEnchantItem;
import committee.nova.mods.avaritia.common.entity.ImmortalItemEntity;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModEntities;
import committee.nova.mods.avaritia.init.registry.ModRarities;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.spearcore.init.SpearSounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.spearcore.util.SpearCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import committee.nova.mods.avaritia.api.common.enchant.InitEnchantment;

import java.util.List;
import java.util.Optional;

public class InfinitySpearItem extends SpearItem implements IUndamageable, InitEnchantItem {

    private static final SoundEvent SPEAR_USE = SpearSounds.ITEM_SPEAR_USE.get();
    private static final SoundEvent SPEAR_HIT = SpearSounds.ITEM_SPEAR_HIT.get();
    private static final SoundEvent SPEAR_ATTACK = SpearSounds.ITEM_SPEAR_ATTACK.get();
    @Override public float getMinCreativeRange() { return 2.0f; }
    @Override public float getMaxCreativeRange() { return 6.5f; }
    @Override public float getMobFactor() { return 0.5f; }
    @Override public boolean dealsKnockback() { return true; }
    @Override public boolean dismounts() { return false; }


    public InfinitySpearItem() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(ModRarities.COSMIC.getValue())
                .durability(9999)
        );
    }
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                ModToolTiers.INFINITY.getAttackDamageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID,
                                1.0f / getAttackDuration() - 4.0f,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("spear_range"),
                                7.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override public float getAttackDuration() { return 0f; }
    @Override public float getDamageMultiplier() { return 300.f; }
    @Override public SoundEvent getUseSound() { return SPEAR_USE; }
    @Override public SoundEvent getHitSound() { return SPEAR_HIT; }
    @Override public SoundEvent getAttackSound() { return SPEAR_ATTACK; }

    // 举矛阶段不变
    @Override public int getDelayTicks() { return 8; }

    // 冲刺攻击阶段翻三倍（平举→击退→伤害）
    @Override public int getDismountEndTick() { return 8 + (50 * 3); }  // 158
    @Override public int getKnockbackEndTick() { return 8 + (70 * 3); } // 218
    @Override public int getDamageEndTick() { return 8 + (100 * 3); }   // 308

    @Override public Optional<SpearCondition> getDismountConditions() { return Optional.of(new SpearCondition(150, 5.1f, 0)); }
    @Override public Optional<SpearCondition> getKnockbackConditions() { return Optional.of(new SpearCondition(210, 5.1f, 0)); }
    @Override public Optional<SpearCondition> getDamageConditions() { return Optional.of(new SpearCondition(300, 0, 4.6f)); }

    @Override public float getForwardMovement() { return 0.38f; }
    @Override public float getMinRange() { return 2.0f; }
    @Override public float getMaxRange() { return 9.5f; }
    @Override public float getHitboxMargin() { return 0.25f; }
    @Override public float getHitboxMargin2() { return 0.125f; }
    @Override public int getContactCooldownTicks() { return 10; }
    @Override public float getSwingTimes() { return 1.15f; }

    @Override public boolean isDamageable(@NotNull ItemStack stack) { return false; }
    @Override public boolean isFoil(@NotNull ItemStack stack) { return false; }
    @Override public int getEnchantmentValue(@NotNull ItemStack stack) { return 0; }
    @Override public boolean isBarVisible(@NotNull ItemStack stack) { return false; }

    @Override
    public int getInitEnchantLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.LOOTING)) return 10;
        return 0;
    }

    @Override
    public boolean hasCustomEntity(@NotNull ItemStack stack) {
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(@NotNull Level level, Entity location, @NotNull ItemStack stack) {
        return ImmortalItemEntity.create(ModEntities.IMMORTAL.get(), level, location.getX(), location.getY(), location.getZ(), stack);
    }
    private final InitEnchantment initEnchantment = new InitEnchantment(Enchantments.LOOTING, 10);

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        this.initEnchantment.appendHoverText(context, tooltipComponents);
        tooltipComponents.add(Component.translatable("tooltip.avaritia_spear.infinity_spear_damage.desc").withStyle(ChatFormatting.RED));
        tooltipComponents.add(Component.translatable("tooltip.avaritia_spear.infinity_spear.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}