package net.avaritia.avaritiaspear.api.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 多模式切换接口（潜行 + 右键切换）。
 * <p>
 * 语义照搬 Avaritia 1.21 的 {@code api.iface.item.ISwitchable}，
 * 但那边用数据组件（{@code DataComponents.CUSTOM_DATA} + {@code CustomData}）存模式，
 * 1.20.1 没有数据组件，这里改成物品 NBT（{@code mode} 子标签），对外行为一致。
 */
public interface ISwitchable {

    String MODE_TAG = "mode";

    /** 取物品上的 mode 子标签（没有就建）。 */
    static CompoundTag getModeTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            tag = new CompoundTag();
            stack.setTag(tag);
        }
        if (!tag.contains(MODE_TAG)) {
            tag.put(MODE_TAG, new CompoundTag());
        }
        return tag.getCompound(MODE_TAG);
    }

    /** 某个功能是否打开。 */
    static boolean isMode(ItemStack stack, String funcName) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_TAG)) return false;
        CompoundTag modeTag = tag.getCompound(MODE_TAG);
        return modeTag.contains(funcName) && modeTag.getBoolean(funcName);
    }

    default boolean isActive(ItemStack stack, String funcName) {
        return isMode(stack, funcName);
    }

    /** 当前激活的模式下标；一个都没开就默认开第一个。 */
    static int getCurrentMode(ItemStack stack, List<String> modeList) {
        for (int i = 0; i < modeList.size(); i++) {
            if (isMode(stack, modeList.get(i))) return i;
        }
        if (!modeList.isEmpty()) {
            setMode(stack, modeList, 0);
            return 0;
        }
        return -1;
    }

    default int getCurrentModeIndex(ItemStack stack, List<String> modeList) {
        return getCurrentMode(stack, modeList);
    }

    /** 从多个模式里单选一个（先全关再开指定项）。 */
    static void setMode(ItemStack stack, List<String> modeList, int modeIndex) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag modeTag = tag.contains(MODE_TAG) ? tag.getCompound(MODE_TAG) : new CompoundTag();
        for (String mode : modeList) {
            modeTag.putBoolean(mode, false);
        }
        if (modeIndex >= 0 && modeIndex < modeList.size()) {
            modeTag.putBoolean(modeList.get(modeIndex), true);
        }
        tag.put(MODE_TAG, modeTag);
    }

    /** 在列表里循环切换。 */
    default void cycleMode(@NotNull Level world, Player player, @NotNull InteractionHand hand, List<String> modeList) {
        if (modeList.isEmpty()) return;
        ItemStack stack = player.getItemInHand(hand);
        int nextIndex = (getCurrentModeIndex(stack, modeList) + 1) % modeList.size();
        setMode(stack, modeList, nextIndex);
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(modeMessage("tooltip.avaritia.active", modeList.get(nextIndex)), true);
        }
        player.swing(hand);
    }

    /** 直接切到指定模式。 */
    default void switchToMode(@NotNull Level world, Player player, @NotNull InteractionHand hand,
                              List<String> modeList, String modeName) {
        if (!modeList.contains(modeName)) return;
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag modeTag = getModeTag(stack);
        for (String mode : modeList) {
            modeTag.putBoolean(mode, false);
        }
        modeTag.putBoolean(modeName, true);
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(modeMessage("tooltip.avaritia.active", modeName), true);
        }
        player.swing(hand);
    }

    /** 单个功能开关翻转（本模组用的是这个，配"潜行 + 右键"）。 */
    default void switchMode(@NotNull Level world, Player player, @NotNull InteractionHand hand, String funcName) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag modeTag = getModeTag(stack);
        boolean now = !modeTag.getBoolean(funcName);
        modeTag.putBoolean(funcName, now);
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    modeMessage(now ? "tooltip.avaritia.active" : "tooltip.avaritia.inactive", funcName), true);
        }
        player.swing(hand);
    }

    /**
     * 构造模式切换提示。文案全部用 Avaritia 自己的键：
     * {@code tooltip.avaritia.active} = "%s 模式已激活!"，{@code inactive} = "%s 模式已取消!"，
     * 模式名则取 {@code tooltip.avaritia.tool.<模式名>}（lunge / crystal_shatter / blaze_spear_blast
     * 这三条由本模组的语言文件补充）。
     */
    private static Component modeMessage(String stateKey, String funcName) {
        return Component.translatable(stateKey, Component.translatable("tooltip.avaritia.tool." + funcName));
    }
}
