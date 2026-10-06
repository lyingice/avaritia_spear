package net.avaritia.avaritiaspear.api.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 自带附魔（虚拟附魔）。
 * <p>
 * 对应 Avaritia 1.21 分支的 {@code net.avaritia.avaritiaspear.api.item.InitEnchantItem}，
 * 但 1.20.1 的那份 Re-Avaritia 里没有这个接口，所以在本模组内自己实现一份。
 * 真正让附魔生效的是 {@code InitEnchantMixin}（挂到 EnchantmentHelper 上）。
 */
public interface InitEnchantItem {
    /**
     * 返回该物品"自带"的某个附魔等级；没有则返回 0。
     * 真实附魔始终优先（{@code InitEnchantMixin} 只在真实等级为 0 时才补）。
     */
    int getInitEnchantLevel(ItemStack stack, Holder<Enchantment> enchantmentHolder);
}
