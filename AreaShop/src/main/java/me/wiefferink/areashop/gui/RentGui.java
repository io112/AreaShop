package me.wiefferink.areashop.gui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.TeleportCommand;
import me.wiefferink.areashop.commands.UnrentCommand;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.interactivemessenger.processing.Message;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chest GUI that lists the regions a player rents and lets them teleport, extend or unrent.
 */
@Singleton
public class RentGui implements Listener {

    private static final int LIST_SIZE = 54;
    private static final int REGIONS_PER_PAGE = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 50;

    private static final int CONFIRM_SIZE = 27;
    private static final int SLOT_CONFIRM_YES = 11;
    private static final int SLOT_CONFIRM_REGION = 13;
    private static final int SLOT_CONFIRM_NO = 15;

    private final Plugin plugin;
    private final IFileManager fileManager;
    private final MessageBridge messageBridge;

    @Inject
    public RentGui(@Nonnull Plugin plugin, @Nonnull IFileManager fileManager, @Nonnull MessageBridge messageBridge) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.messageBridge = messageBridge;
    }

    /**
     * Open the list of rented regions for a player.
     * @param player The player to open it for
     * @param page   The zero-based page to show, clamped to the available pages
     */
    public void open(@Nonnull Player player, int page) {
        List<RentRegion> rents = getRents(player);
        if (rents.isEmpty()) {
            player.closeInventory();
            messageBridge.message(player, "gui-noRents");
            return;
        }
        int pages = (rents.size() + REGIONS_PER_PAGE - 1) / REGIONS_PER_PAGE;
        page = Math.max(0, Math.min(page, pages - 1));

        RentGuiHolder holder = new RentGuiHolder(RentGuiHolder.View.LIST, page, null);
        Inventory inventory = Bukkit.createInventory(holder, LIST_SIZE, text("gui-title", page + 1, pages));
        holder.setInventory(inventory);

        int start = page * REGIONS_PER_PAGE;
        int end = Math.min(rents.size(), start + REGIONS_PER_PAGE);
        for (int i = start; i < end; i++) {
            RentRegion rent = rents.get(i);
            int slot = i - start;
            inventory.setItem(slot, regionItem(player, rent, true));
            holder.setRegion(slot, rent.getName());
        }

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = REGIONS_PER_PAGE; slot < LIST_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, item(Material.ARROW, text("gui-previousPage", page, pages), List.of()));
        }
        inventory.setItem(SLOT_CLOSE, item(Material.BARRIER, text("gui-close"), List.of()));
        if (page < pages - 1) {
            inventory.setItem(SLOT_NEXT, item(Material.ARROW, text("gui-nextPage", page + 2, pages), List.of()));
        }

        player.openInventory(inventory);
    }

    private void openConfirmUnrent(@Nonnull Player player, @Nonnull RentRegion rent, int returnPage) {
        RentGuiHolder holder = new RentGuiHolder(RentGuiHolder.View.CONFIRM_UNRENT, returnPage, rent.getName());
        Inventory inventory = Bukkit.createInventory(holder, CONFIRM_SIZE, text("gui-confirmTitle", rent));
        holder.setInventory(inventory);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < CONFIRM_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(SLOT_CONFIRM_YES, item(Material.LIME_CONCRETE, text("gui-confirmYes", rent), lore(rent, "gui-confirmYesLore")));
        inventory.setItem(SLOT_CONFIRM_REGION, regionItem(player, rent, false));
        inventory.setItem(SLOT_CONFIRM_NO, item(Material.RED_CONCRETE, text("gui-confirmNo", rent), List.of()));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof RentGuiHolder holder)) {
            return;
        }
        // Never let items move in or out of the GUI
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (holder.getView() == RentGuiHolder.View.LIST) {
            handleListClick(player, holder, slot, event.getClick());
        } else {
            handleConfirmClick(player, holder, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RentGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleListClick(@Nonnull Player player, @Nonnull RentGuiHolder holder, int slot, @Nonnull ClickType click) {
        int page = holder.getPage();
        if (slot == SLOT_PREVIOUS && page > 0) {
            later(() -> open(player, page - 1));
            return;
        }
        if (slot == SLOT_NEXT) {
            ItemStack next = holder.getInventory().getItem(SLOT_NEXT);
            if (next != null && next.getType() == Material.ARROW) {
                later(() -> open(player, page + 1));
            }
            return;
        }
        if (slot == SLOT_CLOSE) {
            later(player::closeInventory);
            return;
        }

        String regionName = holder.getRegion(slot);
        if (regionName == null) {
            return;
        }
        RentRegion rent = fileManager.getRent(regionName);
        if (rent == null || !rent.isRenter(player)) {
            messageBridge.message(player, "gui-regionGone", regionName);
            later(() -> open(player, page));
            return;
        }

        switch (click) {
            case LEFT -> {
                if (!TeleportCommand.canUse(player, rent)) {
                    messageBridge.message(player, "teleport-noPermission");
                    return;
                }
                later(() -> {
                    player.closeInventory();
                    rent.getTeleportFeature().teleportPlayer(player, false);
                });
            }
            case RIGHT -> {
                if (!player.hasPermission("areashop.rent")) {
                    messageBridge.message(player, "rent-noPermission");
                    return;
                }
                // Renting a region you already rent extends it, rent() checks all restrictions and sends the messages
                rent.rent(player);
                later(() -> open(player, page));
            }
            case SHIFT_RIGHT -> {
                if (!UnrentCommand.canUse(player, rent)) {
                    messageBridge.message(player, "unrent-noPermission");
                    return;
                }
                later(() -> openConfirmUnrent(player, rent, page));
            }
            default -> {
                // Other click types do nothing
            }
        }
    }

    private void handleConfirmClick(@Nonnull Player player, @Nonnull RentGuiHolder holder, int slot) {
        int page = holder.getPage();
        if (slot == SLOT_CONFIRM_NO) {
            later(() -> open(player, page));
            return;
        }
        if (slot != SLOT_CONFIRM_YES || holder.getConfirmRegion() == null) {
            return;
        }
        RentRegion rent = fileManager.getRent(holder.getConfirmRegion());
        if (rent == null || !rent.isRenter(player)) {
            messageBridge.message(player, "gui-regionGone", holder.getConfirmRegion());
        } else {
            rent.unRent(true, player);
        }
        later(() -> open(player, page));
    }

    @Nonnull
    private List<RentRegion> getRents(@Nonnull Player player) {
        List<RentRegion> result = new ArrayList<>();
        for (RentRegion rent : fileManager.getRentsRef()) {
            if (rent.isRenter(player)) {
                result.add(rent);
            }
        }
        result.sort(Comparator.comparing(GeneralRegion::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Nonnull
    private ItemStack regionItem(@Nonnull Player player, @Nonnull RentRegion rent, boolean withActions) {
        List<String> groups = rent.getGroupNames();
        String groupText = groups.isEmpty() ? text("gui-noGroup") : String.join(", ", groups);

        List<String> lore = new ArrayList<>();
        addLine(lore, text("gui-regionGroup", rent, groupText));
        addLine(lore, text("gui-regionPrice", rent));
        addLine(lore, text("gui-regionTimeLeft", rent));
        addLine(lore, text("gui-regionUntil", rent));
        if (rent.getMaxExtends() >= 0) {
            addLine(lore, text("gui-regionExtendsLeft", rent));
        }
        if (withActions) {
            boolean teleport = TeleportCommand.canUse(player, rent);
            boolean extend = player.hasPermission("areashop.rent");
            boolean unrent = UnrentCommand.canUse(player, rent);
            if (teleport || extend || unrent) {
                lore.add("");
            }
            if (teleport) {
                addLine(lore, text("gui-actionTeleport", rent));
            }
            if (extend) {
                addLine(lore, text("gui-actionExtend", rent));
            }
            if (unrent) {
                addLine(lore, text("gui-actionUnrent", rent));
            }
        }
        return item(Material.GRASS_BLOCK, text("gui-regionName", rent), lore);
    }

    @Nonnull
    private List<String> lore(@Nonnull RentRegion rent, @Nonnull String key) {
        List<String> lore = new ArrayList<>();
        addLine(lore, text(key, rent));
        return lore;
    }

    private static void addLine(@Nonnull List<String> lore, @Nonnull String line) {
        // An empty translation hides the line
        if (!line.isEmpty()) {
            lore.add(line);
        }
    }

    @Nonnull
    private static ItemStack item(@Nonnull Material material, @Nonnull String name, @Nonnull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + name);
            List<String> resetLore = new ArrayList<>(lore.size());
            for (String line : lore) {
                resetLore.add(ChatColor.RESET + line);
            }
            meta.setLore(resetLore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Render a language string as a single line with legacy colour codes, as used by item names and lore.
     * @param key          The language key
     * @param replacements The replacements to apply
     * @return The rendered text
     */
    @Nonnull
    private static String text(@Nonnull String key, @Nonnull Object... replacements) {
        Message message = Message.fromKey(key).replacements(replacements);
        if (AreaShop.useMiniMessage()) {
            String raw = String.join("", message.doReplacements().getRaw());
            return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(raw));
        }
        return message.getPlain().replace("\n", " ").stripTrailing();
    }

    /**
     * Opening or closing inventories from inside a click event is not allowed, so do it on the next tick.
     * @param runnable The action to run
     */
    private void later(@Nonnull Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
