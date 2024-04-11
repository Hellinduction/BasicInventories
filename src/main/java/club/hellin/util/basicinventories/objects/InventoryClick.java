package club.hellin.util.basicinventories.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

@Getter
@AllArgsConstructor
public final class InventoryClick {
    private final InventoryClickEvent event;
    private final Player whoClicked;
    private final InventoryView view;
    private final ItemStack clickedItem;
}