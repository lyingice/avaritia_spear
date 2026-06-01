package net.avaritia.avaritiaspear.init;

import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

public class AvaritiaSpearModItems {
    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems("avaritia_spear");

    public static final DeferredItem<Item> INFINITY_SPEAR = REGISTRY.register("infinity_spear",
            () -> new InfinitySpearItem());
}