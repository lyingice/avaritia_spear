package net.avaritia.avaritiaspear.init;

import committee.nova.mods.avaritia.init.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把三把矛插到 Avaritia 自己的创造栏里，紧跟在对应的剑后面。
 * <p>
 * 注意：{@code BuildCreativeModeTabContentsEvent} 是<b>模组总线</b>事件，
 * Forge 的 @EventBusSubscriber 默认是游戏总线（FORGE），不写 bus = MOD 的话这个监听器
 * 永远不会被调用（在 spearcore 上踩过这个坑）。
 */
@Mod.EventBusSubscriber(modid = "avaritia_spear", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreativeTab {

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> infinityTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                new ResourceLocation("avaritia", "avaritia_group"));

        if (event.getTabKey() == infinityTab) {
            // 1.20.1 没有 insertAfter 辅助方法，直接用可哈希链表的 putAfter
            event.getEntries().putAfter(
                    new ItemStack(ModItems.infinity_sword.get()),
                    AvaritiaSpearModItems.INFINITY_SPEAR.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putAfter(
                    new ItemStack(ModItems.blaze_sword.get()),
                    AvaritiaSpearModItems.BLAZE_SPEAR.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putAfter(
                    new ItemStack(ModItems.crystal_sword.get()),
                    AvaritiaSpearModItems.CRYSTAL_SPEAR.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}