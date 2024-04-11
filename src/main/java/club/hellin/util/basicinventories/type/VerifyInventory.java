package club.hellin.util.basicinventories.type;

import club.hellin.util.basicinventories.AbstractInventory;
import club.hellin.util.basicinventories.InventoryProperties;
import club.hellin.util.basicinventories.objects.Confirmation;
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

@InventoryProperties(title = "&2Verify")
@Getter
public final class VerifyInventory extends AbstractInventory {
    private final Map<UUID, Consumer<Confirmation>> callbackMap = new HashMap<>();

    private ItemStack getConfirm() {
        return new ItemStackBuilder(Material.LIME_CONCRETE)
                .setDisplayName("&a&lConfirm")
                .addEnchant(Enchantment.KNOCKBACK)
                .hideEnchants()
                .build();
    }

    private ItemStack getDeny() {
        return new ItemStackBuilder(Material.RED_CONCRETE)
                .setDisplayName("&c&lDeny")
                .addEnchant(Enchantment.KNOCKBACK)
                .hideEnchants()
                .build();
    }

    @Override
    public List<ItemStack> getItems(final Player player) {
        return Arrays.asList(this.getConfirm(), this.getDeny());
    }

    @Override
    public Inventory createInventory(final Player player) {
        return this.createInventory(player, null);
    }

    @Override
    public <T> Inventory createInventory(final Player player, final T attachment) {
        return this.createInventory(player, null, attachment);
    }

    public <T> Inventory createInventory(final Player player, final String title, final T attachment) {
        super.addOpen(player);

        if (attachment != null)
            super.setAttachment(player, attachment);

        final int size = this.getSize(player);
        final Inventory inventory = Bukkit.createInventory(null, size, title != null ? ChatColor.translateAlternateColorCodes('&', title) : super.getTitle());

        this.setItems(player, inventory);
        return inventory;
    }

    @Override
    public void handle(final InventoryClick click) {
        final Confirmation confirmation = new Confirmation(click.getClickedItem().isSimilar(this.getConfirm()) ? Confirmation.Status.CONFIRMED : Confirmation.Status.DENIED, click);
        final Consumer<Confirmation> callback = this.callbackMap.get(click.getWhoClicked().getUniqueId());

        if (callback == null)
            return;

        final Player whoClicked = click.getWhoClicked();

        whoClicked.closeInventory();
        callback.accept(confirmation);
    }

    @Override
    public void setItems(final Player player, final Inventory inventory) {
        inventory.clear();

        inventory.setItem(2, this.getConfirm());
        inventory.setItem(4, super.getBackButton());
        inventory.setItem(6, this.getDeny());
    }
}