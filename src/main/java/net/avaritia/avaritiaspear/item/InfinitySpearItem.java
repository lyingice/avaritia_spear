package net.avaritia.avaritiaspear.item;

import net.avaritia.avaritiaspear.api.item.ISwitchable;
import net.avaritia.avaritiaspear.api.item.IUndamageable;
import net.avaritia.avaritiaspear.api.item.InitEnchantItem;
import committee.nova.mods.avaritia.common.entity.ImmortalItemEntity;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModEntities;
import committee.nova.mods.avaritia.init.registry.ModRarities;
import committee.nova.mods.avaritia.init.registry.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.spearcore.init.SpearSounds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.spearcore.item.SpearItem;
import net.minecraft.spearcore.util.SpearCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.avaritia.avaritiaspear.api.enchant.InitEnchantment;

import java.util.List;
import java.util.Optional;

public class InfinitySpearItem extends SpearItem implements IUndamageable, InitEnchantItem , ISwitchable {
    private static final String MODE_LUNGE = "lunge";
    private static final ResourceKey<Enchantment> LUNGE_KEY =
            ResourceKey.create(Registries.ENCHANTMENT,
                    new ResourceLocation("spearcore", "lunge"));

    private final InitEnchantment lootEnchant = new InitEnchantment(Enchantments.MOB_LOOTING, 10);
    private final InitEnchantment lungeEnchant = new InitEnchantment(LUNGE_KEY, 10);
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
                .rarity(ModRarities.COSMIC)
                .durability(9999)
        );
    }
    /** 攻击距离加成的修饰符 ID：1.20.1 用 UUID */
    private static final java.util.UUID SPEAR_RANGE_ID =
            java.util.UUID.fromString("6b1f4d0e-2a3b-4c5d-8e9f-0a1b2c3d4e5f");

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        // 1.20.1 的 super.getDefaultAttributeModifiers 返回的是 ImmutableMultimap，
        // 直接 put 会抛 UnsupportedOperationException —— 打开创造栏构建搜索树时会走到这里（会崩）。
        // 必须自己建一个 builder。
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "Weapon modifier", ModToolTiers.INFINITY.getAttackDamageBonus(),
                    AttributeModifier.Operation.ADDITION));
            // 注意：getAttackDuration() 被本类覆写为 0f（无冷却），所以这里算出来是 Infinity。
            // 这是 1.21 原版就有的写法，保持原样；1.20.1 下表现为攻速加成趋近无穷（几乎无攻击间隔）。
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Weapon modifier", 1.0f / getAttackDuration() - 4.0f,
                    AttributeModifier.Operation.ADDITION));
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(SPEAR_RANGE_ID,
                    "Spear range", 7.0, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
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
    @Override public float getSwingTimes() { return 0.3f; }
    @Override public boolean isDamageable(@NotNull ItemStack stack) { return false; }
    @Override public boolean isFoil(@NotNull ItemStack stack) { return false; }
    @Override public int getEnchantmentValue(@NotNull ItemStack stack) { return 0; }
    @Override public boolean isBarVisible(@NotNull ItemStack stack) { return false; }

    // ==================== 切换模式 ====================
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            switchMode(world, player, hand, MODE_LUNGE);
            return InteractionResultHolder.success(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // ==================== 附魔：Looting 10 常驻，Lunge 10 按模式 ====================
    @Override
    public int getInitEnchantLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.value() == Enchantments.MOB_LOOTING) return 10;
        if (enchantment.is(LUNGE_KEY) && isActive(stack, MODE_LUNGE)) return 10;
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
    private final InitEnchantment initEnchantment = new InitEnchantment(Enchantments.MOB_LOOTING, 10);

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Level level,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        this.lootEnchant.appendHoverText(level, tooltipComponents);
        if (isActive(stack, MODE_LUNGE)) {
            this.lungeEnchant.appendHoverText(level, tooltipComponents);
        }
        tooltipComponents.add(Component.translatable("tooltip.avaritia_spear.infinity_spear_damage.desc")
                .withStyle(ChatFormatting.RED));
        tooltipComponents.add(Component.translatable("tooltip.avaritia_spear.infinity_spear.desc")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}