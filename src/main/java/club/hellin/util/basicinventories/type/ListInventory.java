package club.hellin.util.basicinventories.type;

import club.hellin.util.basicinventories.AbstractInventory;
import club.hellin.util.basicinventories.objects.InventoryClick;
import club.hellin.util.basicinventories.utils.ItemStackBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public abstract class ListInventory<E> extends AbstractInventory {
    private static final int MAX_INVENTORY_ROWS = 6;
    private static final int DEFAULT_PAGE_NUMBER = 1;

    private final Map<UUID, Integer> pageMap = new HashMap<>();

    public List<E> getFilteredElements(final Player player) {
        final List<E> filtered = this.provide(player).stream().filter(element -> this.convertFrom(element).getType() != Material.AIR).collect(Collectors.toList());
        return filtered;
    }

    public abstract E convertTo(final ItemStack item);

    public abstract ItemStack convertFrom(final E element);

    public abstract Collection<E> provide(final Player player);

    @Override
    public int getSize(final Player player) {
        final int maxSize = MAX_INVENTORY_ROWS * 9;
        int size = ((this.getFilteredElements(player).size() / 9 + 1) * 9) + (9 * 2);

        if (size > maxSize)
            size = maxSize;

        return size;
    }

    private int getUsableSize(final Player player) {
        return this.getSize(player) - (9 * 2);
    }

    @Override
    public List<ItemStack> getItems(final Player player) {
        return this.getFilteredElements(player).stream().map(this::convertFrom).collect(Collectors.toList());
    }

    @Override
    public Inventory createInventory(final Player player) {
        return this.createInventory(player, null);
    }

    @Override
    public <T> Inventory createInventory(final Player player, final T attachment) {
        super.addOpen(player);

        if (attachment != null)
            super.setAttachment(player, attachment);

        final UUID uuid = player.getUniqueId();
        this.pageMap.put(uuid, DEFAULT_PAGE_NUMBER);

        final int size = this.getSize(player);
        final Inventory inventory = Bukkit.createInventory(null, size, super.getTitle());

        this.setItems(player, inventory);
        return inventory;
    }

    @Override
    public void handle(final InventoryClick click) {
        final Player player = click.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        final ItemStack item = click.getClickedItem();

        final int currentPage = this.pageMap.getOrDefault(uuid, DEFAULT_PAGE_NUMBER);
        final Consumer<Integer> setNextPage = pageNumber -> {
            if (!this.pageExists(player, pageNumber)) {
                player.sendMessage(ChatColor.RED + "This page does not exist.");
                return;
            }

            this.pageMap.put(uuid, pageNumber);
        };

        if (item.isSimilar(this.getPreviousPageButton())) {
            final int nextPage = currentPage - 1;
            setNextPage.accept(nextPage);
            return;
        }

        if (item.isSimilar(this.getNextPageButton())) {
            final int nextPage = currentPage + 1;
            setNextPage.accept(nextPage);
            return;
        }

        if (item.isSimilar(this.getPageInfo(player)))
            return;

        final E element = this.convertTo(item);

        if (element == null)
            return;

        this.handle(click, element);
    }

    private ItemStack getPreviousPageButton() {
        ItemStack item = new ItemStackBuilder(Material.RED_CONCRETE).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName("&bPrevious Page").build();
        item = super.tag(item);
        return item;
    }

    private ItemStack getNextPageButton() {
        ItemStack item = new ItemStackBuilder(Material.GREEN_CONCRETE).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName("&bNext Page").build();
        item = super.tag(item);
        return item;
    }

    private ItemStack getPageInfo(final Player player) {
        final UUID uuid = player.getUniqueId();
        final int page = this.pageMap.getOrDefault(uuid, DEFAULT_PAGE_NUMBER);

        ItemStack item = new ItemStackBuilder(Material.PAPER).addEnchant(Enchantment.KNOCKBACK).hideEnchants().setDisplayName(String.format("&aPage Number:&e %s", page)).build();
        item = super.tag(item);
        return item;
    }

    public abstract void handle(final InventoryClick click, final E element);

    @Override
    public void setItems(final Player player, final Inventory inventory) {
        inventory.clear();

        final UUID uuid = player.getUniqueId();
        final int pageNumber = this.pageMap.getOrDefault(uuid, DEFAULT_PAGE_NUMBER);

        final List<E> elements = this.getPage(player, pageNumber);

        for (final E element : elements) {
            ItemStack item = this.convertFrom(element);

            if (item == null)
                continue;

            item = super.tag(item);
            inventory.addItem(item);
        }

        // Add page buttons
        final int bottomRowFirstIndex = this.getSize(player) - 9;
        final int bottomRowMidIndex = this.getSize(player) - 5;
        final int bottomRowLastIndex = this.getSize(player) - 1;

        inventory.setItem(bottomRowFirstIndex, this.getPreviousPageButton());
        inventory.setItem(bottomRowMidIndex, this.getPageInfo(player));
        inventory.setItem(bottomRowLastIndex - 1, super.getBackButton());
        inventory.setItem(bottomRowLastIndex, this.getNextPageButton());
    }

    private boolean pageExists(final Player player, final int pageNumber) {
        return !this.getPage(player, pageNumber).isEmpty();
    }

    private List<E> getPage(final Player player, final int pageNumber) {
        return this.getPages(player).getOrDefault(pageNumber, new ArrayList<>());
    }

    private Map<Integer, List<E>> getPages(final Player player) {
        final Map<Integer, List<E>> pages = new HashMap<>();
        final int usableSlots = this.getUsableSize(player);

        int currentPage = 1;
        int currentSlot = 0;

        for (final E element : this.getFilteredElements(player)) {
            final List<E> list = pages.getOrDefault(currentPage, new ArrayList<>());

            if (!list.contains(element))
                list.add(element);

            pages.put(currentPage, list);

            ++currentSlot;

            if (currentSlot == usableSlots) {
                currentSlot = 0;
                ++currentPage;
            }
        }

        return pages;
    }

    @Override
    public void close(final Player player) {
        final UUID uuid = player.getUniqueId();
        this.pageMap.remove(uuid);

        super.close(player);
    }
}