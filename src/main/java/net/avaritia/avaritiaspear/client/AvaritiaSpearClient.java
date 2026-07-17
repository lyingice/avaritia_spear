package net.avaritia.avaritiaspear.client;

import net.avaritia.avaritiaspear.client.model.CosmicSpModelLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/**
 * 无尽矛客户端事件 —— 不依赖 cosmic 模型 loader，
 * 纯代码方式将 mask sprite 注册到 block atlas 并缓存，
 * 供 {@link net.avaritia.avaritiaspear.mixin.ItemRendererCosmicMixin} 渲染星空层使用。
 */
@EventBusSubscriber(modid = "avaritia_spear", value = Dist.CLIENT)
public class AvaritiaSpearClient {

    /** mask 贴图在 block atlas 上的 ResourceLocation */
    public static final ResourceLocation SPEAR_MASK = ResourceLocation.fromNamespaceAndPath("avaritia_spear", "mask/item/infinity_spear");
    public static final ModelResourceLocation SPEAR_IN_HAND_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("avaritia_spear", "item/infinity_spear_in_hand"));

    /** atlas stitch 后缓存的 mask sprite */
    public static TextureAtlasSprite spearMaskSprite;

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SPEAR_IN_HAND_MODEL);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath("avaritia_spear", "cosmic_sp"), CosmicSpModelLoader.INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAtlasStitched(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            spearMaskSprite = event.getAtlas().getSprite(SPEAR_MASK);
        }
    }
}
