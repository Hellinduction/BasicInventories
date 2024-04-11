package club.hellin.util.basicinventories;

import club.hellin.util.basicinventories.objects.Confirmation;
import club.hellin.util.basicinventories.type.VerifyInventory;
import club.hellin.util.basicinventories.utils.ComponentManager;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

@Getter
public final class InventoryManager extends ComponentManager<AbstractInventory> {
    @Getter
    @ToString
    public static final class InventoryPath {
        private final UUID uuid;
        private final List<AbstractInventory> path = new ArrayList<>();

        private int pointer = 0;

        public InventoryPath(final UUID uuid) {
            this.uuid = uuid;
        }

        public void addInventory(final AbstractInventory inventory) {
            if (this.get() != null && this.get().equals(inventory))
                return;

            this.path.add(inventory);
            ++pointer;
        }

        /**
         * Read only
         * @return
         */
        public AbstractInventory get() {
            if (!this.validate(this.pointer))
                return null;

            final AbstractInventory inventory = this.path.get(this.pointer);
            return inventory;
        }

        /**
         * Adjusts the pointer
         * @return
         */
        public AbstractInventory next() {
            final int next = ++this.pointer;

            if (!this.validate(next))
                return null;

            final AbstractInventory inventory = this.path.get(next);
            return inventory;
        }

        /**
         * Adjusts the pointer
         * @return
         */
        public AbstractInventory previous() {
            final int previous = --this.pointer;

            if (!this.validate(previous))
                return null;

            final AbstractInventory inventory = this.path.get(previous);
            return inventory;
        }

        /**
         * Read only
         * @return
         */
        public AbstractInventory getNext() {
            final int next = this.pointer + 1;

            if (!this.validate(next))
                return null;

            final AbstractInventory inventory = this.path.get(next);
            return inventory;
        }

        /**
         * Read only
         * @return
         */
        public AbstractInventory getPrevious() {
            final int previous = this.pointer - 1;

            if (!this.validate(previous))
                return null;

            final AbstractInventory inventory = this.path.get(previous);
            return inventory;
        }

        private boolean validate(final int index) {
            return index >= 0 && index < this.path.size();
        }
    }

    private static InventoryManager singletonInstance;

    private final Map<UUID, InventoryPath> inventoryPathMap = new HashMap<>();

    private @Setter Plugin plugin;

    public static InventoryManager getInstance() {
        if (!(singletonInstance instanceof InventoryManager))
            singletonInstance = new InventoryManager();

        return singletonInstance;
    }

    @Override
    public void init(final Plugin plugin) {
        getInstance().setPlugin(plugin);
    }

    public String toEnumName(String name) {
        name = ChatColor.stripColor(name);
        name = name.toUpperCase();
        name = name.replace(" ", "_");

        return name;
    }

    public String getInventoryName(final InventoryView view) {
        try {
            final Inventory inventory = view.getTopInventory();

            final Class<? extends Inventory> clazz = inventory.getClass();
            final Method method = clazz.getDeclaredMethod("getName");
            method.setAccessible(true);

            final Object nameObj = method.invoke(inventory);
            final String name = (String) nameObj;

            return name;
        } catch (final Exception ignored) {
        }

        try {
            final Class<? extends InventoryView> clazz = view.getClass();
            final Method method = clazz.getDeclaredMethod("getTitle");
            method.setAccessible(true);

            final Object nameObj = method.invoke(view);
            final String name = (String) nameObj;

            return name;
        } catch (final Exception ignored) {
        }

        return null;
    }

    public AbstractInventory getInventory(final InventoryView view) {
        AbstractInventory found = null;

        for (final AbstractInventory inventory : super.get()) {
            if (!inventory.isInventory(view))
                continue;

            found = inventory;
            break;
        }

        return found;
    }

    public AbstractInventory getInventory(String name) {
        AbstractInventory found = null;
        name = this.toEnumName(name);

        for (final AbstractInventory inventory : super.get()) {
            if (!inventory.getRawName().equals(name))
                continue;

            found = inventory;
            break;
        }

        return found;
    }

    public <T> List<AbstractInventory> getInventories(final T attachment) {
        final List<AbstractInventory> inventories = new ArrayList<>();

        for (final AbstractInventory inventory : super.get()) {
            for (final AbstractInventory.OpenSession session : inventory.getOpen().values()) {
                if (session == null)
                    continue;

                final Player player = session.getPlayer();

                if (player == null || !player.isOnline())
                    continue;

                if (!attachment.equals(inventory.getAttachment(player)))
                    continue;

                inventories.add(inventory);
            }
        }

        return inventories;
    }

    public void verify(final Player player, final Consumer<Confirmation> callback) {
        this.verify(player, callback, null);
    }

    public void verify(final Player player, final Consumer<Confirmation> callback, final String title) {
        final UUID uuid = player.getUniqueId();
        final InventoryView view = player.getOpenInventory();

        Object attachment = null;

        if (view != null) {
            final AbstractInventory inventory = InventoryManager.getInstance().getInventory(view);

            if (inventory != null && inventory.getAttachment(player) != null)
                attachment = inventory.getAttachment(player);
        }

        final VerifyInventory verifyInventory = (VerifyInventory) this.getInventory("VERIFY");

        player.closeInventory();
        verifyInventory.getCallbackMap().put(uuid, callback);

        final Inventory inventory = verifyInventory.createInventory(player, title, attachment);
        player.openInventory(inventory);
    }
}