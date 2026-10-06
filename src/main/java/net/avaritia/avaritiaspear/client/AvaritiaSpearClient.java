package net.avaritia.avaritiaspear.client;

import net.avaritia.avaritiaspear.client.model.CosmicSpModelLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 无尽矛客户端事件 —— 不依赖 cosmic 模型 loader，
 * 纯代码方式将 mask sprite 注册到 block atlas 并缓存，
 * 供 {@link net.avaritia.avaritiaspear.mixin.ItemRendererCosmicMixin} 渲染星空层使用。
 * <p>
 * 1.20.1 的差异：
 * <ul>
 *   <li>{@code EventBusSubscriber} 是 {@code Mod} 的嵌套注解，且这两个事件都是<b>模组总线</b>事件，必须写 {@code bus = Bus.MOD}；</li>
 *   <li>{@code TextureAtlasStitchedEvent}（1.21）在 1.20.1 叫 {@code TextureStitchEvent.Post}；</li>
 *   <li>{@code ModelResourceLocation.standalone(...)}（1.21）在 1.20.1 是 {@code new ModelResourceLocation(loc, "standalone")}。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "avaritia_spear", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AvaritiaSpearClient {

    /** mask 贴图在 block atlas 上的 ResourceLocation */
    public static final ResourceLocation SPEAR_MASK = new ResourceLocation("avaritia_spear", "mask/item/infinity_spear");
    // 1.21 用 ModelResourceLocation.standalone(...)，1.20.1 没有这个概念；
    // 物品模型要用 "inventory" variant，否则 ModelBakery 会去找 blockstates 定义并报
    // "missing model for variant"。（standalone 是 1.20.2+ 才认的）
    // 注意：inventory variant 会自动补 item/ 前缀，所以这里不能再写 item/，
    // 否则会去找 models/item/item/xxx.json。
    public static final ModelResourceLocation SPEAR_IN_HAND_MODEL = new ModelResourceLocation(
            new ResourceLocation("avaritia_spear", "infinity_spear_in_hand"), "inventory");

    /** atlas stitch 后缓存的 mask sprite */
    public static TextureAtlasSprite spearMaskSprite;

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SPEAR_IN_HAND_MODEL);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        // 1.20.1 的 register 收 String 名字（1.21 才是 ResourceLocation）
        event.register("cosmic_sp", CosmicSpModelLoader.INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAtlasStitched(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            spearMaskSprite = event.getAtlas().getSprite(SPEAR_MASK);
        }
    }
}
