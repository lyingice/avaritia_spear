package net.avaritia.avaritiaspear.api.item;

/**
 * 标记接口：物品不消耗耐久。
 * <p>
 * 对应 Avaritia 1.21 的 {@code api.iface.item.IUndamageable}，1.20.1 那份里没有，自己实现一份。
 * 它本身只作标记，实际效果由物品覆写 {@code isDamageable} 实现。
 */
public interface IUndamageable {
}
