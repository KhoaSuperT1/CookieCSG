package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    private final CookieCSGPlugin plugin;

    public GuiListener(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getInventory().getHolder() == null) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MainMenu menu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                menu.handleClick(event);
            }
            return;
        }

        if (holder instanceof WeaponEditorMenu menu) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                event.setCancelled(true);
                menu.handleClick(event);
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        if (holder instanceof AnimationFramesMenu menu) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                event.setCancelled(true);
                menu.handleClick(event);
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        if (holder instanceof StatsEditorMenu menu) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                event.setCancelled(true);
                menu.handleClick(event);
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        if (holder instanceof AdsEditorMenu menu) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                event.setCancelled(true);
                menu.handleClick(event);
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() == null || event.getInventory().getHolder() == null) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof WeaponEditorMenu menu) {
            if (event.getRawSlots().contains(8)) {
                menu.handleQuickImportDrag(event);
                event.setCancelled(true);
                return;
            }
            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (holder instanceof MainMenu || holder instanceof AnimationFramesMenu
                || holder instanceof StatsEditorMenu || holder instanceof AdsEditorMenu) {
            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
