package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.config.BlockZConfigs;
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
    private final Map<Item, Integer> capacityWidths;
    private final Map<Item, Integer> gridColors;

    public SyncGridRulesS2C(boolean gridEnabled, Map<Item, ItemSizeManager.ItemSize> sizes, List<ItemSizeManager.NbtRule> nbtRules,
                            Map<Item, Integer> customSlots, Map<Item, Integer> capacityWidths, Map<Item, Integer> gridColors) {
        this.gridEnabled = gridEnabled;
        this.sizes = sizes;
        this.nbtRules = nbtRules;
        this.customSlots = customSlots;
        this.capacityWidths = capacityWidths;
        this.gridColors = gridColors;
    }

    public static SyncGridRulesS2C createServerSnapshot() {
        return new SyncGridRulesS2C(
                BlockZConfigs.isGridEnabled(),
                ItemSizeManager.snapshotSizes(),
                ItemSizeManager.snapshotNbtRules(),
                ItemSizeManager.snapshotCustomSlots(),
                ItemSizeManager.snapshotCapacityWidths(),
                ItemSizeManager.snapshotGridColors()
        );
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

        this.capacityWidths = new HashMap<>();
        int widthCount = buf.readVarInt();
        for (int i = 0; i < widthCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            int width = buf.readVarInt();
            if (item != null) {
                capacityWidths.put(item, width);
            }
        }

        this.gridColors = new HashMap<>();
        int colorCount = buf.readVarInt();
        for (int i = 0; i < colorCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = ForgeRegistries.ITEMS.getValue(id);
            int color = buf.readInt();
            if (item != null) {
                gridColors.put(item, color);
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

        buf.writeVarInt(capacityWidths.size());
        capacityWidths.forEach((item, width) -> {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            buf.writeVarInt(width);
        });

        buf.writeVarInt(gridColors.size());
        gridColors.forEach((item, color) -> {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            buf.writeInt(color);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ItemSizeManager.setSyncedGridEnabled(gridEnabled);
            ItemSizeManager.setRules(sizes, nbtRules, customSlots, capacityWidths, gridColors);
        });
        context.setPacketHandled(true);
    }
}
