package com.yitianys.BlockZ.menu;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.capability.PlayerBackpackProvider;
import com.yitianys.BlockZ.client.gui.UIConstants;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.entity.CorpseEntity;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.init.ModMenus;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.menu.slot.TetrisSlot;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import com.yitianys.BlockZ.util.InventoryUtils;
import com.yitianys.BlockZ.util.ItemHandlerContainer;
import com.yitianys.BlockZ.util.ItemSizeManager;
import com.yitianys.BlockZ.menu.VicinityManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DayZInventoryMenu extends AbstractContainerMenu {
    public static final TagKey<Item> BACKPACKS = ItemTags.create(new ResourceLocation(BlockZ.MODID, "backpacks"));
    public static final int VICINITY_SLOTS = 81;

    public static final class VicinitySlotLayout {
        public final int index;
        public final int x;
        public final int y;

        public VicinitySlotLayout(int index, int x, int y) {
            this.index = index;
            this.x = x;
            this.y = y;
        }
    }

    private static List<VicinitySlotLayout> pendingClientLayout;

    // Proxy container that delegates to activeContainer or behaves as empty
    private final SimpleContainer vicinityInventory = new SimpleContainer(VICINITY_SLOTS);
    private final Container vicinityProxy = new Container() {
        @Override
        public int getContainerSize() {
            return VICINITY_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return (DayZInventoryMenu.this.activeContainer == null || DayZInventoryMenu.this.activeContainer.isEmpty()) && DayZInventoryMenu.this.vicinityInventory.isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            if (DayZInventoryMenu.this.player.level().isClientSide) {
                return DayZInventoryMenu.this.vicinityInventory.getItem(index);
            }

            if (DayZInventoryMenu.this.activeContainer != null) {
                int containerSize = DayZInventoryMenu.this.activeContainer.getContainerSize();
                int containerIndex = DayZInventoryMenu.this.mapToActiveContainerIndex(index);
                if (containerIndex >= 0 && containerIndex < containerSize) {
                    return DayZInventoryMenu.this.activeContainer.getItem(containerIndex);
                }
            }

            return DayZInventoryMenu.this.vicinityInventory.getItem(index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack result;
            if (DayZInventoryMenu.this.activeContainer != null) {
                int containerSize = DayZInventoryMenu.this.activeContainer.getContainerSize();
                int containerIndex = DayZInventoryMenu.this.mapToActiveContainerIndex(index);
                if (containerIndex >= 0 && containerIndex < containerSize) {
                    result = DayZInventoryMenu.this.activeContainer.removeItem(containerIndex, count);
                    DayZInventoryMenu.this.vicinityInventory.setItem(index, DayZInventoryMenu.this.activeContainer.getItem(containerIndex));
                    DayZInventoryMenu.this.activeContainer.setChanged();

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.activeContainer instanceof BaseContainerBlockEntity be) {
                        be.setChanged();
                    }

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.lastLootrId != null) {
                        DayZInventoryMenu.this.isVicinityDirty = true;
                    }

                    return result;
                }
            }

            result = DayZInventoryMenu.this.vicinityInventory.removeItem(index, count);
            DayZInventoryMenu.this.vicinityInventory.setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack result;
            if (DayZInventoryMenu.this.activeContainer != null) {
                int containerSize = DayZInventoryMenu.this.activeContainer.getContainerSize();
                int containerIndex = DayZInventoryMenu.this.mapToActiveContainerIndex(index);
                if (containerIndex >= 0 && containerIndex < containerSize) {
                    result = DayZInventoryMenu.this.activeContainer.removeItemNoUpdate(containerIndex);
                    DayZInventoryMenu.this.vicinityInventory.setItem(index, ItemStack.EMPTY);
                    DayZInventoryMenu.this.activeContainer.setChanged();

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.activeContainer instanceof BaseContainerBlockEntity be) {
                        be.setChanged();
                    }

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.lastLootrId != null) {
                        DayZInventoryMenu.this.isVicinityDirty = true;
                    }

                    return result;
                }
            }

            result = DayZInventoryMenu.this.vicinityInventory.removeItemNoUpdate(index);
            DayZInventoryMenu.this.vicinityInventory.setChanged();
            return result;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (DayZInventoryMenu.this.activeContainer != null) {
                int containerSize = DayZInventoryMenu.this.activeContainer.getContainerSize();
                int containerIndex = DayZInventoryMenu.this.mapToActiveContainerIndex(index);
                if (containerIndex >= 0 && containerIndex < containerSize) {
                    DayZInventoryMenu.this.activeContainer.setItem(containerIndex, stack);
                    DayZInventoryMenu.this.vicinityInventory.setItem(index, stack);
                    DayZInventoryMenu.this.activeContainer.setChanged();

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.activeContainer instanceof BaseContainerBlockEntity be) {
                        be.setChanged();
                    }

                    if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.lastLootrId != null) {
                        DayZInventoryMenu.this.isVicinityDirty = true;
                    }

                    return;
                }
            }

            DayZInventoryMenu.this.vicinityInventory.setItem(index, stack);
            DayZInventoryMenu.this.vicinityInventory.setChanged();
        }

        @Override
        public void setChanged() {
            if (DayZInventoryMenu.this.activeContainer != null) {
                DayZInventoryMenu.this.activeContainer.setChanged();

                if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.activeContainer instanceof BaseContainerBlockEntity be) {
                    be.setChanged();
                } else if (!DayZInventoryMenu.this.player.level().isClientSide && DayZInventoryMenu.this.containerPos != null) {
                    BlockEntity be = DayZInventoryMenu.this.player.level().getBlockEntity(DayZInventoryMenu.this.containerPos);
                    if (be != null) {
                        be.setChanged();
                    }
                }

                if (!DayZInventoryMenu.this.player.level().isClientSide) {
                    DayZInventoryMenu.this.isVicinityDirty = true;
                }
            }

            DayZInventoryMenu.this.vicinityInventory.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return (DayZInventoryMenu.this.activeContainer == null || DayZInventoryMenu.this.activeContainer.stillValid(player)) && DayZInventoryMenu.this.vicinityInventory.stillValid(player);
        }

        @Override
        public void clearContent() {
            if (DayZInventoryMenu.this.activeContainer != null) {
                DayZInventoryMenu.this.activeContainer.clearContent();
            }

            DayZInventoryMenu.this.vicinityInventory.clearContent();
        }
    };

    private final List<ItemEntity> nearbyEntities = new ArrayList<>();
    private final Player player;
    public final boolean isLockedMode;
    public Container activeContainer = null;
    private int containerPage = 0;
    private BlockPos containerPos = null;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private boolean isWorkbench = false;
    public boolean isEnchantingTable = false;
    
    // Enchantment Fields
    public final int[] costs = new int[3];
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};
    private final RandomSource random = RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();

    private boolean isLoading = false;
    private boolean suppressDrop = false; // Flag to prevent double-dropping during swap
    // 5x6 Vicinity + 10xX Inventory, 64 slots total for inventory seems safe for current configs
    private final ItemStackHandler backpackContentHandler = new ItemStackHandler(64) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!isLoading) {
                saveBackpackToItem();
            }
        }
    };
    
    private boolean isCorpseLoading = false;
    private boolean corpseStorageDirty = false;
    private int corpseStorageSlotStart = -1;
    private int corpseStorageCapacity = 0;
    private int lastCorpseBackpackCap = 0;
    private int lastCorpseVestCap = 0;
    private int lastCorpseShirtCap = 0;
    private int lastCorpsePantsCap = 0;
    private final ItemStackHandler corpseContentHandler = new ItemStackHandler(128) {
        @Override
        protected void onContentsChanged(int slot) {
            if (isCorpseLoading) return;
            corpseStorageDirty = true;
        }
    };

    private Entity containerEntity;
    private int syncedPocketCount = -1;
    private List<VicinitySlotLayout> clientVicinityLayout;
    private Map<Integer, VicinitySlotLayout> clientLayoutMap;

    public void setSyncedPocketCount(int count) {
        this.syncedPocketCount = count;
    }

    public static void setPendingClientLayout(List<VicinitySlotLayout> layout) {
        pendingClientLayout = layout;
    }

    private static List<VicinitySlotLayout> consumePendingClientLayout() {
        List<VicinitySlotLayout> layout = pendingClientLayout;
        pendingClientLayout = null;
        return layout;
    }

    private void setClientVicinityLayout(List<VicinitySlotLayout> layout) {
        this.clientVicinityLayout = layout;
        this.clientLayoutMap = null;
        if (layout != null && !layout.isEmpty()) {
            Map<Integer, VicinitySlotLayout> map = new HashMap<>();
            for (VicinitySlotLayout entry : layout) {
                map.put(entry.index, entry);
            }
            this.clientLayoutMap = map;
        }
    }

    public static DayZInventoryMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        int pocketCount = buf.readInt();
        boolean hasPos = buf.readBoolean();
        
        BlockPos pos = null;
        Entity entity = null;
        int virtualContainerSize = -1;

        if (hasPos) {
            pos = buf.readBlockPos();
        } else {
            if (buf.isReadable()) {
                byte type = buf.readByte();
                if (type == 1) {
                    int entityId = buf.readInt();
                    entity = inv.player.level().getEntity(entityId);
                } else if (type == 2) {
                    virtualContainerSize = buf.readInt();
                }
            }
        }

        DayZInventoryMenu menu;
        if (entity != null) {
            menu = new DayZInventoryMenu(id, inv, entity);
        } else if (pos != null) {
            menu = new DayZInventoryMenu(id, inv, pos);
        } else if (virtualContainerSize > 0) {
            // Virtual Container Mode
            menu = new DayZInventoryMenu(id, inv, new SimpleContainer(virtualContainerSize));
        } else {
            // Fallback (should not happen if protocol is correct)
            menu = new DayZInventoryMenu(id, inv, (BlockPos) null);
        }
        
        if (pocketCount != -1) {
            menu.setSyncedPocketCount(pocketCount);
            // Force update slot positions with the synced pocket count
            menu.updateSlotPositions();
        }
        return menu;
    }

    public DayZInventoryMenu(int id, Inventory inv) {
        this(id, inv, (BlockPos) null);
    }

    // Layout Y positions for Screen rendering
    public int pocketsY = UIConstants.INVENTORY_SLOTS_Y;
    public int backpackY = -1000;
    public int vestY = -1000;
    public int shirtY = -1000;
    public int pantsY = -1000;

    // 记录各分区当前使用的列数（用于 Tetris 点击/锚点计算）
    private int backpackSectionCols = UIConstants.INVENTORY_COLS;
    private int vestSectionCols = UIConstants.INVENTORY_COLS;
    private int shirtSectionCols = UIConstants.INVENTORY_COLS;
    private int pantsSectionCols = UIConstants.INVENTORY_COLS;
    
    // Corpse Storage Layout Y positions
    public int corpseStorageY = -1000; // Deprecated/Fallback
    public int corpseBackpackY = -1000;
    public int corpseVestY = -1000;
    public int corpseShirtY = -1000;
    public int corpsePantsY = -1000;

    // Public capacities for Screen rendering
    public int backpackCapacity = 0;
    public int vestCapacity = 0;
    public int shirtCapacity = 0;
    public int pantsCapacity = 0;

    // Track last capacity to ensure correct offset handling during save
    private int lastBackpackCap = 0;
    private int lastVestCap = 0;
    private int lastShirtCap = 0;
    private int lastPantsCap = 0;
    
    public boolean isCorpseMode() {
        return this.activeContainer instanceof CorpseEntity;
    }
    
    public int getCorpseStorageSlotStart() {
        return corpseStorageSlotStart;
    }
    
    public int getCorpseStorageSlotEnd() {
        if (corpseStorageSlotStart < 0) return -1;
        return corpseStorageSlotStart + 45 - 1;
    }
    
    public int getCorpseStorageCapacity() {
        return corpseStorageCapacity;
    }

    // Vicinity 布局模式：有容器时使用 9 列宽面板，普通背包界面使用 5 列窄面板
    public boolean isContainerVicinityLayout() {
        return this.activeContainer != null && !this.isWorkbench && !this.isEnchantingTable && !(this.activeContainer instanceof CorpseEntity);
    }

    public boolean supportsContainerPaging() {
        return this.activeContainer != null && !this.isWorkbench && !this.isEnchantingTable && !(this.activeContainer instanceof CorpseEntity) && this.activeContainer.getContainerSize() > VICINITY_SLOTS;
    }

    public int getContainerPage() {
        return containerPage;
    }

    public int getContainerPageCount() {
        if (!supportsContainerPaging()) return 1;
        int size = this.activeContainer.getContainerSize();
        return (size + VICINITY_SLOTS - 1) / VICINITY_SLOTS;
    }

    public void setContainerPage(int page) {
        int newPage = clampContainerPage(page);
        if (newPage == this.containerPage) return;
        this.containerPage = newPage;
        if (!player.level().isClientSide) {
            markVicinityDirty();
            updateVicinityItems(player);
        }
        updateSlotPositions();
    }

    public int getVicinityCols() {
        if (isContainerVicinityLayout()) return getContainerVicinityCols();
        if (isCorpseMode()) return getCorpseMaxCols();
        return UIConstants.INVENTORY_COLS;
    }

    public int getVicinityPanelWidth() {
        if (isContainerVicinityLayout()) {
            int cols = getContainerVicinityCols();
            int width = UIConstants.PANEL_W + Math.max(0, cols - UIConstants.INVENTORY_COLS) * UIConstants.SLOT_PITCH;
            int layoutWidth = getClientLayoutRequiredWidth();
            if (layoutWidth > 0) {
                width = Math.max(width, layoutWidth);
            }
            return Math.min(UIConstants.VICINITY_PANEL_W, width);
        }
        int cols = getVicinityCols();
        int extraCols = Math.max(0, cols - UIConstants.INVENTORY_COLS);
        return UIConstants.PANEL_W + extraCols * UIConstants.SLOT_PITCH;
    }

    private int getClientLayoutRequiredWidth() {
        if (this.clientLayoutMap == null || this.clientLayoutMap.isEmpty()) return 0;
        int maxX = 0;
        for (VicinitySlotLayout layout : this.clientLayoutMap.values()) {
            if (layout.x > maxX) {
                maxX = layout.x;
            }
        }
        int padding = UIConstants.PANEL_W - UIConstants.INVENTORY_COLS * UIConstants.SLOT_PITCH;
        return maxX + UIConstants.SLOT_SIZE + Math.max(0, padding);
    }

    // 当 Vicinity 使用窄面板时，需要整体向右平移，和 PLAYER 面板贴紧
    public int getVicinityOffsetX() {
        return UIConstants.VICINITY_PANEL_W - getVicinityPanelWidth();
    }

    private int getContainerVicinityCols() {
        if (this.activeContainer == null) return UIConstants.VICINITY_COLS;
        int size = this.activeContainer.getContainerSize();
        if (size <= 0) return UIConstants.VICINITY_COLS;
        if (size >= UIConstants.VICINITY_COLS * 2) return UIConstants.VICINITY_COLS;
        int cols = (int) Math.ceil(Math.sqrt(size));
        if (cols < 1) cols = 1;
        if (cols > UIConstants.VICINITY_COLS) cols = UIConstants.VICINITY_COLS;
        return cols;
    }

    private int getContainerPageOffset() {
        if (!supportsContainerPaging()) return 0;
        return this.containerPage * VICINITY_SLOTS;
    }

    private int getCorpseMaxCols() {
        if (!isCorpseMode()) return UIConstants.INVENTORY_COLS;
        int maxCols = UIConstants.INVENTORY_COLS;
        int limit = UIConstants.VICINITY_COLS;
        int defaultCols = UIConstants.INVENTORY_COLS;

        ItemStack cBpStack = getCorpseEquipmentStack(0);
        ItemStack cVestStack = getCorpseEquipmentStack(1);
        ItemStack cShirtStack = getCorpseEquipmentStack(2);
        ItemStack cPantsStack = getCorpseEquipmentStack(3);

        int bpCap = getCorpseBackpackSlots(cBpStack);
        int vestCap = getCorpseBackpackSlots(cVestStack);
        int shirtCap = getCorpseBackpackSlots(cShirtStack);
        int pantsCap = getCorpseBackpackSlots(cPantsStack);

        if (bpCap > 0) maxCols = Math.max(maxCols, getCorpseCapacityCols(cBpStack, bpCap, defaultCols, limit));
        if (vestCap > 0) maxCols = Math.max(maxCols, getCorpseCapacityCols(cVestStack, vestCap, defaultCols, limit));
        if (shirtCap > 0) maxCols = Math.max(maxCols, getCorpseCapacityCols(cShirtStack, shirtCap, defaultCols, limit));
        if (pantsCap > 0) maxCols = Math.max(maxCols, getCorpseCapacityCols(cPantsStack, pantsCap, defaultCols, limit));

        if (maxCols > limit) maxCols = limit;
        return maxCols;
    }

    private int getCorpseCapacityCols(ItemStack stack, int cap, int defaultCols, int limit) {
        if (cap <= 0) return defaultCols;
        int cols = ItemSizeManager.getCapacityCols(stack, defaultCols);
        if (cols <= 0) cols = defaultCols;
        if (cols > limit) cols = limit;
        if (cols > cap) cols = cap;
        return cols;
    }

    private int clampContainerPage(int page) {
        int max = Math.max(0, getContainerPageCount() - 1);
        if (page < 0) return 0;
        if (page > max) return max;
        return page;
    }

    private int mapToActiveContainerIndex(int slotIndex) {
        if (this.activeContainer == null) return slotIndex;
        return supportsContainerPaging() ? slotIndex + getContainerPageOffset() : slotIndex;
    }

    public DayZInventoryMenu(int id, Inventory inv, Entity entity) {
        super(ModMenus.DAYZ_INVENTORY.get(), id);
        this.player = inv.player;
        this.containerEntity = entity;
        
        // 预先计算锁定状态
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);
        // 如果是尸体模式，强制解除锁定，以便玩家使用背包扩展槽位
        boolean isCorpse = entity instanceof CorpseEntity;
        this.isLockedMode = (!dayzEnabled && !player.hasPermissions(2)) && !isCorpse;

        this.containerPos = entity != null ? entity.blockPosition() : null;
        this.access = ContainerLevelAccess.create(inv.player.level(), entity != null ? entity.blockPosition() : inv.player.blockPosition());
        
        if (entity instanceof Container c) {
            this.activeContainer = c;
            c.startOpen(player);
        }
        
        // Initialize Crafting Grid
        this.craftSlots = new TransientCraftingContainer(this, 2, 2);
        
        initSlots(inv);
    }

    public DayZInventoryMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, pos, null);
    }

    public DayZInventoryMenu(int id, Inventory inv, net.minecraft.world.Container container) {
        this(id, inv, null, container);
    }

    private DayZInventoryMenu(int id, Inventory inv, BlockPos pos, net.minecraft.world.Container container) {
        super(ModMenus.DAYZ_INVENTORY.get(), id);
        this.player = inv.player;
        
        // 预先计算锁定状态
        boolean dayzEnabled = player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::isDayzEnabled)
                .orElse(true);
        this.isLockedMode = !dayzEnabled && !player.hasPermissions(2);

        this.containerPos = pos;
        this.access = pos != null ? ContainerLevelAccess.create(inv.player.level(), pos) : ContainerLevelAccess.create(inv.player.level(), inv.player.blockPosition());
        
        if (container != null) {
            this.activeContainer = container;
            this.activeContainer.startOpen(player);
        } else if (pos != null) {
            BlockEntity be = player.level().getBlockEntity(pos);
            
            // 优先检查是否是 Lootr 容器
            IItemHandler lootrInv = InventoryUtils.getLootrInventory(player.level(), pos, player);
            if (lootrInv != null) {
                this.activeContainer = new ItemHandlerContainer(lootrInv);
                this.lastContainerPos = pos;
                this.lastLootrId = InventoryUtils.getLootrTileId(be);
                InventoryUtils.startOpenLootr(be, player);
            } else {
                // 检查是否是双箱 (Double Chest)
                // ChestBlock.getContainer 会自动处理双箱合并
                if (player.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock chestBlock) {
                    Container combined = net.minecraft.world.level.block.ChestBlock.getContainer(
                        chestBlock, 
                        player.level().getBlockState(pos), 
                        player.level(), 
                        pos, 
                        true // ignoreBlocked: allow opening even if blocked? Usually false, but let's stick to default behavior or true if we want to force open
                    );
                    if (combined != null) {
                        this.activeContainer = combined;
                    } else if (be instanceof Container c) {
                         this.activeContainer = c;
                    }
                } else if (be instanceof Container c) {
                    this.activeContainer = c;
                }

                if (this.activeContainer == null && be != null) {
                    be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> this.activeContainer = new ItemHandlerContainer(handler));
                }

                if (this.activeContainer != null) {
                    this.lastContainerPos = pos;
                    this.lastLootrId = null;
                    this.activeContainer.startOpen(player);
                }
            }
            
            if (player.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.CraftingTableBlock) {
                this.isWorkbench = true;
            }
            if (player.level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.EnchantmentTableBlock) {
                this.isEnchantingTable = true;
            }
        }
        
        // Initialize Crafting Grid based on context (Moved initialization here)
        if (this.isWorkbench) {
            this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        } else {
            this.craftSlots = new TransientCraftingContainer(this, 2, 2);
        }

        if (this.isEnchantingTable) {
            this.addDataSlot(this.enchantmentSeed).set(inv.player.getEnchantmentSeed());
            for(int i = 0; i < 3; ++i) {
                this.addDataSlot(DataSlot.shared(this.costs, i));
                this.addDataSlot(DataSlot.shared(this.enchantClue, i));
                this.addDataSlot(DataSlot.shared(this.levelClue, i));
            }
        }
        
        initSlots(inv);
    }

    private void initSlots(Inventory inv) {
        // 0-29: Vicinity 槽位 (5x6)
        addVicinitySlots(inv);
        
        // 30-38: 装备槽位 (Vanilla Armor + Capability Slots)
        addEquipmentSlots(inv);
        
        // 39-88: 主物品栏槽位 (27个)
        addMainInventorySlots(inv);
        
        // 89-97: 快捷栏槽位 (9个)
        addHotbarSlots(inv);

        // Crafting Slots - Add these ONLY if NOT a workbench
        if (!this.isWorkbench) {
            // 98: Crafting Result (2x2)
            this.addSlot(new ResultSlot(inv.player, this.craftSlots, this.resultSlots, 0, UIConstants.CRAFTING_RESULT_X, UIConstants.CRAFTING_RESULT_Y));

            // 99-102: Crafting Input (2x2)
            for(int i = 0; i < 2; ++i) {
                for(int j = 0; j < 2; ++j) {
                    this.addSlot(new Slot(this.craftSlots, j + i * 2, UIConstants.CRAFTING_X + j * 18, UIConstants.CRAFTING_Y + i * 18));
                }
            }
        }

        // 最后初始化背包内容，确保槽位已添加
        loadBackpackFromItem();
        
        if (isCorpseMode()) {
            ensureCorpseStorageSlotsAdded();
            loadCorpseStorageFromItems();
        }
        
        // Update positions initially
        updateSlotPositions();
    }
    
    @Override
    public void removed(Player player) {
        saveBackpackToItem();
        saveCorpseStorageToItems();
        super.removed(player);
        this.resultSlots.clearContent();
        
        if (this.activeContainer != null) {
            this.activeContainer.stopOpen(player);
        }
        
        // 确保 Lootr 动画关闭
        if (lastLootrId != null && lastContainerPos != null) {
            closeLootrAnimation(player, lastContainerPos);
        }
        
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.craftSlots);
        });
        
        if (this.isEnchantingTable) {
             this.clearContainer(player, this.vicinityInventory);
        }
    }

    @Override
    public void slotsChanged(Container p_38920_) {
        super.slotsChanged(p_38920_);
        // Update slot positions when inventory changes (e.g. equipment change)
        // We do this aggressively to ensure layout is correct
        if (this.player != null && !this.player.level().isClientSide) {
             // Server side update
             updateSlotPositions();
             // We might need to sync this to client? 
             // Actually, slots are synced via standard container sync if we change their x/y?
             // No, x/y are not automatically synced. They are usually static.
             // If we change x/y on server, client doesn't know.
             // Client needs to run updateSlotPositions() too.
             // Client runs slotsChanged when packets arrive?
             // Yes, ClientboundContainerSetSlotPacket triggers updates.
        } else if (this.player != null && this.player.level().isClientSide) {
             updateSlotPositions();
        }

        if (p_38920_ == this.craftSlots) {
            slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots);
        }
        if (this.isEnchantingTable && p_38920_ == this.vicinityInventory) {
            this.access.execute((level, pos) -> {
                this.slotsChangedEnchantment(this.vicinityInventory, level, pos);
            });
        }
    }

    private static final java.lang.reflect.Field SLOT_X_FIELD;
    private static final java.lang.reflect.Field SLOT_Y_FIELD;
    
    // Total height of the content in the scrollable area
    public int totalContentHeight = 0;
    public int totalVicinityHeight = 0; // Height for the left panel (Vicinity/Corpse)

    static {
        java.lang.reflect.Field x = null;
        java.lang.reflect.Field y = null;
        try {
            // 尝试使用 ObfuscationReflectionHelper 获取字段 (适用于生产环境 SRG 名)
            try {
                x = ObfuscationReflectionHelper.findField(Slot.class, "f_40220_"); // x
                y = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_"); // y
            } catch (Exception e) {
                // 如果失败 (例如在某些开发环境中)，尝试直接获取
                x = net.minecraft.world.inventory.Slot.class.getDeclaredField("x");
                y = net.minecraft.world.inventory.Slot.class.getDeclaredField("y");
            }
            x.setAccessible(true);
            y.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SLOT_X_FIELD = x;
        SLOT_Y_FIELD = y;
    }

    public void updateSlotPositions() {
        if (this.player == null) return;

        if (this.clientVicinityLayout == null && this.player.level().isClientSide && isContainerVicinityLayout()) {
            List<VicinitySlotLayout> pendingLayout = consumePendingClientLayout();
            if (pendingLayout != null && !pendingLayout.isEmpty()) {
                setClientVicinityLayout(pendingLayout);
            }
        }
        
        // 使用已有的 isLockedMode 字段
        boolean isLocked = this.isLockedMode;
        if (!supportsContainerPaging()) {
            this.containerPage = 0;
        } else {
            this.containerPage = clampContainerPage(this.containerPage);
        }

        // 1. Get Capacities
        ItemStack backpackStack = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK))
                .orElse(ItemStack.EMPTY);
        ItemStack vestStack = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST))
                .orElse(ItemStack.EMPTY);
        ItemStack shirtStack = this.player.getInventory().getArmor(2);
        ItemStack pantsStack = this.player.getInventory().getArmor(1);

        int bpCap = BlockZConfigs.getBackpackSlots(backpackStack);
        int vestCap = BlockZConfigs.getBackpackSlots(vestStack);
        int shirtCap = BlockZConfigs.getBackpackSlots(shirtStack);
        int pantsCap = BlockZConfigs.getBackpackSlots(pantsStack);

        int[] safeCaps = clampBackpackCaps(bpCap, vestCap, shirtCap, pantsCap);
        bpCap = safeCaps[0];
        vestCap = safeCaps[1];
        shirtCap = safeCaps[2];
        pantsCap = safeCaps[3];

        this.backpackCapacity = bpCap;
        this.vestCapacity = vestCap;
        this.shirtCapacity = shirtCap;
        this.pantsCapacity = pantsCap;

        int startX = UIConstants.INVENTORY_SLOTS_X;
        int currentY = UIConstants.INVENTORY_SLOTS_Y;
        final int cols = UIConstants.INVENTORY_COLS;
        final int sectionMaxCols = UIConstants.INVENTORY_MAX_COLS;
        int gap = 24;

        // 2.1 口袋区域（固定在顶部），其高度决定后续“衣服/背包内容区”的起始位置。
        // 否则穿上衣服/背包后，内容格子会从同一个 Y 开始布局，造成与口袋重叠。
        int pocketCount = getPocketCount();
        int pocketRows = (pocketCount + cols - 1) / cols;
        int pocketsHeight = pocketRows * UIConstants.SLOT_PITCH;
        this.pocketsY = UIConstants.INVENTORY_SLOTS_Y;

        // 口袋槽位需要每 tick 重新设置位置：
        // Screen 滚动时会把不可见槽位移到 -10000 隐藏，如果这里不恢复，口袋会“永久消失”直到重开菜单。
        int pocketStartIdx = getPocketStart();
        for (int i = 0; i < pocketCount; i++) {
            int menuIndex = pocketStartIdx + i;
            if (menuIndex >= this.slots.size()) break;
            Slot s = this.slots.get(menuIndex);
            int r = i / cols;
            int c = i % cols;
            int x = startX + c * UIConstants.SLOT_PITCH;
            int y = UIConstants.INVENTORY_SLOTS_Y + r * UIConstants.SLOT_PITCH;
            setSlotPos(s, x, y);
        }

        currentY += pocketsHeight + gap;
        
        // Track Vicinity Height
        int vicinityMinY = UIConstants.VICINITY_SLOTS_Y;
        int vicinityMaxY = UIConstants.VICINITY_SLOTS_Y;

        // 2. Position Core Inventory Slots
        // Equipment (81-89)
        // Main Inventory (90-116)
        // Hotbar (117-125)
        // Crafting (126-130)
        // These are mostly handled by their own UI constants or fixed positions in initSlots,
        // but we might need to hide them or move them if needed.
        // For now, let's keep them at their default positions defined in UIConstants.

        // 3. Position Backpack Grid
        // 注意：背包网格在菜单中的索引会随着 pocketCount 等变化而变化，不能写死。
        int backpackStartIdx = getBackpackSlotStart();
        
        if (isLocked) {
            // In locked mode, we just show a limited set of slots linearly
            int lockedSlotsCount = getPocketCount();
            for (int i = 0; i < lockedSlotsCount; i++) {
                int menuIndex = backpackStartIdx + i;
                if (menuIndex >= this.slots.size()) break;
                
                Slot s = this.slots.get(menuIndex);
                int r = i / cols;
                int c = i % cols;
                int x = startX + c * UIConstants.SLOT_PITCH;
                int y = currentY + r * UIConstants.SLOT_PITCH;
                setSlotPos(s, x, y);
            }
            this.backpackY = currentY;
            this.vestY = -1000;
            this.shirtY = -1000;
            this.pantsY = -1000;

            int rows = (int) Math.ceil((double) lockedSlotsCount / cols);
            currentY += rows * UIConstants.SLOT_PITCH + gap;
        } else {
            // In DayZ mode, we group by item type
            int backpackOffset = 0;
            int vestOffset = backpackOffset + bpCap;
            int shirtOffset = vestOffset + vestCap;
            int pantsOffset = shirtOffset + shirtCap;

            // Position Shirt Slots
            if (shirtCap > 0) {
                this.shirtY = currentY;
                int shirtCols = getCapacityColsForItem(shirtStack, sectionMaxCols, shirtCap);
                this.shirtSectionCols = shirtCols;
                updateGridPos(backpackStartIdx + shirtOffset, shirtCap, startX, currentY, shirtCols, shirtOffset);
                int rows = (int) Math.ceil((double) shirtCap / shirtCols);
                currentY += rows * UIConstants.SLOT_PITCH + gap;
            } else {
                this.shirtY = -1000;
                this.shirtSectionCols = cols;
            }

            // Position Pants Slots
            if (pantsCap > 0) {
                this.pantsY = currentY;
                int pantsCols = getCapacityColsForItem(pantsStack, sectionMaxCols, pantsCap);
                this.pantsSectionCols = pantsCols;
                updateGridPos(backpackStartIdx + pantsOffset, pantsCap, startX, currentY, pantsCols, pantsOffset);
                int rows = (int) Math.ceil((double) pantsCap / pantsCols);
                currentY += rows * UIConstants.SLOT_PITCH + gap;
            } else {
                this.pantsY = -1000;
                this.pantsSectionCols = cols;
            }

            // Position Vest Slots
            if (vestCap > 0) {
                this.vestY = currentY;
                int vestCols = getCapacityColsForItem(vestStack, sectionMaxCols, vestCap);
                this.vestSectionCols = vestCols;
                updateGridPos(backpackStartIdx + vestOffset, vestCap, startX, currentY, vestCols, vestOffset);
                int rows = (int) Math.ceil((double) vestCap / vestCols);
                currentY += rows * UIConstants.SLOT_PITCH + gap;
            } else {
                this.vestY = -1000;
                this.vestSectionCols = cols;
            }

            // Position Backpack Slots
            if (bpCap > 0) {
                this.backpackY = currentY;
                int bpCols = getCapacityColsForItem(backpackStack, sectionMaxCols, bpCap);
                this.backpackSectionCols = bpCols;
                updateGridPos(backpackStartIdx + backpackOffset, bpCap, startX, currentY, bpCols, backpackOffset);
                int rows = (int) Math.ceil((double) bpCap / bpCols);
                currentY += rows * UIConstants.SLOT_PITCH + gap;
            } else {
                this.backpackY = -1000;
                this.backpackSectionCols = cols;
            }

            // Hide unused slots
            int totalUsedCap = bpCap + vestCap + shirtCap + pantsCap;
            int gridSlots = getBackpackGridSlots();
            for (int i = totalUsedCap; i < gridSlots; i++) {
                int menuIndex = backpackStartIdx + i;
                if (menuIndex < this.slots.size()) {
                    setSlotPos(this.slots.get(menuIndex), -10000, -10000);
                }
            }
        }

        this.totalContentHeight = currentY - UIConstants.INVENTORY_SLOTS_Y;

        // 4. Position Vicinity Slots (Indices 0-80)
        if (this.slots.size() >= 30) {
            int vicBaseX = UIConstants.VICINITY_SLOTS_X + getVicinityOffsetX();
            int visible = 0;
            if (this.isWorkbench) {
                visible = 10; // 3x3 + Result
            } else if (this.isEnchantingTable) {
                visible = 2; // Item + Lapis
            } else if (this.activeContainer instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
                visible = 3; // In, Fuel, Out
            } else if (this.activeContainer != null) {
                if (this.activeContainer instanceof CorpseEntity) {
                    visible = getCorpseVisibleSlots(this.activeContainer);
                } else {
                    int remaining = this.activeContainer.getContainerSize() - getContainerPageOffset();
                    visible = Math.min(VICINITY_SLOTS, Math.max(0, remaining));
                }
            } else {
                // Ground Items Mode: Only show filled slots to reduce clutter
                int lastFilledIndex = -1;
                for (int i = 0; i < VICINITY_SLOTS; i++) {
                    if (!this.vicinityInventory.getItem(i).isEmpty()) {
                        lastFilledIndex = i;
                    }
                }
                visible = lastFilledIndex + 1;
            }

            for (int i = 0; i < VICINITY_SLOTS; i++) {
                Slot s = this.slots.get(i);
                if (i < visible) {
                    if (this.activeContainer instanceof CorpseEntity) {
                        layoutCorpseVicinitySlot(i, s);
                        if (s.y + 18 > vicinityMaxY) vicinityMaxY = s.y + 18;
                    } else if (this.isWorkbench || this.isEnchantingTable || this.activeContainer instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) {
                        // Special layouts are handled in addVicinitySlots, so we just track height here
                        if (s.y + 18 > vicinityMaxY) vicinityMaxY = s.y + 18;
                    } else {
                        // Standard Grid Layout for containers or ground
                        if (this.clientLayoutMap != null && isContainerVicinityLayout() && this.activeContainer != null) {
                            int containerIndex = mapToActiveContainerIndex(i);
                            VicinitySlotLayout layout = this.clientLayoutMap.get(containerIndex);
                            if (layout != null) {
                                int x = vicBaseX + layout.x;
                                int y = UIConstants.VICINITY_SLOTS_Y + layout.y;
                                setSlotPos(s, x, y);
                                if (y + 18 > vicinityMaxY) vicinityMaxY = y + 18;
                                continue;
                            }
                        }
                        int vCols = getVicinityCols();
                        int r = i / vCols;
                        int c = i % vCols;
                        int x = vicBaseX + c * UIConstants.SLOT_PITCH;
                        int y = UIConstants.VICINITY_SLOTS_Y + r * UIConstants.SLOT_PITCH;
                        setSlotPos(s, x, y);
                        if (y + 18 > vicinityMaxY) vicinityMaxY = y + 18;
                    }
                } else {
                    setSlotPos(s, -10000, -10000);
                }
            }
        }

        // 5. Position Corpse Storage Slots (if applicable)
        int corpseStorageSlotStart = getCorpseStorageSlotStart();
        if (isCorpseMode() && corpseStorageSlotStart >= 0) {
            ItemStack cBpStack = getCorpseEquipmentStack(0);
            ItemStack cVestStack = getCorpseEquipmentStack(1);
            ItemStack cShirtStack = getCorpseEquipmentStack(2);
            ItemStack cPantsStack = getCorpseEquipmentStack(3);

            int cBpCap = getCorpseBackpackSlots(cBpStack);
            int cVestCap = getCorpseBackpackSlots(cVestStack);
            int cShirtCap = getCorpseBackpackSlots(cShirtStack);
            int cPantsCap = getCorpseBackpackSlots(cPantsStack);

            int safeBpCount = Math.min(cBpCap, 45);
            int safeVestCount = Math.min(cVestCap, 45 - safeBpCount);
            int safeShirtCount = Math.min(cShirtCap, 45 - safeBpCount - safeVestCount);
            int safePantsCount = Math.min(cPantsCap, 45 - safeBpCount - safeVestCount - safeShirtCount);

            this.corpseStorageCapacity = safeBpCount + safeVestCount + safeShirtCount + safePantsCount;

            if (this.corpseStorageCapacity > 0) {
                int cCurrentY = vicinityMaxY + gap;
                int cStartX = UIConstants.VICINITY_SLOTS_X + getVicinityOffsetX();

                int vCols = getVicinityCols();

                // Position Corpse Backpack Slots
                if (safeBpCount > 0) {
                    cCurrentY += 10;
                    this.corpseBackpackY = cCurrentY;
                    int bpOffset = 0;
                    int sectionCols = getCapacityColsForItem(cBpStack, vCols, safeBpCount);
                    updateGridPos(corpseStorageSlotStart + bpOffset, safeBpCount, cStartX, cCurrentY, sectionCols, bpOffset);
                    {
                        int rows = (int) Math.ceil((double) safeBpCount / sectionCols);
                        cCurrentY += rows * UIConstants.SLOT_PITCH + 4;
                    }
                } else {
                    this.corpseBackpackY = -1000;
                }

                // Position Corpse Vest Slots
                if (safeVestCount > 0) {
                    if (cCurrentY > vicinityMaxY + gap) cCurrentY += 2;
                    else cCurrentY += 10;

                    this.corpseVestY = cCurrentY;
                    int vestOffset = safeBpCount;
                    int sectionCols = getCapacityColsForItem(cVestStack, vCols, safeVestCount);
                    updateGridPos(corpseStorageSlotStart + vestOffset, safeVestCount, cStartX, cCurrentY, sectionCols, vestOffset);
                    int rows = (int) Math.ceil((double) safeVestCount / sectionCols);
                    cCurrentY += rows * UIConstants.SLOT_PITCH + 4;
                } else {
                    this.corpseVestY = -1000;
                }

                // Position Corpse Shirt Slots
                if (safeShirtCount > 0) {
                    if (cCurrentY > vicinityMaxY + gap) cCurrentY += 2;
                    else cCurrentY += 10;

                    this.corpseShirtY = cCurrentY;
                    int shirtOffset = safeBpCount + safeVestCount;
                    int sectionCols = getCapacityColsForItem(cShirtStack, vCols, safeShirtCount);
                    updateGridPos(corpseStorageSlotStart + shirtOffset, safeShirtCount, cStartX, cCurrentY, sectionCols, shirtOffset);
                    int rows = (int) Math.ceil((double) safeShirtCount / sectionCols);
                    cCurrentY += rows * UIConstants.SLOT_PITCH + 4;
                } else {
                    this.corpseShirtY = -1000;
                }

                // Position Corpse Pants Slots
                if (safePantsCount > 0) {
                    if (cCurrentY > vicinityMaxY + gap) cCurrentY += 2;
                    else cCurrentY += 10;

                    this.corpsePantsY = cCurrentY;
                    int pantsOffset = safeBpCount + safeVestCount + safeShirtCount;
                    int sectionCols = getCapacityColsForItem(cPantsStack, vCols, safePantsCount);
                    updateGridPos(corpseStorageSlotStart + pantsOffset, safePantsCount, cStartX, cCurrentY, sectionCols, pantsOffset);
                    int rows = (int) Math.ceil((double) safePantsCount / sectionCols);
                    cCurrentY += rows * UIConstants.SLOT_PITCH + 4;
                } else {
                    this.corpsePantsY = -1000;
                }

                vicinityMaxY = cCurrentY;
                this.corpseStorageY = vicinityMaxY;
            } else {
                this.corpseStorageY = -1000;
                this.corpseBackpackY = -1000;
                this.corpseVestY = -1000;
                this.corpseShirtY = -1000;
                this.corpsePantsY = -1000;
            }

            // Hide overflow slots
            for (int i = 0; i < 45; i++) {
                boolean isValid = false;
                if (i < safeBpCount) isValid = true;
                else if (i >= safeBpCount && i < safeBpCount + safeVestCount) isValid = true;
                else if (i >= safeBpCount + safeVestCount && i < safeBpCount + safeVestCount + safeShirtCount) isValid = true;
                else if (i >= safeBpCount + safeVestCount + safeShirtCount && i < safeBpCount + safeVestCount + safeShirtCount + safePantsCount) isValid = true;

                if (!isValid && corpseStorageSlotStart + i < this.slots.size()) {
                    setSlotPos(this.slots.get(corpseStorageSlotStart + i), -10000, -10000);
                }
            }
        } else {
            this.corpseStorageY = -1000;
            this.corpseBackpackY = -1000;
            this.corpseVestY = -1000;
            this.corpseShirtY = -1000;
            this.corpsePantsY = -1000;
        }

        this.totalVicinityHeight = vicinityMaxY - UIConstants.VICINITY_SLOTS_Y;
    }

    private int getCorpseVisibleSlots(Container container) {
        if (!(container instanceof CorpseEntity corpse)) return 0;
        // Corpse has 9 equipment slots (0-8) + hotbar (9-17) + pockets (18-XX)
        return 18 + this.getPocketCount();
    }

    private void layoutCorpseVicinitySlot(int containerIndex, Slot slot) {
        int equipmentStartX = UIConstants.VICINITY_SLOTS_X + getVicinityOffsetX() + 4;
        int equipmentStartY = UIConstants.VICINITY_SLOTS_Y + 10;
        int x = -10000, y = -10000;
        int eqCol = -1, eqRow = -1;

        if (containerIndex == 4) { // Head
            eqCol = 0; eqRow = 0;
        } else if (containerIndex == 7) { // Mask
            eqCol = 1; eqRow = 0;
        } else if (containerIndex == 2) { // Shirt
            eqCol = 2; eqRow = 0;
        } else if (containerIndex == 3) { // Pants
            eqCol = 3; eqRow = 0;
        } else if (containerIndex == 5) { // Shoes
            eqCol = 4; eqRow = 0;
        } else if (containerIndex == 1) { // Vest
            eqCol = 0; eqRow = 1;
        } else if (containerIndex == 0) { // Backpack
            eqCol = 1; eqRow = 1;
        } else if (containerIndex == 6) { // Offhand
            eqCol = 2; eqRow = 1;
        } else if (containerIndex == 8) { // Gloves
            eqCol = 3; eqRow = 1;
        }

        if (eqCol >= 0) {
            x = equipmentStartX + eqCol * UIConstants.SLOT_PITCH;
            y = equipmentStartY + eqRow * UIConstants.SLOT_PITCH;
        } else {
            int lootStartX = UIConstants.VICINITY_SLOTS_X + getVicinityOffsetX();
            int hotbarStartY = equipmentStartY + UIConstants.SLOT_PITCH * 2 + 18;
            int pocketsStartY = hotbarStartY + UIConstants.SLOT_PITCH * 2 + 8;
            int vCols = getVicinityCols();

            if (containerIndex >= 9 && containerIndex <= 17) {
                int hotbarIndex = containerIndex - 9;
                int r = hotbarIndex / vCols;
                int c = hotbarIndex % vCols;
                x = lootStartX + c * UIConstants.SLOT_PITCH;
                y = hotbarStartY + r * UIConstants.SLOT_PITCH;
            } else if (containerIndex >= 18) {
                int pocketIndex = containerIndex - 18;
                int r = pocketIndex / vCols;
                int c = pocketIndex % vCols;
                x = lootStartX + c * UIConstants.SLOT_PITCH;
                y = pocketsStartY + r * UIConstants.SLOT_PITCH;
            }
        }
        setSlotPos(slot, x, y);
    }

    private boolean canEquip(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getType().getSlot() == slot;
        }
        if (stack.getItem() instanceof Equipable equipable) {
            return equipable.getEquipmentSlot() == slot;
        }
        return false;
    }

    private void updateGridPos(int startSlotIdx, int count, int startX, int startY, int cols, int sectionStart) {
        // BlockZ.LOGGER.info("UpdateGridPos: StartIdx={}, Count={}, SectionStart={}", startSlotIdx, count, sectionStart);
        for (int i = 0; i < count; i++) {
            if (startSlotIdx + i >= this.slots.size()) break;
            int menuIndex = startSlotIdx + i;
            Slot s = this.slots.get(menuIndex);
            
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * UIConstants.SLOT_PITCH;
            int y = startY + row * UIConstants.SLOT_PITCH;
            
            setSlotPosWithCols(s, x, y, sectionStart, count, cols);
        }
    }

    private void setSlotPosWithCols(Slot slot, int x, int y, int sectionStart, int sectionSize, int sectionCols) {
        if (slot instanceof TetrisSlot ts) {
            ts.setSectionBounds(sectionStart, sectionSize);
            ts.setSectionGridCols(sectionCols);
        }
        
        if (SLOT_X_FIELD == null || SLOT_Y_FIELD == null) return;
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCapacityColsForItem(ItemStack stack, int maxCols, int cap) {
        if (cap <= 0) return maxCols;
        int cols = ItemSizeManager.getCapacityCols(stack, maxCols);
        if (cols <= 0) cols = maxCols;
        if (cols > maxCols) cols = maxCols;
        if (cols > cap) cols = cap;
        return cols;
    }

    private void setSlotPos(Slot slot, int x, int y, int sectionStart, int sectionSize) {
        if (slot instanceof TetrisSlot ts) {
            ts.setSectionBounds(sectionStart, sectionSize);
            // if (sectionSize > 0) BlockZ.LOGGER.info("SetSlotPos: Slot={}, X={}, Y={}, SectionStart={}, SectionSize={}", slot.index, x, y, sectionStart, sectionSize);
        }
        
        if (SLOT_X_FIELD == null || SLOT_Y_FIELD == null) return;
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSlotPos(Slot slot, int x, int y) {
        setSlotPos(slot, x, y, 0, 0);
    }

    private void slotsChangedEnchantment(Container inventory, Level level, BlockPos pos) {
        ItemStack itemstack = inventory.getItem(0);
        if (!itemstack.isEmpty() && itemstack.isEnchantable()) {
              float f = 0;

              for(BlockPos blockpos : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
                 if (EnchantmentTableBlock.isValidBookShelf(level, pos, blockpos)) {
                    f += level.getBlockState(pos.offset(blockpos)).getEnchantPowerBonus(level, pos.offset(blockpos));
                 }
              }

              this.random.setSeed((long)this.enchantmentSeed.get());

              for(int i = 0; i < 3; ++i) {
                 this.costs[i] = EnchantmentHelper.getEnchantmentCost(this.random, i, (int)f, itemstack);
                 this.enchantClue[i] = -1;
                 this.levelClue[i] = -1;
                 if (this.costs[i] < i + 1) {
                    this.costs[i] = 0;
                 }
              }

              for(int j = 0; j < 3; ++j) {
                 if (this.costs[j] > 0) {
                    java.util.List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, j, this.costs[j]);
                    if (list != null && !list.isEmpty()) {
                       EnchantmentInstance enchantmentinstance = list.get(this.random.nextInt(list.size()));
                       this.enchantClue[j] = RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY).registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getId(enchantmentinstance.enchantment);
                       this.levelClue[j] = enchantmentinstance.level;
                    }
                 }
              }

              this.broadcastChanges();
        } else {
           for(int i = 0; i < 3; ++i) {
              this.costs[i] = 0;
              this.enchantClue[i] = -1;
              this.levelClue[i] = -1;
           }
        }
    }

    private java.util.List<EnchantmentInstance> getEnchantmentList(ItemStack stack, int enchantSlot, int level) {
        this.random.setSeed((long)(this.enchantmentSeed.get() + enchantSlot));
        java.util.List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, stack, level, false);
        if (stack.getItem() == Items.BOOK && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }
        return list;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.isEnchantingTable) {
            if (id >= 0 && id < this.costs.length) {
                ItemStack itemstack = this.vicinityInventory.getItem(0);
                ItemStack itemstack1 = this.vicinityInventory.getItem(1);
                int i = id + 1;
                if ((itemstack1.isEmpty() || itemstack1.getCount() < i) && !player.getAbilities().instabuild) {
                    return false;
                } else if (this.costs[id] <= 0 || itemstack.isEmpty() || (player.experienceLevel < i || player.experienceLevel < this.costs[id]) && !player.getAbilities().instabuild) {
                    return false;
                } else {
                    this.access.execute((level, pos) -> {
                        ItemStack itemstack2 = itemstack;
                        java.util.List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, id, this.costs[id]);
                        if (!list.isEmpty()) {
                            player.onEnchantmentPerformed(itemstack, i);
                            boolean flag = itemstack.is(Items.BOOK);
                            if (flag) {
                                itemstack2 = new ItemStack(Items.ENCHANTED_BOOK);
                                this.vicinityInventory.setItem(0, itemstack2);
                            }

                            for(EnchantmentInstance enchantmentinstance : list) {
                                if (flag) {
                                    EnchantedBookItem.addEnchantment(itemstack2, enchantmentinstance);
                                } else {
                                    itemstack2.enchant(enchantmentinstance.enchantment, enchantmentinstance.level);
                                }
                            }

                            if (!player.getAbilities().instabuild) {
                                itemstack1.shrink(i);
                                if (itemstack1.isEmpty()) {
                                    this.vicinityInventory.setItem(1, ItemStack.EMPTY);
                                }
                            }

                            player.awardStat(Stats.ENCHANT_ITEM);
                            if (player instanceof ServerPlayer) {
                                CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer)player, itemstack2, i);
                            }

                            this.vicinityInventory.setChanged();
                            this.enchantmentSeed.set(player.getEnchantmentSeed());
                            this.slotsChangedEnchantment(this.vicinityInventory, level, pos);
                            level.playSound((Player)null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
                        }

                    });
                    return true;
                }
            }
        }
        if (id == 100 || id == 101) {
            if (supportsContainerPaging()) {
                int delta = id == 100 ? -1 : 1;
                setContainerPage(this.containerPage + delta);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, net.minecraft.world.level.Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, level);
            if (optional.isPresent()) {
                CraftingRecipe craftingrecipe = optional.get();
                if (resultSlots.setRecipeUsed(level, serverplayer, craftingrecipe)) {
                    itemstack = craftingrecipe.assemble(craftSlots, level.registryAccess());
                }
            }

            resultSlots.setItem(0, itemstack);
            
            // Find the correct result slot index in the menu
            int resultSlotIndex = -1;
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot instanceof ResultSlot && ((ResultSlot)slot).container == resultSlots) {
                    resultSlotIndex = i;
                    break;
                }
            }

            if (resultSlotIndex != -1) {
                menu.setRemoteSlot(resultSlotIndex, itemstack);
                serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), resultSlotIndex, itemstack));
            }
        }
    }

    private void addVicinitySlots(Inventory inv) {
        int addedSlots = 0;

        // 1. Context Specific Slots
        if (this.isWorkbench) {
            // Special Layout for Workbench (3x3 Crafting + Result)
            int offsetX = getVicinityOffsetX();
            int centerX = UIConstants.VICINITY_X + offsetX + UIConstants.PANEL_W / 2;
            int startY = UIConstants.VICINITY_SLOTS_Y + 30;
            int gridX = UIConstants.VICINITY_X + offsetX + 21;

            // Crafting Input (0-8 in our list order, but mapped to craftSlots 0-8)
            for(int i = 0; i < 3; ++i) {
                for(int j = 0; j < 3; ++j) {
                    this.addSlot(new Slot(this.craftSlots, j + i * 3, gridX + j * 18, startY + i * 18));
                    addedSlots++;
                }
            }
            
            // Result Slot (9)
            this.addSlot(new ResultSlot(inv.player, this.craftSlots, this.resultSlots, 0, centerX - 9, startY - 24));
            addedSlots++;
        }
        else if (this.isEnchantingTable) {
            // Special Layout for Enchanting Table
            int offsetX = getVicinityOffsetX();
            int centerX = UIConstants.VICINITY_X + offsetX + UIConstants.PANEL_W / 2;
            int startY = UIConstants.VICINITY_SLOTS_Y + 20;

            // Slot 0: Item to Enchant
            this.addSlot(new Slot(this.vicinityProxy, 0, centerX - 9, startY) {
                @Override public boolean mayPlace(ItemStack stack) { return true; }
                @Override public int getMaxStackSize() { return 1; }
            });
            addedSlots++;

            // Slot 1: Lapis
            this.addSlot(new Slot(this.vicinityProxy, 1, centerX - 9, startY + 36) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(net.minecraftforge.common.Tags.Items.GEMS_LAPIS); }
            });
            addedSlots++;
        }
        else if (this.activeContainer instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
            // Special Layout for Furnace
            int offsetX = getVicinityOffsetX();
            int centerX = UIConstants.VICINITY_X + offsetX + UIConstants.PANEL_W / 2;
            int startY = UIConstants.VICINITY_SLOTS_Y + 20;

            // Slot 0: Input
            this.addSlot(new Slot(this.vicinityProxy, 0, centerX - 18, startY + 18));
            addedSlots++;
            
            // Slot 1: Fuel
            this.addSlot(new Slot(this.vicinityProxy, 1, centerX - 18, startY + 54));
            addedSlots++;

            // Slot 2: Output (Using FurnaceResultSlot for XP)
            this.addSlot(new net.minecraft.world.inventory.FurnaceResultSlot(inv.player, furnace, 2, centerX + 18, startY + 36));
            addedSlots++;
        }
        else if (this.activeContainer != null && this.activeContainer.getContainerSize() == 3) {
            // Compatibility for common 3-slot containers from third-party mods
            // Map to a Furnace-like layout: Input, Fuel, Output
            int centerX = UIConstants.VICINITY_X + UIConstants.PANEL_W / 2;
            int startY = UIConstants.VICINITY_SLOTS_Y + 20;

            // Slot 0: Input
            this.addSlot(new Slot(this.vicinityProxy, 0, centerX - 9, startY));
            addedSlots++;
            
            // Slot 1: Fuel
            this.addSlot(new Slot(this.vicinityProxy, 1, centerX - 9, startY + 36));
            addedSlots++;
            
            // Slot 2: Output (generic slot for third-party containers)
            this.addSlot(new Slot(this.vicinityProxy, 2, centerX + 24, startY + 18));
            addedSlots++;
        }
        else if (this.activeContainer != null) {
            // Generic Container (Chest, Barrel, etc.)
            // Use Standard 9xX Grid Layout for ALL generic containers to match "DayZ UI" style
            // Map slots 0 to containerSize-1 to Container
            int size = this.activeContainer.getContainerSize();
            
            // Note: We loop up to VICINITY_SLOTS later, so just add the valid ones first
            for (int i = 0; i < size && addedSlots < VICINITY_SLOTS; i++) {
                 int vCols = getVicinityCols();
                 int r = addedSlots / vCols;
                 int c = addedSlots % vCols;
                 int x = UIConstants.VICINITY_SLOTS_X + c * UIConstants.SLOT_PITCH;
                 int y = UIConstants.VICINITY_SLOTS_Y + r * UIConstants.SLOT_PITCH;
                
                 if (isCorpseMode() && i <= 8) {
                     this.addSlot(createCorpseEquipmentSlot(i, x, y));
                 } else {
                     this.addSlot(new Slot(this.vicinityProxy, i, x, y));
                 }
                 addedSlots++;
            }
        }
        else {
             // No Container (Ground Items only)
             // Will be filled by padding loop
        }

        // 2. Pad Remaining Slots to ensure Vicinity always has VICINITY_SLOTS slots
        // This fixes the "Interactive blocks cannot drag items" bug caused by index shifting
        // and allows Ground Items to appear in empty slots (via vicinityProxy delegation)
        while (addedSlots < VICINITY_SLOTS) {
            int x, y;
            int vicBaseX = UIConstants.VICINITY_SLOTS_X + getVicinityOffsetX();

            // Fix for "Two-layer grid" issue:
            // 当存在任何活动容器时，Vicinity 的剩余槽位不再显示地面物品，避免混入容器网格
            // 保持槽位 ID 一致，但将其移出屏幕
            if (this.isWorkbench || this.isEnchantingTable) {
                x = -10000;
                y = -10000;
            } else {
                // Standard Grid layout for padding (Ground items or Chest items)
                int vCols = getVicinityCols();
                
                // Hide slots beyond 30 if in narrow mode (5 cols) to avoid overlap
                if (vCols == 5 && addedSlots >= 30) {
                    x = -10000;
                    y = -10000;
                } else {
                    int r = addedSlots / vCols;
                    int c = addedSlots % vCols;
                    x = vicBaseX + c * UIConstants.SLOT_PITCH;
                    y = UIConstants.VICINITY_SLOTS_Y + r * UIConstants.SLOT_PITCH;
                }
            }

            // Use vicinityProxy with the current index.
            // If activeContainer is set but index >= size, vicinityProxy delegates to vicinityInventory (Ground)
            // If activeContainer is null, vicinityProxy delegates to vicinityInventory (Ground)
            // This perfectly handles "Show Ground Items in empty slots"
            final int currentSlotIndex = addedSlots;
            this.addSlot(new Slot(this.vicinityProxy, currentSlotIndex, x, y) {
                 @Override
                 public boolean isActive() {
                     return this.x > -1000; 
                 }
                 @Override
                 public boolean mayPlace(ItemStack stack) {
                    if (DayZInventoryMenu.this.activeContainer != null) {
                        int containerIndex = DayZInventoryMenu.this.mapToActiveContainerIndex(currentSlotIndex);
                        if (containerIndex < DayZInventoryMenu.this.activeContainer.getContainerSize()) {
                            return DayZInventoryMenu.this.activeContainer.canPlaceItem(containerIndex, stack);
                        }
                     }
                     return true;
                 }
                 @Override
                 public void set(ItemStack stack) {
                     // Drop item if it's a ground slot (not in active container)
                    if (DayZInventoryMenu.this.activeContainer == null || DayZInventoryMenu.this.mapToActiveContainerIndex(currentSlotIndex) >= DayZInventoryMenu.this.activeContainer.getContainerSize()) {
                        if (!stack.isEmpty() && !DayZInventoryMenu.this.player.level().isClientSide) {
                            DayZInventoryMenu.this.player.drop(stack, false);
                        }
                     }
                     super.set(stack);
                 }
            });
            addedSlots++;
        }
    }

    private Slot createCorpseEquipmentSlot(int containerIndex, int x, int y) {
        return new Slot(this.vicinityProxy, containerIndex, x, y) {
            @Override
            public boolean isActive() {
                return this.x > -1000;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.isEmpty()) return false;
                return switch (containerIndex) {
                    case 0 -> isBackpackItemValid(stack) || (stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.BACKPACK);
                    case 1 -> stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.VEST;
                    case 2 -> canEquip(stack, EquipmentSlot.CHEST) || (stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.SHIRT);
                    case 3 -> canEquip(stack, EquipmentSlot.LEGS) || (stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.PANTS);
                    case 4 -> canEquip(stack, EquipmentSlot.HEAD) || (stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.HAT);
                    case 5 -> canEquip(stack, EquipmentSlot.FEET) || (stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.SHOES);
                    case 6 -> stack.getMaxStackSize() == 1;
                    case 7 -> stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.MASK;
                    case 8 -> stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.GLOVES;
                    default -> false;
                };
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                boolean updatesStorage = containerIndex <= 3;
                if (updatesStorage && !player.level().isClientSide) {
                    saveCorpseStorageToItems();
                }
                super.onTake(player, stack);
                if (updatesStorage && !player.level().isClientSide) {
                    loadCorpseStorageFromItems();
                    updateSlotPositions();
                    broadcastChanges();
                }
            }

            @Override
            public void set(ItemStack stack) {
                boolean updatesStorage = containerIndex <= 3;
                if (updatesStorage && !isCorpseLoading && this.hasItem() && !player.level().isClientSide) {
                    saveCorpseStorageToItems();
                }
                super.set(stack);
                if (updatesStorage && !isCorpseLoading) {
                    loadCorpseStorageFromItems();
                    updateSlotPositions();
                    broadcastChanges();
                }
            }
        };
    }

    private void addEquipmentSlots(Inventory inv) {
        // 30: Headgear (Vanilla Helmet + Custom Hat)
        this.addSlot(new Slot(inv, 39, UIConstants.SLOT_HEADGEAR_X, UIConstants.SLOT_HEADGEAR_Y) {
            @Override 
            public boolean mayPlace(ItemStack stack) {
                if (canEquip(stack, EquipmentSlot.HEAD)) return true;
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.HAT;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 31: Shirt (Vanilla Chestplate + Custom Shirt)
        this.addSlot(new Slot(inv, 38, UIConstants.SLOT_SHIRT_X, UIConstants.SLOT_SHIRT_Y) {
            @Override 
            public boolean mayPlace(ItemStack stack) {
                if (canEquip(stack, EquipmentSlot.CHEST)) return true;
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.SHIRT;
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public void onTake(Player player, ItemStack stack) {
                if (!player.level().isClientSide && !suppressDrop) {
                     dropShirtItems(player, stack);
                }
                super.onTake(player, stack);
                saveBackpackToItem(); // Save before clearing
                loadBackpackFromItem();
                updateSlotPositions();
                broadcastChanges();
            }
            @Override
            public void set(ItemStack stack) {
                if (!isLoading) {
                    if (this.hasItem() && !player.level().isClientSide) {
                        ItemStack current = this.getItem();
                        dropShirtItems(player, current);
                    }
                    saveBackpackToItem(); // Saves empty to Old Shirt (clears NBT)
                }
                super.set(stack);
                if (!isLoading) {
                    loadBackpackFromItem();
                    suppressDrop = true; // Prevent onTake from dropping/stripping again
                    updateSlotPositions();
                    broadcastChanges();
                }
            }
        });

        // 32: Pants (Vanilla Leggings + Custom Pants)
        this.addSlot(new Slot(inv, 37, UIConstants.SLOT_PANTS_X, UIConstants.SLOT_PANTS_Y) {
            @Override 
            public boolean mayPlace(ItemStack stack) {
                if (canEquip(stack, EquipmentSlot.LEGS)) return true;
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.PANTS;
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public void onTake(Player player, ItemStack stack) {
                if (!player.level().isClientSide && !suppressDrop) {
                     dropPantsItems(player, stack);
                }
                super.onTake(player, stack);
                saveBackpackToItem(); // Save before clearing
                loadBackpackFromItem();
                updateSlotPositions();
                broadcastChanges();
            }
            @Override
            public void set(ItemStack stack) {
                if (!isLoading) {
                    if (this.hasItem() && !player.level().isClientSide) {
                        ItemStack current = this.getItem();
                        dropPantsItems(player, current);
                    }
                    saveBackpackToItem(); // Saves empty to Old Pants (clears NBT)
                }
                super.set(stack);
                if (!isLoading) {
                    loadBackpackFromItem();
                    suppressDrop = true; // Prevent onTake from dropping/stripping again
                    updateSlotPositions();
                    broadcastChanges();
                }
            }
        });

        // 33: Shoes (Vanilla Boots + Custom Shoes)
        this.addSlot(new Slot(inv, 36, UIConstants.SLOT_SHOES_X, UIConstants.SLOT_SHOES_Y) {
            @Override 
            public boolean mayPlace(ItemStack stack) {
                if (canEquip(stack, EquipmentSlot.FEET)) return true;
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.SHOES;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 34: Offhand
        this.addSlot(new Slot(inv, 40, UIConstants.OFFHAND_X, UIConstants.OFFHAND_Y));

        // Capability Slots (必须始终添加，否则索引会偏移导致崩溃)
        IItemHandler capHandler = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(PlayerBackpack::getInventory)
                .orElse(new ItemStackHandler(PlayerBackpack.SLOT_COUNT));

        // 35: Backpack
        this.addSlot(new SlotItemHandler(capHandler, PlayerBackpack.SLOT_BACKPACK, UIConstants.BACKPACK_EQUIP_X, UIConstants.BACKPACK_EQUIP_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BackpackItem || stack.is(BACKPACKS);
            }
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public void set(ItemStack stack) {
                if (!isLoading) saveBackpackToItem();
                super.set(stack);
                if (!isLoading) {
                    loadBackpackFromItem();
                    syncSlot(PlayerBackpack.SLOT_BACKPACK, stack);
                    updateSlotPositions();
                    broadcastChanges();
                }
            }
            @Override
            public void onTake(Player player, ItemStack stack) {
                saveBackpackToItem(stack, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
                super.onTake(player, stack);
                loadBackpackFromItem();
                syncSlot(PlayerBackpack.SLOT_BACKPACK, ItemStack.EMPTY);
                updateSlotPositions();
                broadcastChanges();
            }
            @Override
            public void setChanged() {
                super.setChanged();
                syncSlot(PlayerBackpack.SLOT_BACKPACK, this.getItem());
            }
        });

        // 36: Vest
        this.addSlot(new SlotItemHandler(capHandler, PlayerBackpack.SLOT_VEST, UIConstants.SLOT_VEST_X, UIConstants.SLOT_VEST_Y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.VEST;
            }
            @Override
            public void set(ItemStack stack) {
                if (!isLoading) saveBackpackToItem();
                super.set(stack);
                if (!isLoading) {
                    loadBackpackFromItem();
                    syncSlot(PlayerBackpack.SLOT_VEST, stack);
                    updateSlotPositions();
                    broadcastChanges();
                }
            }
            @Override
            public void onTake(Player player, ItemStack stack) {
                saveBackpackToItem(ItemStack.EMPTY, stack, ItemStack.EMPTY, ItemStack.EMPTY);
                super.onTake(player, stack);
                loadBackpackFromItem();
                syncSlot(PlayerBackpack.SLOT_VEST, ItemStack.EMPTY);
                updateSlotPositions();
                broadcastChanges();
            }
            @Override
            public void setChanged() {
                super.setChanged();
                syncSlot(PlayerBackpack.SLOT_VEST, this.getItem());
            }
        });

        // 37: Gloves
        this.addSlot(new SlotItemHandler(capHandler, PlayerBackpack.SLOT_GLOVES, UIConstants.SLOT_GLOVES_X, UIConstants.SLOT_GLOVES_Y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.GLOVES;
            }
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                syncSlot(PlayerBackpack.SLOT_GLOVES, stack);
            }
            @Override
            public void setChanged() {
                super.setChanged();
                syncSlot(PlayerBackpack.SLOT_GLOVES, this.getItem());
            }
        });

        // 38: Mask (Also allows Hats)
        this.addSlot(new SlotItemHandler(capHandler, PlayerBackpack.SLOT_MASK, UIConstants.SLOT_MASK_X, UIConstants.SLOT_MASK_Y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (canEquip(stack, EquipmentSlot.HEAD)) return true;
                if (!(stack.getItem() instanceof ClothingItem c)) return false;
                return c.getType() == ClothingItem.ClothingType.MASK || c.getType() == ClothingItem.ClothingType.HAT;
            }
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                syncSlot(PlayerBackpack.SLOT_MASK, stack);
            }
            @Override
            public void setChanged() {
                super.setChanged();
                syncSlot(PlayerBackpack.SLOT_MASK, this.getItem());
            }
        });
    }

    private void syncSlot(int slotId, ItemStack stack) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            try {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), 
                    new SyncBackpackS2C(slotId, stack));
            } catch (Exception e) {
                BlockZ.LOGGER.error("Failed to sync slot " + slotId, e);
            }
        }
    }

    public net.minecraft.world.Container getActiveContainer() {
        return this.activeContainer;
    }

    private BlockPos lastContainerPos = null;
    private UUID lastLootrId = null;
    private boolean isVicinityDirty = false;
    private long lastVicinityUpdateTime = 0;
    
    public void updateVicinityItems(Player player) {
        if (player.level().isClientSide) return;
        long currentTime = System.currentTimeMillis();
        if (!isVicinityDirty && currentTime - lastVicinityUpdateTime < 500) {
            return;
        }
        lastVicinityUpdateTime = currentTime;
        isVicinityDirty = false;
        int slotIndex = 0;
        if (this.activeContainer != null) {
            if (supportsContainerPaging()) {
                this.containerPage = clampContainerPage(this.containerPage);
            } else {
                this.containerPage = 0;
            }
            int size = this.activeContainer.getContainerSize();
            int offset = getContainerPageOffset();
            int remaining = Math.max(0, size - offset);
            int toFill = Math.min(VICINITY_SLOTS, remaining);
            for (int i = 0; i < toFill; i++) {
                this.vicinityInventory.setItem(slotIndex, this.activeContainer.getItem(offset + i));
                slotIndex++;
            }
            while (slotIndex < VICINITY_SLOTS) {
                if (!this.vicinityInventory.getItem(slotIndex).isEmpty()) {
                    this.vicinityInventory.setItem(slotIndex, ItemStack.EMPTY);
                }
                slotIndex++;
            }
            return;
        }
        VicinityManager.fillGroundItems(this.vicinityInventory, player, this.nearbyEntities, VICINITY_SLOTS);
    }

    public boolean hasBackpack() {
        boolean hasBackpackOrVest = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> !cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK).isEmpty() || !cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST).isEmpty())
                .orElse(false);
        if (hasBackpackOrVest) return true;

        // Check Shirt and Pants
        ItemStack shirt = this.player.getInventory().getArmor(2);
        if (BlockZConfigs.getBackpackSlots(shirt) > 0) return true;

        ItemStack pants = this.player.getInventory().getArmor(1);
        if (BlockZConfigs.getBackpackSlots(pants) > 0) return true;

        return false;
    }

    /**
     * 获取当前装备背包和背心提供的额外格子数
     */
    public int getBackpackCapacity() {
        int bpCap = 0;
        int vestCap = 0;
        if (this.player != null) {
            bpCap = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                    .map(cap -> BlockZConfigs.getBackpackSlots(cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK)))
                    .orElse(0);
            vestCap = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                    .map(cap -> BlockZConfigs.getBackpackSlots(cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST)))
                    .orElse(0);
        }

        ItemStack shirt = this.player.getInventory().getArmor(2);
        ItemStack pants = this.player.getInventory().getArmor(1);
        int shirtCap = BlockZConfigs.getBackpackSlots(shirt);
        int pantsCap = BlockZConfigs.getBackpackSlots(pants);

        int[] safeCaps = clampBackpackCaps(bpCap, vestCap, shirtCap, pantsCap);
        return safeCaps[0] + safeCaps[1] + safeCaps[2] + safeCaps[3];
    }

    private int getBackpackGridSlots() {
        return this.backpackContentHandler.getSlots();
    }

    private int[] clampBackpackCaps(int bpCap, int vestCap, int shirtCap, int pantsCap) {
        int maxSlots = getBackpackGridSlots();
        int remaining = maxSlots;
        int safeBp = Math.min(bpCap, remaining);
        remaining -= safeBp;
        int safeVest = Math.min(vestCap, remaining);
        remaining -= safeVest;
        int safeShirt = Math.min(shirtCap, remaining);
        remaining -= safeShirt;
        int safePants = Math.min(pantsCap, remaining);
        return new int[]{safeBp, safeVest, safeShirt, safePants};
    }

    /**
     * 获取指定索引处的物品锚点索引 (用于 Tetris 物品)
     */
    private int getAnchorSlot(int handlerIndex) {
        if (handlerIndex < 0 || handlerIndex >= this.backpackContentHandler.getSlots()) return -1;
        
        // 检查当前槽位是否已有物品，如果有，它就是锚点
        if (!this.backpackContentHandler.getStackInSlot(handlerIndex).isEmpty()) return handlerIndex;
        
        // 否则，遍历之前的所有槽位，看是否有大物品覆盖了这里
        // 注意：不同分区可能有不同列数（cap_width），不能写死 5 列。
        int cols = getSectionColsForHandlerIndex(handlerIndex);
        int row = handlerIndex / cols;
        int col = handlerIndex % cols;
        
        int searchBack = UIConstants.INVENTORY_MAX_COLS;
        for (int r = Math.max(0, row - searchBack); r <= row; r++) {
            for (int c = Math.max(0, col - searchBack); c <= col; c++) {
                int checkIdx = r * cols + c;
                if (checkIdx >= handlerIndex) continue;
                if (checkIdx < 0 || checkIdx >= this.backpackContentHandler.getSlots()) continue;
                
                ItemStack stack = this.backpackContentHandler.getStackInSlot(checkIdx);
                if (stack.isEmpty()) continue;
                
                ItemSizeManager.ItemSize size = ItemSizeManager.getSize(stack);
                if (col >= c && col < c + size.width() && row >= r && row < r + size.height()) {
                    return checkIdx;
                }
            }
        }
        
        return -1;
    }

    private int getSectionColsForHandlerIndex(int handlerIndex) {
        // 按当前可用容量分段判断所属分区（与 updateSlotPositions 的 offset 划分保持一致）
        ItemStack backpackStack = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK))
                .orElse(ItemStack.EMPTY);
        ItemStack vestStack = this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK)
                .map(cap -> cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST))
                .orElse(ItemStack.EMPTY);
        ItemStack shirtStack = this.player.getInventory().getArmor(2);
        ItemStack pantsStack = this.player.getInventory().getArmor(1);

        int bpCap = BlockZConfigs.getBackpackSlots(backpackStack);
        int vestCap = BlockZConfigs.getBackpackSlots(vestStack);
        int shirtCap = BlockZConfigs.getBackpackSlots(shirtStack);
        int pantsCap = BlockZConfigs.getBackpackSlots(pantsStack);
        int[] safeCaps = clampBackpackCaps(bpCap, vestCap, shirtCap, pantsCap);
        bpCap = safeCaps[0];
        vestCap = safeCaps[1];
        shirtCap = safeCaps[2];
        pantsCap = safeCaps[3];

        int backpackOffset = 0;
        int vestOffset = backpackOffset + bpCap;
        int shirtOffset = vestOffset + vestCap;
        int pantsOffset = shirtOffset + shirtCap;

        if (handlerIndex >= pantsOffset && handlerIndex < pantsOffset + pantsCap) return this.pantsSectionCols;
        if (handlerIndex >= shirtOffset && handlerIndex < shirtOffset + shirtCap) return this.shirtSectionCols;
        if (handlerIndex >= vestOffset && handlerIndex < vestOffset + vestCap) return this.vestSectionCols;
        if (handlerIndex >= backpackOffset && handlerIndex < backpackOffset + bpCap) return this.backpackSectionCols;

        // fallback
        return UIConstants.INVENTORY_COLS;
    }

    public int getPocketCount() {
        if (syncedPocketCount != -1) return syncedPocketCount;
        return BlockZConfigs.initialPocketSlots.get();
    }

    public int getBackpackSlotStart() {
        return getPocketStart() + getPocketCount();
    }

    public int getBackpackSlotEnd() {
        return getBackpackSlotStart() + getBackpackGridSlots() - 1;
    }

    /**
     * 当前右侧物品栏需要的最大列数（用于 UI 动态扩展宽度）。
     */
    public int getInventoryMaxCols() {
        int max = UIConstants.INVENTORY_COLS;
        max = Math.max(max, this.backpackSectionCols);
        max = Math.max(max, this.vestSectionCols);
        max = Math.max(max, this.shirtSectionCols);
        max = Math.max(max, this.pantsSectionCols);
        return max;
    }

    private boolean isGroundVicinityMode() {
        return !this.isWorkbench && !this.isEnchantingTable && this.activeContainer == null;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // 1. 如果处于锁定模式，禁止操作扩展物品栏
        // 39-XX 是口袋（对应原版 9-XX 槽位），不属于“多余”格子
        if (!player.level().isClientSide && slotId >= getBackpackSlotStart() && slotId <= getBackpackSlotEnd() && this.isLockedMode) {
            // 如果格子里已经有物品，强制丢出来 (这可能发生在管理员关闭 DayZ UI 后)
            Slot slot = this.getSlot(slotId);
            if (slot.hasItem() && !(slot instanceof LockedSlot)) {
                player.drop(slot.getItem(), true);
                slot.set(ItemStack.EMPTY);
            }
            return;
        }

        this.suppressDrop = false; // Reset flag
        // 在进行任何操作之前，先保存当前背包状态
        // 这对于 Swap 操作 (快捷键换装) 尤为重要，确保旧装备的 NBT 被保存
        if (!player.level().isClientSide) {
            saveBackpackToItem();
        }

        // 处理 Vicinity 槽位的点击 (索引 0-53)
        // 注意：VICINITY_SLOTS 必须与 addVicinitySlots 中的数量一致
        if (slotId >= 0 && slotId < VICINITY_SLOTS) {
            // 如果是工作台，直接使用默认逻辑 (允许标准交互)
            if (this.isWorkbench) {
                super.clicked(slotId, button, clickType, player);
                saveBackpackToItem();
                return;
            }

            if (clickType == ClickType.QUICK_CRAFT) {
                super.clicked(slotId, button, clickType, player);
                saveBackpackToItem();
                return;
            }

            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            
            // 如果是尝试放置物品
            if (!carried.isEmpty()) {
                // 1. 真实容器槽位：完全交给原版逻辑处理堆叠/交换，我们只做背包保存
                if (this.activeContainer != null && mapToActiveContainerIndex(slotId) < this.activeContainer.getContainerSize()) {
                    super.clicked(slotId, button, clickType, player);
                    saveBackpackToItem();
                    return;
                }

                int containerSize = (this.activeContainer != null) ? this.activeContainer.getContainerSize() : 0;

                if (isGroundVicinityMode()) {
                    ItemStack toDrop;
                    if (button == 1) {
                        toDrop = carried.split(1);
                    } else {
                        toDrop = carried.copy();
                        carried.setCount(0);
                    }

                    if (!player.level().isClientSide) {
                        player.drop(toDrop, true);
                        this.markVicinityDirty();
                    }

                    this.setCarried(carried);
                    this.broadcastChanges();
                    saveBackpackToItem();
                    return;
                }

                if (slot.mayPlace(carried)) {
                    // 只有当槽位有物品时才允许交换 (因为实体已存在)
                    if (slotId >= containerSize && slot.hasItem()) {
                         super.clicked(slotId, button, clickType, player);
                         
                         // 同步实体
                         int entityIndex = slotId - containerSize;
                         if (entityIndex >= 0 && entityIndex < this.nearbyEntities.size()) {
                             ItemEntity entity = this.nearbyEntities.get(entityIndex);
                             if (entity != null && entity.isAlive()) {
                                 entity.setItem(slot.getItem().copy());
                             }
                         }
                         this.broadcastChanges();
                         saveBackpackToItem();
                         return;
                    }
                    
                    // 3. 地面物品：放置 (放入空位 -> 丢弃到世界)
                    if (slotId >= containerSize && !slot.hasItem()) {
                        // 放置行为：将物品丢弃到世界
                        ItemStack toDrop;
                        if (button == 1) { // Right Click - Drop 1
                             toDrop = carried.split(1);
                        } else { // Left Click - Drop All
                             toDrop = carried.copy();
                             carried.setCount(0);
                        }
                        
                        if (!player.level().isClientSide) {
                             player.drop(toDrop, true);
                             this.markVicinityDirty();
                        }
                        
                        this.setCarried(carried);
                        this.broadcastChanges();
                        saveBackpackToItem();
                        return;
                    }
                }
                return;
            }

            // 如果是尝试取走物品
            if (slot.hasItem() && carried.isEmpty()) {
                // 判断是否是容器内的物品 (Real Container)
                int containerSize = (this.activeContainer != null) ? this.activeContainer.getContainerSize() : 0;
                
                int containerIndex = mapToActiveContainerIndex(slotId);
                if (containerIndex < containerSize) {
                    // 真实容器槽位：使用默认逻辑 (允许拖拽、Shift点击等)
                    super.clicked(slotId, button, clickType, player);

                    // Sync back to activeContainer manually to prevent desync/flickering
                    if (this.activeContainer != null) {
                        ItemStack stack = this.slots.get(slotId).getItem();
                        this.activeContainer.setItem(containerIndex, stack);
                        this.activeContainer.setChanged();
                    }
                    saveBackpackToItem(); // 额外保存一次
                    return;
                } else {
                    // 地面掉落物
                    // 1. 快速拾取：仅 Shift+左键 (QUICK_MOVE)
                    //    普通左键点击保持“拿到鼠标上”以支持拖动
                    if (clickType == ClickType.QUICK_MOVE) {
                        ItemStack stack = slot.getItem();
                        boolean added;

                        if (this.isLockedMode) {
                            added = player.getInventory().add(stack);
                        } else {
                            // DayZ 模式：限制只能捡起到快捷栏(0-8)和口袋(9-13)
                            added = InventoryUtils.addItemToDayZInventory(player.getInventory(), stack);
                        }

                        if (added) {
                            int entityIndex = slotId - containerSize;
                            if (entityIndex >= 0 && entityIndex < this.nearbyEntities.size()) {
                                ItemEntity entity = this.nearbyEntities.get(entityIndex);
                                if (entity != null && entity.isAlive()) {
                                    if (stack.isEmpty()) {
                                        entity.discard();
                                    } else {
                                        entity.setItem(stack.copy());
                                    }
                                }
                            }
                            slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                            this.broadcastChanges();
                        }
                    }
                    // 2. 如果是普通点击 (PICKUP) -> 拿到鼠标上，支持左键/右键拖动
                    else if (clickType == ClickType.PICKUP) {
                        super.clicked(slotId, button, clickType, player);
                        
                        // 必须手动同步状态到实体，因为 Vicinity 槽位 (SimpleContainer) 不会自动同步到 ItemEntity
                        int entityIndex = slotId - containerSize;
                        if (entityIndex >= 0 && entityIndex < this.nearbyEntities.size()) {
                            ItemEntity entity = this.nearbyEntities.get(entityIndex);
                            if (entity != null && entity.isAlive()) {
                                ItemStack newStack = slot.getItem();
                                if (newStack.isEmpty()) {
                                    entity.discard();
                                } else {
                                    entity.setItem(newStack.copy());
                                }
                            }
                        }
                        this.broadcastChanges();
                     }
                     // 3. 如果是丢弃 (THROW - Q键) -> 将地面物品丢弃到玩家脚下 (搬运)
                     else if (clickType == ClickType.THROW) {
                         ItemStack stack = slot.getItem();
                         int count = (button == 0) ? 1 : stack.getCount(); // 0=DropOne, 1=DropAll
                         
                         ItemStack toDrop = stack.split(count);
                         if (!player.level().isClientSide) {
                             player.drop(toDrop, true);
                         }
                         
                         // 同步实体状态
                         int entityIndex = slotId - containerSize;
                         if (entityIndex >= 0 && entityIndex < this.nearbyEntities.size()) {
                             ItemEntity entity = this.nearbyEntities.get(entityIndex);
                             if (entity != null && entity.isAlive()) {
                                 if (stack.isEmpty()) {
                                     entity.discard();
                                 } else {
                                     entity.setItem(stack.copy());
                                 }
                             }
                         }
                         
                         if (stack.isEmpty()) {
                             slot.set(ItemStack.EMPTY);
                         }
                         this.broadcastChanges();
                     }
                     
                     saveBackpackToItem(); // 额外保存一次
                     return;
            }
        }
    }
        
        // 检查点击的槽位是否属于被锁定的背包区域
        if (slotId >= getBackpackSlotStart() && slotId <= getBackpackSlotEnd()) { 
            if (!hasBackpack()) {
                return; 
            }
            
            // Tetris 逻辑: 如果点击的是空位，检查是否是被覆盖的子区域
            int handlerIndex = slotId - getBackpackSlotStart();
            ItemStack carried = this.getCarried();

            // Case 1: Cursor Empty - Forward click to anchor if hitting a "fake" slot
            if (this.backpackContentHandler.getStackInSlot(handlerIndex).isEmpty() && carried.isEmpty()) {
                int anchor = getAnchorSlot(handlerIndex);
                if (anchor != -1 && anchor != handlerIndex) {
                    // 转发点击到锚点槽位
                    super.clicked(anchor + getBackpackSlotStart(), button, clickType, player);
                    saveBackpackToItem(); // 额外保存一次
                    return;
                }
            }

            // Case 2: Cursor Has Item - Try Swap
            // 如果鼠标有物品，且点击的位置有物品（或是被占用的格子），尝试交换
            if (!carried.isEmpty()) {
                int anchor = getAnchorSlot(handlerIndex);
                // anchor != -1 意味着该格子被占用 (要么是锚点本身，要么是被覆盖)
                if (anchor != -1) {
                    ItemStack existingItem = this.backpackContentHandler.getStackInSlot(anchor);
                    
                    // 检查是否可以堆叠 (Stacking Fix)
                    if (!existingItem.isEmpty() && ItemStack.isSameItemSameTags(carried, existingItem)) {
                         // 转发点击到锚点槽位，让原版逻辑处理堆叠
                         super.clicked(anchor + getBackpackSlotStart(), button, clickType, player);
                         saveBackpackToItem();
                         return;
                    }

                    if (!existingItem.isEmpty()) {
                        // 尝试交换：
                        // 1. 临时移除原有物品
                        this.backpackContentHandler.setStackInSlot(anchor, ItemStack.EMPTY);
                        
                        // 2. 检查当前手持物品能否放入点击的位置
                        // 注意：我们尝试放入的是 clickedSlot (即 handlerIndex)
                        // 这允许大换小 (放入点击位置)，或小换大 (需满足空间要求)
                        Slot clickedSlot = this.getSlot(slotId);
                        boolean canFit = clickedSlot.mayPlace(carried);
                        
                        // 3. 根据结果执行交换或还原
                        if (canFit) {
                            // 执行交换
                            // 将手持物品放入点击的格子 (这会自动更新 handler)
                            clickedSlot.set(carried);
                            
                            // 将原物品拿在手上
                            this.setCarried(existingItem);
                            
                            // 标记更新
                            saveBackpackToItem();
                            return;
                        } else {
                            // 还原原有物品
                            this.backpackContentHandler.setStackInSlot(anchor, existingItem);
                        }
                    }
                }
            }
        }

        super.clicked(slotId, button, clickType, player);
        // 任何点击后都尝试保存一次，确保万无一失
        if (!player.level().isClientSide) {
            saveBackpackToItem();
        }
    }

    private int tickCount = 0;

    @Override
    public void broadcastChanges() {
        // 只有在服务端才执行更新
        if (this.player instanceof ServerPlayer) {
            if (++tickCount % 10 == 0 || isVicinityDirty || corpseStorageDirty) { // 每 10 tick (0.5秒) 更新一次附近物品，或者被标记为 dirty
                updateVicinityItems(this.player);
                isVicinityDirty = false;
            }
            if (corpseStorageDirty) {
                saveCorpseStorageToItems();
                corpseStorageDirty = false;
            }
        }
        
        super.broadcastChanges();
    }

    public void markVicinityDirty() {
        this.isVicinityDirty = true;
    }

    public void stillValidUpdate(Player player) {
        if (!player.level().isClientSide) {
            updateVicinityItems(player);
        }
    }

    private void addHotbarSlots(Inventory inv) {
        // 快捷栏槽位 (参考 DayM 布局)
        for (int i = 0; i < 9; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = UIConstants.HOTBAR_X + 4 + col * UIConstants.SLOT_PITCH;
            int y = UIConstants.HOTBAR_Y + 10 + row * UIConstants.SLOT_PITCH;
            this.addSlot(new Slot(inv, i, x, y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.containerEntity != null) {
            if (this.containerEntity.isRemoved()) return false;
            return player.distanceToSqr(this.containerEntity) <= 64.0D;
        }
        if (this.containerPos != null) {
            BlockEntity be = player.level().getBlockEntity(this.containerPos);
            if (be != null) {
                return Container.stillValidBlockEntity(be, player);
            }
            // 如果方块实体不存在，则仅基于距离校验，避免错误的类型转换导致崩溃
            double dx = this.containerPos.getX() + 0.5D;
            double dy = this.containerPos.getY() + 0.5D;
            double dz = this.containerPos.getZ() + 0.5D;
            return player.distanceToSqr(dx, dy, dz) <= 64.0D;
        }
        return true;
    }

    private boolean isBackpackNotEmpty(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.hasTag()) return false;
        if (!stack.getTag().contains("Inventory")) return false;
        net.minecraft.nbt.CompoundTag invTag = stack.getTag().getCompound("Inventory");
        if (!invTag.contains("Items")) return false;
        return !invTag.getList("Items", 10).isEmpty();
    }

    private boolean isBackpackItemValid(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem || stack.is(BACKPACKS);
    }
    
    private boolean isBackpackNested(ItemStack stack) {
        return isBackpackItemValid(stack) && isBackpackNotEmpty(stack);
    }

    private void dropClothingItems(Player player, ItemStack clothingStack, int startOffset, int cap) {
        if (cap <= 0) return;
        boolean droppedFromHandler = false;
        for (int i = 0; i < cap; i++) {
            int idx = startOffset + i;
            if (idx < this.backpackContentHandler.getSlots()) {
                ItemStack stack = this.backpackContentHandler.getStackInSlot(idx);
                if (!stack.isEmpty()) {
                    player.drop(stack, true);
                    this.backpackContentHandler.setStackInSlot(idx, ItemStack.EMPTY);
                    droppedFromHandler = true;
                }
            }
        }
        if (!droppedFromHandler && !clothingStack.isEmpty() && clothingStack.hasTag()) {
            net.minecraft.nbt.CompoundTag tag = clothingStack.getTag();
            if (tag != null && tag.contains("Inventory")) {
                ItemStackHandler handler = new ItemStackHandler(cap);
                handler.deserializeNBT(tag.getCompound("Inventory"));
                for (int i = 0; i < cap; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        player.drop(stack, true);
                    }
                }
            }
        }
        if (!clothingStack.isEmpty()) {
            net.minecraft.nbt.CompoundTag tag = clothingStack.getTag();
            if (tag != null) {
                tag.remove("Inventory");
            }
        }
    }

    private void dropShirtItems(Player player, ItemStack shirtStack) {
        int cap = Math.max(this.lastShirtCap, BlockZConfigs.getBackpackSlots(shirtStack));
        if (cap <= 0) return;
        int startOffset = this.lastBackpackCap + this.lastVestCap;
        dropClothingItems(player, shirtStack, startOffset, cap);
    }

    private void dropPantsItems(Player player, ItemStack pantsStack) {
        int cap = Math.max(this.lastPantsCap, BlockZConfigs.getBackpackSlots(pantsStack));
        if (cap <= 0) return;
        int startOffset = this.lastBackpackCap + this.lastVestCap + this.lastShirtCap;
        dropClothingItems(player, pantsStack, startOffset, cap);
    }

    private void addMainInventorySlots(Inventory inv) {
        int pocketCount = getPocketCount();
        int totalSlots = pocketCount + getBackpackGridSlots();
        
        for (int i = 0; i < totalSlots; i++) {
            int row = i / UIConstants.INVENTORY_COLS;
            int col = i % UIConstants.INVENTORY_COLS;
            int x = UIConstants.INVENTORY_SLOTS_X + col * UIConstants.SLOT_PITCH;
            int y = UIConstants.INVENTORY_SLOTS_Y + row * UIConstants.SLOT_PITCH;
            
            final int slotIdx = i;
            if (slotIdx < pocketCount) {
                // 口袋，对应玩家物品栏索引 9-XX
                // 口袋限制：不能放有物品的背包
                this.addSlot(new Slot(inv, 9 + slotIdx, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (isBackpackNested(stack)) return false; // 禁止套娃
                        return super.mayPlace(stack);
                    }
                });
            } else {
                // 后面的对应背包和背心内容
                if (this.isLockedMode) {
                    // 如果处于锁定模式，使用 LockedSlot 填充空间
                    this.addSlot(new LockedSlot(x, y));
                } else {
                    this.addSlot(new TetrisSlot(
                            this.backpackContentHandler,
                            slotIdx - pocketCount,
                            x,
                            y,
                            UIConstants.INVENTORY_COLS,
                            this::getBackpackCapacity,
                            this::isBackpackNested
                    ));
                }
            }
        }
    }

    /**
     * 锁定槽位：始终显示锁定图标，不允许放置或取出物品
     */
    private static class LockedSlot extends Slot {
        private static final Container DUMMY_CONTAINER = new SimpleContainer(1);

        public LockedSlot(int x, int y) {
            super(DUMMY_CONTAINER, 0, x, y);
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(ModItems.LOCK_ITEM.get());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public void set(ItemStack stack) {
            // 禁止设置物品
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }

    private void saveBackpackToItem() {
        saveBackpackToItem(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    private void saveBackpackToItem(ItemStack overrideBackpack, ItemStack overrideVest, ItemStack overrideShirt, ItemStack overridePants) {
        if (this.player == null) return;
        
        this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStack backpackStack = !overrideBackpack.isEmpty() ? overrideBackpack : cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK);
            ItemStack vestStack = !overrideVest.isEmpty() ? overrideVest : cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST);
            
            ItemStack shirtStack = !overrideShirt.isEmpty() ? overrideShirt : this.player.getInventory().getArmor(2);
            ItemStack pantsStack = !overridePants.isEmpty() ? overridePants : this.player.getInventory().getArmor(1);
            
            // Use LAST known capacities to determine offsets, NOT current item capacities
            // This ensures we map the correct handler slots to the correct items, even if an item was removed
            int currentOffset = 0;
            
            // 1. Backpack
            if (lastBackpackCap > 0) {
                if (!backpackStack.isEmpty()) {
                    ItemStackHandler bpHandler = new ItemStackHandler(lastBackpackCap);
                    boolean hasItems = false;
                    for (int i = 0; i < lastBackpackCap; i++) {
                        if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                            ItemStack s = this.backpackContentHandler.getStackInSlot(currentOffset + i);
                            bpHandler.setStackInSlot(i, s);
                            if (!s.isEmpty()) hasItems = true;
                        }
                    }
                    
                    if (hasItems) {
                        backpackStack.getOrCreateTag().put("Inventory", bpHandler.serializeNBT());
                    } else {
                        net.minecraft.nbt.CompoundTag tag = backpackStack.getTag();
                        if (tag != null) {
                            tag.remove("Inventory");
                        }
                    }

                    // Only sync if it's the capability item (not override)
                    if (overrideBackpack.isEmpty()) {
                        cap.getInventory().setStackInSlot(PlayerBackpack.SLOT_BACKPACK, backpackStack);
                        syncSlot(PlayerBackpack.SLOT_BACKPACK, backpackStack);
                    }
                }
                currentOffset += lastBackpackCap;
            }
            
            // 2. Vest
            if (lastVestCap > 0) {
                if (!vestStack.isEmpty()) {
                    ItemStackHandler vestHandler = new ItemStackHandler(lastVestCap);
                    boolean hasItems = false;
                    for (int i = 0; i < lastVestCap; i++) {
                        if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                            ItemStack s = this.backpackContentHandler.getStackInSlot(currentOffset + i);
                            vestHandler.setStackInSlot(i, s);
                            if (!s.isEmpty()) hasItems = true;
                        }
                    }
                    
                    if (hasItems) {
                        vestStack.getOrCreateTag().put("Inventory", vestHandler.serializeNBT());
                    } else {
                        net.minecraft.nbt.CompoundTag tag = vestStack.getTag();
                        if (tag != null) {
                            tag.remove("Inventory");
                        }
                    }

                    if (overrideVest.isEmpty()) {
                        cap.getInventory().setStackInSlot(PlayerBackpack.SLOT_VEST, vestStack);
                        syncSlot(PlayerBackpack.SLOT_VEST, vestStack);
                    }
                }
                currentOffset += lastVestCap;
            }

            // 3. Shirt
            if (lastShirtCap > 0) {
                if (!shirtStack.isEmpty()) {
                    ItemStackHandler shirtHandler = new ItemStackHandler(lastShirtCap);
                    boolean hasItems = false;
                    for (int i = 0; i < lastShirtCap; i++) {
                        if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                            ItemStack s = this.backpackContentHandler.getStackInSlot(currentOffset + i);
                            shirtHandler.setStackInSlot(i, s);
                            if (!s.isEmpty()) hasItems = true;
                        }
                    }
                    
                    if (hasItems) {
                        shirtStack.getOrCreateTag().put("Inventory", shirtHandler.serializeNBT());
                    } else {
                        net.minecraft.nbt.CompoundTag tag = shirtStack.getTag();
                        if (tag != null) {
                            tag.remove("Inventory");
                        }
                    }
                }
                currentOffset += lastShirtCap;
            }

            // 4. Pants
            if (lastPantsCap > 0) {
                if (!pantsStack.isEmpty()) {
                    ItemStackHandler pantsHandler = new ItemStackHandler(lastPantsCap);
                    boolean hasItems = false;
                    for (int i = 0; i < lastPantsCap; i++) {
                        if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                            ItemStack s = this.backpackContentHandler.getStackInSlot(currentOffset + i);
                            pantsHandler.setStackInSlot(i, s);
                            if (!s.isEmpty()) hasItems = true;
                        }
                    }
                    
                    if (hasItems) {
                        pantsStack.getOrCreateTag().put("Inventory", pantsHandler.serializeNBT());
                    } else {
                        net.minecraft.nbt.CompoundTag tag = pantsStack.getTag();
                        if (tag != null) {
                            tag.remove("Inventory");
                        }
                    }
                }
                currentOffset += lastPantsCap;
            }
        });
    }

    private void loadBackpackFromItem() {
        if (this.player == null) return;
        this.isLoading = true;
        this.player.getCapability(PlayerBackpackProvider.PLAYER_BACKPACK).ifPresent(cap -> {
            ItemStack backpackStack = cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_BACKPACK);
            ItemStack vestStack = cap.getInventory().getStackInSlot(PlayerBackpack.SLOT_VEST);
            
            ItemStack shirtStack = this.player.getInventory().getArmor(2);
            ItemStack pantsStack = this.player.getInventory().getArmor(1);
            
            int bpCap = BlockZConfigs.getBackpackSlots(backpackStack);
            int vestCap = BlockZConfigs.getBackpackSlots(vestStack);
            int shirtCap = BlockZConfigs.getBackpackSlots(shirtStack);
            int pantsCap = BlockZConfigs.getBackpackSlots(pantsStack);

            int[] safeCaps = clampBackpackCaps(bpCap, vestCap, shirtCap, pantsCap);
            bpCap = safeCaps[0];
            vestCap = safeCaps[1];
            shirtCap = safeCaps[2];
            pantsCap = safeCaps[3];

            this.lastBackpackCap = bpCap;
            this.lastVestCap = vestCap;
            this.lastShirtCap = shirtCap;
            this.lastPantsCap = pantsCap;
            
            clearBackpackHandler();
            
            int currentOffset = 0;
            
            // 从背包加载
            if (bpCap > 0 && backpackStack.hasTag() && backpackStack.getTag().contains("Inventory")) {
                ItemStackHandler bpHandler = new ItemStackHandler(bpCap);
                bpHandler.deserializeNBT(backpackStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(bpCap, bpHandler.getSlots()); i++) {
                    if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                        this.backpackContentHandler.setStackInSlot(currentOffset + i, bpHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += bpCap;
            
            // 从背心加载
            if (vestCap > 0 && vestStack.hasTag() && vestStack.getTag().contains("Inventory")) {
                ItemStackHandler vestHandler = new ItemStackHandler(vestCap);
                vestHandler.deserializeNBT(vestStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(vestCap, vestHandler.getSlots()); i++) {
                    if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                        this.backpackContentHandler.setStackInSlot(currentOffset + i, vestHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += vestCap;

            // 从上衣加载
            if (shirtCap > 0 && shirtStack.hasTag() && shirtStack.getTag().contains("Inventory")) {
                ItemStackHandler shirtHandler = new ItemStackHandler(shirtCap);
                shirtHandler.deserializeNBT(shirtStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(shirtCap, shirtHandler.getSlots()); i++) {
                    if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                        this.backpackContentHandler.setStackInSlot(currentOffset + i, shirtHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += shirtCap;

            // 从裤子加载
            if (pantsCap > 0 && pantsStack.hasTag() && pantsStack.getTag().contains("Inventory")) {
                ItemStackHandler pantsHandler = new ItemStackHandler(pantsCap);
                pantsHandler.deserializeNBT(pantsStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(pantsCap, pantsHandler.getSlots()); i++) {
                    if (currentOffset + i < this.backpackContentHandler.getSlots()) {
                        this.backpackContentHandler.setStackInSlot(currentOffset + i, pantsHandler.getStackInSlot(i));
                    }
                }
            }
        });
        this.isLoading = false;
    }

    private void clearBackpackHandler() {
        for (int i = 0; i < this.backpackContentHandler.getSlots(); i++) {
            this.backpackContentHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    
    private void clearCorpseHandler() {
        for (int i = 0; i < this.corpseContentHandler.getSlots(); i++) {
            this.corpseContentHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    
    private void ensureCorpseStorageSlotsAdded() {
        if (corpseStorageSlotStart >= 0) return;
        corpseStorageSlotStart = this.slots.size();
        for (int i = 0; i < 45; i++) {
            this.addSlot(new TetrisSlot(
                    this.corpseContentHandler,
                    i,
                    -10000,
                    -10000,
                    UIConstants.INVENTORY_COLS,
                    this::getCorpseStorageCapacity,
                    this::isBackpackNested
            ));
        }
    }
    
    private int getCorpseBackpackSlots(ItemStack stack) {
        return BlockZConfigs.getBackpackSlots(stack);
    }
    
    private ItemStack getCorpseEquipmentStack(int containerIndex) {
        if (!(this.activeContainer instanceof CorpseEntity corpse)) return ItemStack.EMPTY;
        if (containerIndex < 0 || containerIndex >= corpse.getContainerSize()) return ItemStack.EMPTY;
        return corpse.getItem(containerIndex);
    }
    
    private void loadCorpseStorageFromItems() {
        if (!(this.activeContainer instanceof CorpseEntity corpse)) return;
        this.isCorpseLoading = true;
        
        ItemStack backpackStack = getCorpseEquipmentStack(0);
        ItemStack vestStack = getCorpseEquipmentStack(1);
        ItemStack shirtStack = getCorpseEquipmentStack(2);
        ItemStack pantsStack = getCorpseEquipmentStack(3);
        
        // Use default config, but do NOT move base pockets to Storage
        // Base Pockets stay in the Corpse Container slots 9-XX
        int bpCap = getCorpseBackpackSlots(backpackStack);
        int vestCap = getCorpseBackpackSlots(vestStack);
        int shirtCap = getCorpseBackpackSlots(shirtStack);
        int pantsCap = getCorpseBackpackSlots(pantsStack);
        
        this.lastCorpseBackpackCap = bpCap;
        this.lastCorpseVestCap = vestCap;
        this.lastCorpseShirtCap = shirtCap;
        this.lastCorpsePantsCap = pantsCap;
        this.corpseStorageCapacity = Math.min(45, bpCap + vestCap + shirtCap + pantsCap);
        
        clearCorpseHandler();
        
        int currentOffset = 0;
        
        if (bpCap > 0) {
            if (backpackStack.hasTag() && backpackStack.getTag().contains("Inventory")) {
                ItemStackHandler bpHandler = new ItemStackHandler(bpCap);
                bpHandler.deserializeNBT(backpackStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(bpCap, bpHandler.getSlots()); i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        this.corpseContentHandler.setStackInSlot(currentOffset + i, bpHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += bpCap;
        }
        
        if (vestCap > 0) {
            if (vestStack.hasTag() && vestStack.getTag().contains("Inventory")) {
                ItemStackHandler vestHandler = new ItemStackHandler(vestCap);
                vestHandler.deserializeNBT(vestStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(vestCap, vestHandler.getSlots()); i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        this.corpseContentHandler.setStackInSlot(currentOffset + i, vestHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += vestCap;
        }
        
        if (shirtCap > 0) {
            if (shirtStack.hasTag() && shirtStack.getTag().contains("Inventory")) {
                ItemStackHandler shirtHandler = new ItemStackHandler(shirtCap);
                shirtHandler.deserializeNBT(shirtStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(shirtCap, shirtHandler.getSlots()); i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        this.corpseContentHandler.setStackInSlot(currentOffset + i, shirtHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += shirtCap;
        }
        
        if (pantsCap > 0) {
            if (pantsStack.hasTag() && pantsStack.getTag().contains("Inventory")) {
                ItemStackHandler pantsHandler = new ItemStackHandler(pantsCap);
                pantsHandler.deserializeNBT(pantsStack.getTag().getCompound("Inventory"));
                for (int i = 0; i < Math.min(pantsCap, pantsHandler.getSlots()); i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        this.corpseContentHandler.setStackInSlot(currentOffset + i, pantsHandler.getStackInSlot(i));
                    }
                }
            }
            currentOffset += pantsCap;
        }
        
        corpseStorageDirty = false;
        this.isCorpseLoading = false;
    }
    
    private void saveCorpseStorageToItems() {
        if (!(this.activeContainer instanceof CorpseEntity corpse)) return;
        if (this.player == null) return;
        if (this.player.level().isClientSide) return;
        if (corpseStorageSlotStart < 0) return;
        
        ItemStack backpackStack = getCorpseEquipmentStack(0);
        ItemStack vestStack = getCorpseEquipmentStack(1);
        ItemStack shirtStack = getCorpseEquipmentStack(2);
        ItemStack pantsStack = getCorpseEquipmentStack(3);
        
        int currentOffset = 0;
        
        if (lastCorpseBackpackCap > 0) {
            if (!backpackStack.isEmpty()) {
                ItemStackHandler handler = new ItemStackHandler(lastCorpseBackpackCap);
                boolean hasItems = false;
                for (int i = 0; i < lastCorpseBackpackCap; i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        ItemStack s = this.corpseContentHandler.getStackInSlot(currentOffset + i);
                        handler.setStackInSlot(i, s);
                        if (!s.isEmpty()) hasItems = true;
                    }
                }
                if (hasItems) {
                    backpackStack.getOrCreateTag().put("Inventory", handler.serializeNBT());
                } else {
                    net.minecraft.nbt.CompoundTag tag = backpackStack.getTag();
                    if (tag != null) tag.remove("Inventory");
                }
                corpse.setItem(0, backpackStack);
            }
            currentOffset += lastCorpseBackpackCap;
        }
        
        if (lastCorpseVestCap > 0) {
            if (!vestStack.isEmpty()) {
                ItemStackHandler handler = new ItemStackHandler(lastCorpseVestCap);
                boolean hasItems = false;
                for (int i = 0; i < lastCorpseVestCap; i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        ItemStack s = this.corpseContentHandler.getStackInSlot(currentOffset + i);
                        handler.setStackInSlot(i, s);
                        if (!s.isEmpty()) hasItems = true;
                    }
                }
                if (hasItems) {
                    vestStack.getOrCreateTag().put("Inventory", handler.serializeNBT());
                } else {
                    net.minecraft.nbt.CompoundTag tag = vestStack.getTag();
                    if (tag != null) tag.remove("Inventory");
                }
                corpse.setItem(1, vestStack);
            }
            currentOffset += lastCorpseVestCap;
        }
        
        if (lastCorpseShirtCap > 0) {
            if (!shirtStack.isEmpty()) {
                ItemStackHandler handler = new ItemStackHandler(lastCorpseShirtCap);
                boolean hasItems = false;
                for (int i = 0; i < lastCorpseShirtCap; i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        ItemStack s = this.corpseContentHandler.getStackInSlot(currentOffset + i);
                        handler.setStackInSlot(i, s);
                        if (!s.isEmpty()) hasItems = true;
                    }
                }
                if (hasItems) {
                    shirtStack.getOrCreateTag().put("Inventory", handler.serializeNBT());
                } else {
                    net.minecraft.nbt.CompoundTag tag = shirtStack.getTag();
                    if (tag != null) tag.remove("Inventory");
                }
                corpse.setItem(2, shirtStack);
            }
            currentOffset += lastCorpseShirtCap;
        }
        
        if (lastCorpsePantsCap > 0) {
            if (!pantsStack.isEmpty()) {
                ItemStackHandler handler = new ItemStackHandler(lastCorpsePantsCap);
                boolean hasItems = false;
                for (int i = 0; i < lastCorpsePantsCap; i++) {
                    if (currentOffset + i < this.corpseContentHandler.getSlots()) {
                        ItemStack s = this.corpseContentHandler.getStackInSlot(currentOffset + i);
                        handler.setStackInSlot(i, s);
                        if (!s.isEmpty()) hasItems = true;
                    }
                }
                if (hasItems) {
                    pantsStack.getOrCreateTag().put("Inventory", handler.serializeNBT());
                } else {
                    net.minecraft.nbt.CompoundTag tag = pantsStack.getTag();
                    if (tag != null) tag.remove("Inventory");
                }
                corpse.setItem(3, pantsStack);
            }
            currentOffset += lastCorpsePantsCap;
        }
        
        corpse.setChanged();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    // Duplicate slotsChanged removed from here


    public int getPocketStart() {
        return VICINITY_SLOTS + 9;
    }

    public int getPocketEnd() { // Exclusive
        return getBackpackSlotStart();
    }

    public int getBackpackStart() {
        return getBackpackSlotStart();
    }
    
    public int getBackpackEnd() { // Exclusive
        return getBackpackSlotEnd() + 1;
    }

    public int getHotbarStart() {
        return getBackpackEnd();
    }

    public int getHotbarEnd() { // Exclusive
        return getHotbarStart() + 9;
    }
    
    public int getCraftingResultSlot() {
        return getHotbarEnd();
    }

    public int getCraftingInputStart() {
        return getCraftingResultSlot() + 1;
    }

    public int getCraftingInputEnd() { // Exclusive
        return getCraftingInputStart() + 4;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 在进行任何移动之前，先保存背包内容到物品 NBT
        if (!player.level().isClientSide) {
            saveBackpackToItem();
            saveCorpseStorageToItems();
            
            // Special handling for Shirt (31) and Pants (32) to prevent duplication
            if (index == VICINITY_SLOTS + 1) { // Shirt
                 Slot slot = this.slots.get(index);
                 if (slot.hasItem()) {
                     dropShirtItems(player, slot.getItem());
                 }
            } else if (index == VICINITY_SLOTS + 2) { // Pants
                 Slot slot = this.slots.get(index);
                 if (slot.hasItem()) {
                     dropPantsItems(player, slot.getItem());
                 }
            }
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();

        // 如果槽位本身是被锁定的 (Backpack Range)，禁止 Shift-点击移出
        if (this.isLockedMode && index >= getBackpackStart() && index < getBackpackEnd()) {
            return ItemStack.EMPTY;
        }

        // 索引范围:
        // Vicinity: 0-(VICINITY_SLOTS-1)
        // Equipment: VICINITY_SLOTS-(VICINITY_SLOTS+8)
        // Inventory: (VICINITY_SLOTS+9)-XX (Pockets + Backpack)
        // Hotbar: XX-XX
        // Crafting Result: XX
        // Crafting Input: XX

        if (slot instanceof ResultSlot) { // Handle Crafting Result
            this.access.execute((level, pos) -> {
                stack.getItem().onCraftedBy(stack, level, player);
            });
            
            if (this.isLockedMode) {
                // 仅允许移动到口袋 或 快捷栏
                if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), true)) {
                    if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarEnd(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            slot.onQuickCraft(stack, originalStack);
        } else if (index >= getCraftingInputStart() && index < getCraftingInputEnd()) { // Crafting Input (2x2)
            if (this.isLockedMode) {
                if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), false)) {
                    if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarEnd(), false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index >= getHotbarStart() && index < getHotbarEnd()) { // From Hotbar
            if (this.activeContainer != null) {
                this.moveItemStackToContainer(stack);
            }
            if (!stack.isEmpty()) {
                if (this.isLockedMode) {
                    // 仅允许移动到口袋
                    if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarStart(), true)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else if (index >= getPocketStart() && index < getBackpackEnd()) { // From Inventory (Pockets + Backpack)
            if (this.activeContainer != null) {
                this.moveItemStackToContainer(stack);
            }
            if (!stack.isEmpty()) {
                if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index >= 0 && index < VICINITY_SLOTS) { // From Vicinity
            if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                if (this.isLockedMode) {
                    // 仅允许移动到口袋
                    if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarStart(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else if (index >= VICINITY_SLOTS && index < VICINITY_SLOTS + 9) { // From Equipment
            if (this.isLockedMode) {
                // 仅允许移动到口袋 或 快捷栏
                if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), false)) {
                    if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarStart(), false)) {
                    if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else if (corpseStorageSlotStart >= 0 && index >= corpseStorageSlotStart && index <= corpseStorageSlotStart + 44) {
            if (!this.moveItemStackTo(stack, getHotbarStart(), getHotbarEnd(), false)) {
                if (this.isLockedMode) {
                    if (!this.moveItemStackTo(stack, getPocketStart(), getPocketEnd(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, getPocketStart(), getHotbarStart(), false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return originalStack;
    }

    private boolean moveItemStackToContainer(ItemStack stack) {
        if (this.activeContainer == null) return false;
        boolean changed = false;
        int containerSize = this.activeContainer.getContainerSize();
        
        // 1. 先尝试合并到已有堆叠
        for (int slotId = 0; slotId < this.slots.size(); slotId++) {
            Slot menuSlot = this.slots.get(slotId);
            if (menuSlot.container != this.vicinityProxy && menuSlot.container != this.activeContainer) continue;
            int containerIndex = menuSlot.getSlotIndex();
            if (menuSlot.container == this.vicinityProxy) {
                containerIndex = mapToActiveContainerIndex(containerIndex);
            }
            if (containerIndex < 0 || containerIndex >= containerSize) continue;
            if (!menuSlot.isActive()) continue;
            if (!menuSlot.mayPlace(stack)) continue;
            
            ItemStack containerStack = this.activeContainer.getItem(containerIndex);
            if (!containerStack.isEmpty() && ItemStack.isSameItemSameTags(stack, containerStack)) {
                int max = Math.min(stack.getMaxStackSize(), menuSlot.getMaxStackSize());
                int transfer = Math.min(stack.getCount(), max - containerStack.getCount());
                if (transfer > 0) {
                    containerStack.grow(transfer);
                    stack.shrink(transfer);
                    menuSlot.setChanged();
                    changed = true;
                }
            }
            if (stack.isEmpty()) break;
        }
        
        // 2. 尝试放入空位
        if (!stack.isEmpty()) {
            for (int slotId = 0; slotId < this.slots.size(); slotId++) {
                Slot menuSlot = this.slots.get(slotId);
                if (menuSlot.container != this.vicinityProxy && menuSlot.container != this.activeContainer) continue;
                int containerIndex = menuSlot.getSlotIndex();
                if (menuSlot.container == this.vicinityProxy) {
                    containerIndex = mapToActiveContainerIndex(containerIndex);
                }
                if (containerIndex < 0 || containerIndex >= containerSize) continue;
                if (!menuSlot.isActive()) continue;
                if (!menuSlot.mayPlace(stack)) continue;
                if (!this.activeContainer.getItem(containerIndex).isEmpty()) continue;
                
                int max = Math.min(stack.getMaxStackSize(), menuSlot.getMaxStackSize());
                int transfer = Math.min(stack.getCount(), max);
                ItemStack copy = stack.copy();
                copy.setCount(transfer);
                this.activeContainer.setItem(containerIndex, copy);
                stack.shrink(transfer);
                menuSlot.setChanged();
                changed = true;
                
                if (stack.isEmpty()) break;
            }
        }
        
        if (!stack.isEmpty() && isCorpseMode() && corpseStorageSlotStart >= 0 && corpseStorageCapacity > 0) {
            changed |= this.moveItemStackTo(stack, corpseStorageSlotStart, corpseStorageSlotStart + 45, false);
        }
        
        if (changed) {
            this.activeContainer.setChanged();
            // 如果在服务端，手动触发保存 NBT 到 TileEntity
            if (!player.level().isClientSide && activeContainer instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity be) {
                be.setChanged();
            }
            updateVicinityItems(this.player); // 立即更新 UI 槽位
        }
        return changed;
    }

    public IItemHandler getBackpackContentHandler() {
        return backpackContentHandler;
    }

    

    private void closeLootrAnimation(Player player, BlockPos pos) {
        if (pos == null) return;
        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
        if (be != null) {
            String className = be.getClass().getSimpleName();
            if (className.contains("Loot") || className.contains("Special")) {
                 InventoryUtils.stopOpenLootr(be, player);
            }
        }
    }

}
