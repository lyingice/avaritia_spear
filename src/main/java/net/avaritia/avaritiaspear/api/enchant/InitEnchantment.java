package net.avaritia.avaritiaspear.api.enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 一条"自带附魔"（附魔 + 等级），负责在物品说明里显示它。
 * <p>
 * 对应 Avaritia 1.21 的 {@code api.common.enchant.InitEnchantment}。
 * 原版靠 {@code Item.TooltipContext} 拿注册表，1.20.1 没有这个类，改成直接传 {@link Level}。
 */
public class InitEnchantment {

    private final ResourceKey<Enchantment> enchantment;
    private final int level;

    public InitEnchantment(ResourceKey<Enchantment> enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    /**
     * 1.20.1 的 {@code Enchantments.FIRE_ASPECT} 这类常量是 {@link Enchantment} 本体，
     * 而 1.21 是 {@link ResourceKey}，所以这里多给一个重载，让调用点两种写法都能用。
     */
    public InitEnchantment(Enchantment enchantment, int level) {
        this(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getResourceKey(enchantment)
                .orElseThrow(() -> new IllegalArgumentException("未注册的附魔: " + enchantment)), level);
    }

    public ResourceKey<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public int getFixedLevel() {
        return this.level;
    }

    /** 在物品说明里加一行"自带附魔：<附魔名> <等级>"。 */
    public void appendHoverText(@Nullable Level level, List<Component> tooltipComponents) {
        if (level == null) return;
        level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(this.enchantment)
                .ifPresent(holder -> tooltipComponents.add(
                        // 用 Avaritia 自己的键（zh_cn 是"自带 %s"，en_us 是 "Always has at least %s"），
                        // 这样措辞和无尽贪婪重生一致，也不用在本模组里重复维护一份文案。
                        // 1.20.1 的 getFullname 是实例方法（1.21 才是静态 + Holder 参数）。
                        Component.translatable("tooltip.avaritia.init_enchant",
                                holder.value().getFullname(this.level))));
    }

    /** 与真实附魔比较时用：是本条附魔就给等级，否则 0。 */
    public int getLevel(Holder<Enchantment> enchantment) {
        return enchantment.is(this.enchantment) ? this.level : 0;
    }
}
