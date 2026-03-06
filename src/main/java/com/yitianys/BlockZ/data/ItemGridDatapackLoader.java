package com.yitianys.BlockZ.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class ItemGridDatapackLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public ItemGridDatapackLoader() {
        super(GSON, "blockz/item_grids");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        ItemSizeManager.loadCustomSizes();
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonElement element = entry.getValue();
            if (!element.isJsonObject()) continue;
            JsonObject root = element.getAsJsonObject();
            if (root.has("items")) {
                JsonObject items = root.getAsJsonObject("items");
                for (Map.Entry<String, JsonElement> itemEntry : items.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(itemEntry.getKey());
                    if (id == null) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(id);
                    if (item == null || item == Items.AIR) continue;
                    JsonElement value = itemEntry.getValue();
                    if (!value.isJsonObject()) continue;
                    JsonObject obj = value.getAsJsonObject();
                    if (obj.has("width") && obj.has("height")) {
                        int w = obj.get("width").getAsInt();
                        int h = obj.get("height").getAsInt();
                        if (w > 0 && h > 0) {
                            ItemSizeManager.registerSize(item, w, h);
                        }
                    }
                    int capW = obj.has("cap_width") ? obj.get("cap_width").getAsInt() : -1;
                    int capH = obj.has("cap_height") ? obj.get("cap_height").getAsInt() : -1;
                    if (capW > 0 && capH > 0) {
                        ItemSizeManager.registerCapacityShape(item, capW, capH);
                    }
                }
            }
            if (root.has("nbt_items") && root.get("nbt_items").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("nbt_items")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("id") || !obj.has("nbt_key") || !obj.has("nbt_value")) continue;
                    if (!obj.has("width") || !obj.has("height")) continue;
                    ResourceLocation id = ResourceLocation.tryParse(obj.get("id").getAsString());
                    if (id == null) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(id);
                    if (item == null || item == Items.AIR) continue;
                    String key = obj.get("nbt_key").getAsString();
                    String value = obj.get("nbt_value").getAsString();
                    int w = obj.get("width").getAsInt();
                    int h = obj.get("height").getAsInt();
                    if (w <= 0 || h <= 0) continue;
                    ItemSizeManager.registerNbtSize(item, key, value, w, h);
                }
            }
        }
        BlockZ.LOGGER.info("Loaded {} BlockZ item grid datapack definitions", map.size());
    }
}
