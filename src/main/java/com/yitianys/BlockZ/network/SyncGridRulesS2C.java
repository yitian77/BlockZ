package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncGridRulesS2C {
    private final boolean gridEnabled;
    private final Map<Item, ItemSizeManager.ItemSize> sizes;
    private final List<ItemSizeManager.NbtRule> nbtRules;
    private final Map<Item, Integer> customSlots;

    public SyncGridRulesS2C(boolean gridEnabled, Map<Item, ItemSizeManager.ItemSize> sizes, List<ItemSizeManager.NbtRule> nbtRules, Map<Item, Integer> customSlots) {
        this.gridEnabled = gridEnabled;
        this.sizes = sizes;
        this.nbtRules = nbtRules;
        this.customSlots = customSlots;
    }

    public SyncGridRulesS2C(FriendlyByteBuf buf) {
        this.gridEnabled = buf.readBoolean();
        
        this.sizes = new HashMap<>();
        int sizeCount = buf.readVarInt();
        for (int i = 0; i < sizeCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            int w = buf.readVarInt();
            int h = buf.readVarInt();
            if (item != null) {
                sizes.put(item, new ItemSizeManager.ItemSize(w, h));
            }
        }

        this.nbtRules = new ArrayList<>();
        int ruleCount = buf.readVarInt();
        for (int i = 0; i < ruleCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            String key = buf.readUtf();
            String value = buf.readUtf();
            int w = buf.readVarInt();
            int h = buf.readVarInt();
            if (item != null) {
                nbtRules.add(new ItemSizeManager.NbtRule(item, key, value, w, h));
            }
        }

        this.customSlots = new HashMap<>();
        int slotCount = buf.readVarInt();
        for (int i = 0; i < slotCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            int slots = buf.readVarInt();
            if (item != null) {
                customSlots.put(item, slots);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(gridEnabled);
        
        buf.writeVarInt(sizes.size());
        sizes.forEach((item, size) -> {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            buf.writeVarInt(size.width());
            buf.writeVarInt(size.height());
        });

        buf.writeVarInt(nbtRules.size());
        for (ItemSizeManager.NbtRule rule : nbtRules) {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(rule.item()));
            buf.writeUtf(rule.nbtKey());
            buf.writeUtf(rule.nbtValue());
            buf.writeVarInt(rule.width());
            buf.writeVarInt(rule.height());
        }

        buf.writeVarInt(customSlots.size());
        customSlots.forEach((item, slots) -> {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            buf.writeVarInt(slots);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ItemSizeManager.setSyncedGridEnabled(gridEnabled);
            ItemSizeManager.setRules(sizes, nbtRules, customSlots);
        });
        context.setPacketHandled(true);
    }
}
