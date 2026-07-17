package net.avaritia.avaritiaspear.init;

import committee.nova.mods.avaritia.init.registry.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = "avaritia_spear", value = Dist.CLIENT)
public class CreativeTab {

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> infinityTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath("avaritia", "avaritia_group"));

        if (event.getTabKey() == infinityTab) {
            event.insertAfter(
                    new ItemStack(ModItems.infinity_sword.get()),
                    AvaritiaSpearModItems.INFINITY_SPEAR.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}