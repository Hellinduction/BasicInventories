package club.hellin.util.basicinventories.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public final class ItemStackBuilder {
    private final ItemStack item;

    public ItemStackBuilder(final Material material) {
        this.item = new ItemStack(material);
    }

    public ItemStackBuilder(final ItemStack item) {
        this.item = item;
    }

    public ItemStack build() {
        return this.item;
    }

    public ItemStackBuilder hideEnchants() {
        this.updateMeta(meta -> meta.addItemFlags(ItemFlag.HIDE_ENCHANTS));

        return this;
    }

    public ItemStackBuilder addEnchant(final Enchantment enchant) {
        return this.addEnchant(enchant, 1);
    }

    public ItemStackBuilder addEnchant(final Enchantment enchant, final int level) {
        this.item.addUnsafeEnchantment(enchant, level);

        return this;
    }

    public ItemStackBuilder setDisplayName(final String displayName) {
        this.updateMeta(meta -> meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName)));

        return this;
    }

    public ItemStackBuilder addLoreLine(final String loreLine) {
        this.updateMeta(meta -> {
            List<String> lore = meta.getLore();

            if (lore == null)
                lore = new ArrayList<>();

            lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));

            meta.setLore(lore);
        });

        return this;
    }

    public ItemStackBuilder removeLoreLineIf(final Predicate<String> predicate) {
        this.updateMeta(meta -> {
            final List<String> lore = new ArrayList<>();

            if (lore == null)
                return;

            lore.removeIf(predicate);
            meta.setLore(lore);
        });

        return this;
    }

    public ItemStackBuilder setLore(final String... lore) {
        this.updateMeta(meta -> meta.setLore(Arrays.stream(lore).map(str -> ChatColor.translateAlternateColorCodes('&', str)).collect(Collectors.toList())));
        return this;
    }

    public ItemMeta getMeta() {
        return this.item.getItemMeta();
    }

    public ItemStackBuilder updateMeta(final Consumer<ItemMeta> metaConsumer) {
        final ItemMeta meta = this.getMeta();
        metaConsumer.accept(meta);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        this.item.setItemMeta(meta);

        return this;
    }

    public ItemStackBuilder unbreakable() {
        this.updateMeta(meta -> meta.setUnbreakable(true));
        return this;
    }

    public ItemStackBuilder amount(final int amount) {
        this.item.setAmount(amount);
        return this;
    }
}