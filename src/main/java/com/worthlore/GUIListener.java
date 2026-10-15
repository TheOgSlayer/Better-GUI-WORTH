package com.worthlore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class GUIListener implements Listener {

    private final WorthLore plugin;

    public GUIListener(WorthLore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof WorthGUI gui)) return;

        // Nothing can be taken, moved or shift-clicked while the GUI is open
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        UUID id = player.getUniqueId();
        int slot = e.getSlot();

        if (slot == WorthGUI.PREV_SLOT && gui.getPage() > 0) {
            new WorthGUI(plugin, gui.getPage() - 1, id).open(player);

        } else if (slot == WorthGUI.NEXT_SLOT && gui.getPage() < gui.getPages() - 1) {
            new WorthGUI(plugin, gui.getPage() + 1, id).open(player);

        } else if (slot == WorthGUI.SORT_SLOT) {
            // one button that cycles: alphabetical -> lowest-highest -> highest-lowest
            plugin.setSort(id, plugin.getSort(id).next());
            new WorthGUI(plugin, 0, id).open(player);

        } else if (slot == WorthGUI.SEARCH_SLOT) {
            if (e.isRightClick()) {
                plugin.clearSearch(id);
                new WorthGUI(plugin, 0, id).open(player);
            } else {
                plugin.setAwaitingSearch(id, true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    player.sendMessage(plugin.msg("search-prompt",
                            "&eType the item name you want to search for in chat &7(or type &ccancel&7)."));
                });
            }
        }
    }

    /** Captures the player's next chat message as their search text. */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();
        if (!plugin.isAwaitingSearch(id)) return;

        e.setCancelled(true);
        plugin.setAwaitingSearch(id, false);
        String text = e.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!text.equalsIgnoreCase("cancel")) plugin.setSearch(id, text);
            if (player.isOnline()) new WorthGUI(plugin, 0, id).open(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.setAwaitingSearch(e.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof WorthGUI) {
            e.setCancelled(true);
        }
    }
}
