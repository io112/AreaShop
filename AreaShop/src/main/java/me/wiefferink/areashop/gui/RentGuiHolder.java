package me.wiefferink.areashop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Marks an inventory as one of the rented regions GUI screens and keeps track of what is shown in it.
 */
public class RentGuiHolder implements InventoryHolder {

    public enum View {
        LIST,
        CONFIRM_UNRENT
    }

    private final View view;
    private final int page;
    private final String confirmRegion;
    private final Map<Integer, String> regionSlots = new HashMap<>();
    private Inventory inventory;

    RentGuiHolder(@Nonnull View view, int page, @Nullable String confirmRegion) {
        this.view = view;
        this.page = page;
        this.confirmRegion = confirmRegion;
    }

    @Nonnull
    public View getView() {
        return view;
    }

    /**
     * Get the page of the list view, for the confirm view this is the page to return to.
     * @return The zero-based page number
     */
    public int getPage() {
        return page;
    }

    /**
     * Get the region that the confirm view asks about.
     * @return The region name, or null for the list view
     */
    @Nullable
    public String getConfirmRegion() {
        return confirmRegion;
    }

    void setRegion(int slot, @Nonnull String regionName) {
        regionSlots.put(slot, regionName);
    }

    /**
     * Get the region shown in a slot of the list view.
     * @param slot The raw slot number
     * @return The region name, or null if the slot does not show a region
     */
    @Nullable
    public String getRegion(int slot) {
        return regionSlots.get(slot);
    }

    void setInventory(@Nonnull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }
}
