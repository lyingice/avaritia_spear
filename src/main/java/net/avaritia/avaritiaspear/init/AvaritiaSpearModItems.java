package net.avaritia.avaritiaspear.init;

import net.avaritia.avaritiaspear.item.BlazeSpearItem;
import net.avaritia.avaritiaspear.item.CrystalSpearItem;
import net.avaritia.avaritiaspear.item.InfinitySpearItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AvaritiaSpearModItems {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, "avaritia_spear");

    public static final RegistryObject<Item> INFINITY_SPEAR = REGISTRY.register("infinity_spear",
            InfinitySpearItem::new);
    public static final RegistryObject<Item> CRYSTAL_SPEAR = REGISTRY.register("crystal_spear",
            CrystalSpearItem::new);
    public static final RegistryObject<Item> BLAZE_SPEAR = REGISTRY.register("blaze_spear",
            BlazeSpearItem::new);
}
