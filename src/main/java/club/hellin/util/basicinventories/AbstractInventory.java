package club.hellin.util.basicinventories;

import club.hellin.util.basicinventories.utils.ItemStackBuilder;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter(AccessLevel.PROTECTED)
@ToString
public abstract class AbstractInventory implements InventoryBase {
    @Getter
    @Setter
    public static final class OpenSession {
        private final UUID uuid;

        private Object attachment;

        public OpenSession(final UUID uuid) {
            this.uuid = uuid;
        }

        public <T> void setAttachment(final T attachment) {
            this.attachment = attachment;
        }

        public <T> T getAttachment() {
            return (T) this.attachment;
        }

        public Player getPlayer() {
            return Bukkit.getPlayer(this.uuid);
        }
    }

    private static final String NBT_TAG = "basic_inventories_inventory_tag";
    private static final int DEFAULT_SIZE = 9;

    private final Map<UUID, OpenSession> open = new HashMap();

    private String title;
    private String rawName;

    public AbstractInventory() {
        final InventoryProperties properties = this.getProperties();

        this.setTitle(properties.title());
    }

    protected void setTitle(final String title) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.rawName = InventoryManager.getInstance().toEnumName(this.title);
    }

    public InventoryProperties getProperties() {
        final Annotation annotation = this.getClass().getAnnotation(InventoryProperties.class);
        final InventoryProperties properties = (InventoryProperties) annotation;

        return properties;
    }

    public boolean isMainMenu() {
        return this.getClass().getAnnotation(MainMenu.class) != null;
    }

    @Override
    public int getSize(final Player player) {
        return DEFAULT_SIZE;
    }

    @Override
    public boolean isInventory(final InventoryView view) {
        final String name = InventoryManager.getInstance().getInventoryName(view);

        if (view == null || name == null)
            return false;

        if (this.open.containsKey(view.getPlayer().getUniqueId()))
            return true;

        return ChatColor.stripColor(name).equals(ChatColor.stripColor(this.title));
    }

    @Override
    public boolean verify(ItemStack item) {
        item = item.clone();
        item.setAmount(Math.max(item.getAmount(), 1));

        final NBTItem nbtItem = new NBTItem(item);

        if (!nbtItem.hasTag(NBT_TAG))
            return false;

        final String tag = nbtItem.getString(NBT_TAG);
        return tag.equals(this.getRawName());
    }

    @Override
    public ItemStack tag(ItemStack item) {
        if (item.getType() == Material.AIR)
            return item;

        item = item.clone();

        if (item.getAmount() <= 0)
            item.setAmount(1);

        final NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString(NBT_TAG, this.getRawName());
        return nbtItem.getItem();
    }

    @Override
    public void close(final Player player) {
        final UUID uuid = player.getUniqueId();
        this.open.remove(uuid);
    }

    @Override
    public void back(final Player player) {
        final InventoryView view = player.getOpenInventory();
        final UUID uuid = player.getUniqueId();

        final InventoryManager.InventoryPath path = InventoryManager.getInstance().getInventoryPathMap().get(uuid);

        if (path == null)
            return;

        final AbstractInventory lastInventory = path.previous();
        final AbstractInventory currentInventory = InventoryManager.getInstance().getInventory(view);

        if (lastInventory == null || currentInventory == null)
            return;

        final Object attachment = currentInventory.getAttachment(player);

        player.closeInventory();

        final Inventory inventory = lastInventory.createInventory(player, attachment);
        player.openInventory(inventory);
    }

    public ItemStack getBackButton() {
        ItemStack item = new ItemStackBuilder(Material.MAGENTA_GLAZED_TERRACOTTA)
                .addEnchant(Enchantment.KNOCKBACK)
                .hideEnchants()
                .setDisplayName("&bGo Back")
                .build();

        item = this.tag(item);

        return item;
    }

    public void addOpen(final Player player) {
        this.addOpen(player, null);
    }

    protected void addOpen(final Player player, final OpenSession session) {
        final UUID uuid = player.getUniqueId();
        this.open.put(uuid, session == null ? new OpenSession(uuid) : session);
    }

    public boolean isOpen(final Player player) {
        final UUID uuid = player.getUniqueId();
        return this.open.containsKey(uuid);
    }

    protected <T> void setAttachment(final Player player, final T attachment) {
        final UUID uuid = player.getUniqueId();
        final OpenSession session = this.open.get(uuid);

        if (session == null)
            return;

        session.setAttachment(attachment);
    }

    protected <T> T getAttachment(final Player player) {
        final UUID uuid = player.getUniqueId();
        final OpenSession session = this.open.get(uuid);

        if (session == null)
            return null;

        return session.getAttachment();
    }

    public void updateAll() {
        for (final UUID uuid : this.open.keySet()) {
            final Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline())
                continue;

            final InventoryView view = player.getOpenInventory();

            if (view != null && this.isInventory(view) && view.getTopInventory() != null) {
                final Inventory top = view.getTopInventory();
                Bukkit.getScheduler().runTask(InventoryManager.getInstance().getPlugin(), () -> this.setItems(player, top));
            }
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof AbstractInventory))
            return false;

        final AbstractInventory inventory = (AbstractInventory) object;
        return this.getRawName().equals(inventory.getRawName());
    }
}