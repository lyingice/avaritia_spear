package net.avaritia.avaritiaspear.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CosmicSpModelLoader implements IGeometryLoader<CosmicSpModelLoader.Geometry> {
    public static final CosmicSpModelLoader INSTANCE = new CosmicSpModelLoader();

    @Override
    public @NotNull Geometry read(@NotNull JsonObject modelContents, @NotNull JsonDeserializationContext context) throws JsonParseException {
        JsonObject baseJson = modelContents.deepCopy();
        baseJson.remove("loader");
        baseJson.remove("cosmic");

        JsonObject maskJson = baseJson.deepCopy();
        JsonObject cosmic = GsonHelper.getAsJsonObject(modelContents, "cosmic");
        JsonObject textures = GsonHelper.getAsJsonObject(maskJson, "textures", new JsonObject());
        String mask = GsonHelper.getAsString(cosmic, "mask");
        textures.addProperty("layer0", mask);
        maskJson.add("textures", textures);

        return new Geometry(
                context.deserialize(baseJson, BlockModel.class),
                context.deserialize(maskJson, BlockModel.class)
        );
    }

    public record Geometry(BlockModel baseModel, BlockModel maskModel) implements IUnbakedGeometry<Geometry> {
        @Override
        public void resolveParents(@NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull IGeometryBakingContext context) {
            baseModel.resolveParents(modelGetter);
            maskModel.resolveParents(modelGetter);
        }

        // 1.20.1 的 bake 比 1.21 多一个 ResourceLocation（模型位置）参数，
        // BlockModel.bake 也相应多一个位置参数。
        @Override
        public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, @NotNull ModelBaker baker,
                                        @NotNull Function<Material, TextureAtlasSprite> spriteGetter,
                                        @NotNull ModelState modelState, @NotNull ItemOverrides overrides,
                                        @NotNull ResourceLocation modelLocation) {
            BakedModel bakedBase = baseModel.bake(baker, baseModel, spriteGetter, modelState, modelLocation, true);
            BakedModel bakedMask = maskModel.bake(baker, maskModel, spriteGetter, modelState, modelLocation, true);
            return new CosmicSpBakedModel(bakedBase, bakedMask);
        }
    }
}
