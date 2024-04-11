package club.hellin.util.basicinventories.type;

import club.hellin.util.basicinventories.*;
import club.hellin.util.basicinventories.objects.InventoryClick;
import club.hellin.util.basicinventories.utils.ItemStackBuilder;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public abstract class CaseByCaseInventory extends AbstractInventory {
    private static final String NBT_TAG = "basic_inventories_item_id_tag";
    private static final int MAX_SIZE = 17;

    @AllArgsConstructor
    @ToString
    @Getter
    public static class InventoryItem {
        private final String coloredName;
        private final String rawName;
        private final @Getter(AccessLevel.NONE) ItemStack item;
        private final Method method;
        private final CaseByCaseInventory inventory;
        private final int index;

        public ItemStack getItem(final Player player) {
            return this.item;
        }
    }

    @ToString
    public static final class InventorySwitchItem extends InventoryItem {
        public InventorySwitchItem(final String coloredName, final String rawName, final Method method, final int index, final CaseByCaseInventory inventory) {
            super(coloredName, rawName, null, method, inventory, index);
        }

        @Override
        public ItemStack getItem(final Player player) {
            final InventoryItemProvider provider = super.getInventory().getProvider(player);

            if (provider == null)
                return null;

            ItemStack item = provider.provide();
            item = super.getInventory().tag(item);
            item = super.getInventory().tag(item, super.getIndex());

            return item;
        }
    }

    public int getAddToSize() {
        return 0;
    }

    @ToString
    public static final class InventoryToggleItem extends InventoryItem {
        private final Method method;
        private final int index;

        public InventoryToggleItem(final String coloredName, final String rawName, final Method method, final int index, final CaseByCaseInventory inventory) {
            super(coloredName, rawName, null, method, inventory, index);

            this.method = method;
            this.index = index;
        }

        @Override
        public ItemStack getItem(final Player player) {
            final boolean toggled = super.getInventory().isToggledOn(player, super.getRawName());

            final ItemStackBuilder builder = new ItemStackBuilder(toggled ? Material.EMERALD : Material.REDSTONE);

            builder.addEnchant(Enchantment.KNOCKBACK)
                    .hideEnchants();

            if (toggled)
                builder.setDisplayName(String.format("%s&7: &a&lON", super.getColoredName()));
            else
                builder.setDisplayName(String.format("%s&7: &c&lOFF", super.getColoredName()));

            ItemStack item = builder.build();
            item = super.getInventory().tag(item);
            item = super.getInventory().tag(item, super.getIndex());

            return item;
        }
    }

    private final Map<Integer, InventoryItem> inventoryItemMap;

    public CaseByCaseInventory() {
        this.inventoryItemMap = this.getHandlers();
    }

    private Map<Integer, InventoryItem> getHandlers() {
        final Map<Integer, InventoryItem> itemMap = new LinkedHashMap<>();
        final Class<?> clazz = this.getClass();

        int currentIndex = 1;

        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.getParameters().length != 1 || method.getAnnotations().length == 0)
                continue;

            if (!Arrays.stream(method.getAnnotations()).anyMatch(annotation -> annotation instanceof InventoryHandler || annotation instanceof InventoryToggleHandler))
                continue;

            final InventoryHandler handler = method.getAnnotation(InventoryHandler.class);
            final InventoryToggleHandler toggleHandler = method.getAnnotation(InventoryToggleHandler.class);

            final Parameter param = method.getParameters()[0];

            if (param == null)
                continue;

            final Class<?> paramType = param.getType();
            if (!InventoryClick.class.isAssignableFrom(paramType))
                continue;

            if (handler != null) {
                ItemStackBuilder itemBuilder = new ItemStackBuilder(handler.type()).setDisplayName(handler.name()).setLore(handler.lore());
                if (handler.enchanted())
                    itemBuilder.addEnchant(Enchantment.KNOCKBACK).hideEnchants();

                ItemStack item = itemBuilder.build();
                item = super.tag(item);
                item = this.tag(item, currentIndex);

                final String displayName = item.getItemMeta().getDisplayName();
                final String rawName = InventoryManager.getInstance().toEnumName(displayName);

                final boolean switcher = handler.switcher();

                if (!switcher) {
                    final InventoryItem inventoryItem = new InventoryItem(displayName, rawName, item, method, this, currentIndex);
                    itemMap.put(currentIndex, inventoryItem);
                } else {
                    final InventorySwitchItem inventorySwitchItem = new InventorySwitchItem(displayName, rawName, method, currentIndex, this);
                    itemMap.put(currentIndex, inventorySwitchItem);
                }
            }

            if (toggleHandler != null) {
                final String coloredName = ChatColor.translateAlternateColorCodes('&', toggleHandler.name());
                final String rawName = InventoryManager.getInstance().toEnumName(coloredName);
                final InventoryToggleItem toggleItem = new InventoryToggleItem(coloredName, rawName, method, currentIndex, this);
                itemMap.put(currentIndex, toggleItem);
            }

            ++currentIndex;
        }

        return itemMap;
    }

    private ItemStack tag(final ItemStack item, final int index) {
        final NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger(NBT_TAG, index);
        return nbtItem.getItem();
    }

    @Override
    public int getSize(final Player player) {
        int size = this.inventoryItemMap.size();
        int rows = (int) Math.ceil(size / 3.0);
        return (rows * 9) + this.getAddToSize();
    }

    public boolean isToggledOn(final Player player, final String rawName) {
        return false;
    }

    public InventoryItemProvider getProvider(final Player player) {
        return null;
    }

    @Override
    public Inventory createInventory(final Player player) {
        return this.createInventory(player, null);
    }

    @Override
    public <T> Inventory createInventory(final Player player, final T attachment) {
        if (this.inventoryItemMap.size() > MAX_SIZE)
            throw new RuntimeException("This inventory has too many items");

        super.addOpen(player);

        if (attachment != null)
            super.setAttachment(player, attachment);

        final int size = this.getSize(player);
        final Inventory inventory = Bukkit.createInventory(null, size, super.getTitle());

        this.setItems(player, inventory);
        return inventory;
    }

    private boolean isBottomRowFree(final Player player, final Inventory inventory) {
        final int size = this.getSize(player);

        final int start = size - 9;
        int nonAirCount = 0;

        for (int i = start; i < size; ++i) {
            final ItemStack item = inventory.getItem(i);

            if (item == null || item.getType() == Material.AIR)
                continue;

            ++nonAirCount;
        }

        return nonAirCount == 0;
    }

    @Override
    public void setItems(final Player player, final Inventory inventory) {
        inventory.clear();

        int row;
        final int spacing = 4;

        final int size = this.getSize(player);
        final int rows = size / 9;

        for (row = 0; row < rows; ++row) {
            final int centerIndex = (row * 9) + 4;
            final int leftIndex = centerIndex - spacing;
            final int rightIndex = centerIndex + spacing;

            final InventoryItem inventoryItem = this.getInventoryItem(centerIndex);
            final InventoryItem left = this.getInventoryItem(leftIndex);
            final InventoryItem right = this.getInventoryItem(rightIndex);

            if (inventoryItem == null && left == null && right == null)
                continue;

            if (left != null && inventoryItem == null) {
                inventory.setItem(centerIndex, left.getItem(player));
                continue;
            }

            if (left == null && right == null) {
                inventory.setItem(centerIndex, inventoryItem.getItem(player));
                continue;
            }

            if (left == null) {
                inventory.setItem(leftIndex, inventoryItem.getItem(player));
                inventory.setItem(rightIndex, right.getItem(player));
                continue;
            }

            if (right == null) {
                inventory.setItem(leftIndex, left.getItem(player));
                inventory.setItem(rightIndex, inventoryItem.getItem(player));
                continue;
            }

            inventory.setItem(centerIndex, inventoryItem.getItem(player));
            inventory.setItem(leftIndex, left.getItem(player));
            inventory.setItem(rightIndex, right.getItem(player));
        }

        if (this.isBottomRowFree(player, inventory)) {
            final int mid = size - 5;
            inventory.setItem(mid, super.getBackButton());
        }
    }

    private InventoryItem getInventoryItem(final int index) {
        final int itemsPerRow = 3;
        final int rowIndex = index / 9; // Calculate the row index
        final int itemIndexInRow = (index % 9) % itemsPerRow; // Calculate the index of the item within the row
        int mapIndex = (rowIndex * itemsPerRow) + itemIndexInRow; // Calculate the index in the inventoryItemMap
        ++mapIndex;

        final InventoryItem item = this.inventoryItemMap.get(mapIndex);
        return item;
    }

    @Override
    public List<ItemStack> getItems(final Player player) {
        return this.inventoryItemMap.values().stream().map(inventoryItem -> inventoryItem.getItem(player)).collect(Collectors.toList());
    }

    @Override
    public void handle(final InventoryClick click) {
        ItemStack item = click.getClickedItem().clone();
        item.setAmount(1);

        final NBTItem nbtItem = new NBTItem(item);

        if (!nbtItem.hasTag(NBT_TAG))
            return;

        final int index = nbtItem.getInteger(NBT_TAG);
        final InventoryItem inventoryItem = this.inventoryItemMap.get(index);

        if (inventoryItem == null)
            return;

        final Method method = inventoryItem.getMethod();
        method.setAccessible(true);

        try {
            method.invoke(this, click);
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }
}