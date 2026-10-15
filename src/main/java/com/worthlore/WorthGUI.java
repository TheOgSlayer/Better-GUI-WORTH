package com.worthlore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorthGUI implements InventoryHolder {

    public static final int PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int SORT_SLOT = 47;
    public static final int INFO_SLOT = 49;
    public static final int SEARCH_SLOT = 51;
    public static final int NEXT_SLOT = 53;

    private final WorthLore plugin;
    private final int page;
    private final int pages;
    private final Inventory inventory;

    public WorthGUI(WorthLore plugin, int page, UUID viewer) {
        this.plugin = plugin;

        SortMode sort = plugin.getSort(viewer);
        String query = plugin.getSearch(viewer);

        List<Map.Entry<Material, Double>> entries = new ArrayList<>();
        for (Map.Entry<Material, Double> en : plugin.getPrices().entrySet()) {
            if (query == null || matches(en.getKey(), query)) entries.add(en);
        }
        entries.sort(sort.comparator());

        this.pages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
        this.page = Math.max(0, Math.min(page, pages - 1));

        String title = plugin.color(plugin.getConfig().getString("gui.title", "&8Item Worth &7(Page %page%/%pages%)"))
                .replace("%page%", String.valueOf(this.page + 1))
                .replace("%pages%", String.valueOf(pages));
        this.inventory = Bukkit.createInventory(this, 54, title);

        // Items
        int start = this.page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, plugin.withWorth(new ItemStack(entries.get(i).getKey())));
        }

        // Buttons
        if (this.page > 0) {
            inventory.setItem(PREV_SLOT, button(Material.ARROW,
                    plugin.getConfig().getString("gui.previous-page", "&a<< Previous Page"), null, false));
        }
        if (this.page < pages - 1) {
            inventory.setItem(NEXT_SLOT, button(Material.ARROW,
                    plugin.getConfig().getString("gui.next-page", "&aNext Page >>"), null, false));
        }

        String pageName = plugin.getConfig().getString("gui.page-item", "&ePage %page%/%pages%")
                .replace("%page%", String.valueOf(this.page + 1))
                .replace("%pages%", String.valueOf(pages));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(plugin.color("&7Items: &f" + entries.size()));
        inventory.setItem(INFO_SLOT, button(Material.PAPER, pageName, infoLore, false));

        inventory.setItem(SORT_SLOT, sortButton(sort));
        inventory.setItem(SEARCH_SLOT, searchButton(query));

        // Empty search result
        if (entries.isEmpty()) {
            List<String> lore = new ArrayList<>();
            lore.add(plugin.color("&7Nothing matches: &f" + query));
            inventory.setItem(22, button(Material.BARRIER, "&cNo items found", lore, false));
        }

        // Fill everything else with black glass for a clean, framed look
        ItemStack pane = button(Material.BLACK_STAINED_GLASS_PANE, " ", null, false);
        for (int slot = 0; slot < 54; slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, pane);
        }
    }

    private static boolean matches(Material mat, String query) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        String q = query.toLowerCase().replace('_', ' ').trim();
        return name.contains(q);
    }

    private ItemStack sortButton(SortMode current) {
        List<String> lore = new ArrayList<>();
        for (SortMode mode : SortMode.values()) {
            String label = label(mode);
            lore.add(plugin.color(mode == current ? "&a> " + label : "&7  " + label));
        }
        lore.add("");
        lore.add(plugin.color("&8Click to change sorting"));
        return button(Material.HOPPER, plugin.getConfig().getString("gui.sort-button", "&eSort Items"), lore, false);
    }

    private String label(SortMode mode) {
        switch (mode) {
            case LOW_TO_HIGH:
                return plugin.getConfig().getString("gui.sort-low-high", "Lowest to Highest");
            case HIGH_TO_LOW:
                return plugin.getConfig().getString("gui.sort-high-low", "Highest to Lowest");
            default:
                return plugin.getConfig().getString("gui.sort-alphabetical", "Alphabetical (A-Z)");
        }
    }

    private ItemStack searchButton(String query) {
        List<String> lore = new ArrayList<>();
        if (query == null) {
            lore.add(plugin.color("&7Click to search by item name"));
        } else {
            lore.add(plugin.color("&7Searching for: &f" + query));
            lore.add("");
            lore.add(plugin.color("&aLeft-click: &7new search"));
            lore.add(plugin.color("&cRight-click: &7clear search"));
        }
        return button(Material.OAK_SIGN, plugin.getConfig().getString("gui.search-button", "&eSearch Items"), lore, query != null);
    }

    private ItemStack button(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        if (lore != null) meta.setLore(lore);
        if (glow) meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        plugin.markNoWorth(meta); // never add worth lore to GUI buttons
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public int getPage() {
        return page;
    }

    public int getPages() {
        return pages;
    }

    public WorthLore getPlugin() {
        return plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
