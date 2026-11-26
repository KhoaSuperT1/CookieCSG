package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class StatsEditorMenu implements InventoryHolder {

    private final CookieCSGPlugin plugin;
    private final Player player;
    private final WeaponDefinition weapon;
    private final Inventory inventory;

    public StatsEditorMenu(CookieCSGPlugin plugin, Player player, WeaponDefinition weapon) {
        this.plugin = plugin;
        this.player = player;
        this.weapon = weapon;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Stats: " + weapon.getId());
        render();
    }

    public void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        WeaponStats stats = weapon.getStats();
        if (stats == null) {
            stats = new WeaponStats();
            weapon.setStats(stats);
        }

        inventory.setItem(11, createItem(
            Material.IRON_SWORD,
            ChatColor.RED + "" + ChatColor.BOLD + "Damage",
            ChatColor.GRAY + "Current value: " + ChatColor.WHITE + String.format("%.2f", stats.getDamage()) + " HP",
            "",
            ChatColor.GRAY + "Left-Click: +1.0 | Right-Click: -1.0",
            ChatColor.GRAY + "Shift Left-Click: +5.0 | Shift Right-Click: -5.0"
        ));

        inventory.setItem(13, createItem(
            Material.SKELETON_SKULL,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Headshot Multiplier",
            ChatColor.GRAY + "Headshot damage multiplier: " + ChatColor.WHITE + String.format("%.2f", stats.getHeadshotMultiplier()) + "x",
            "",
            ChatColor.GRAY + "Left-Click: +0.25 | Right-Click: -0.25",
            ChatColor.GRAY + "Shift Left-Click: +1.00 | Shift Right-Click: -1.00"
        ));

        inventory.setItem(15, createItem(
            Material.BLAZE_POWDER,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Fire Rate",
            ChatColor.GRAY + "Cooldown between shots: " + ChatColor.WHITE + stats.getFireRateTicks() + " ticks",
            ChatColor.DARK_GRAY + "(" + (stats.getFireRateTicks() > 0 ? (1200 / stats.getFireRateTicks()) : 0) + " RPM)",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick",
            ChatColor.GRAY + "Shift Left-Click: +5 ticks | Shift Right-Click: -5 ticks"
        ));

        inventory.setItem(29, createItem(
            Material.ARROW,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Clip Size",
            ChatColor.GRAY + "Magazine capacity: " + ChatColor.WHITE + stats.getClipSize() + " rounds",
            "",
            ChatColor.GRAY + "Left-Click: +1 round | Right-Click: -1 round",
            ChatColor.GRAY + "Shift Left-Click: +5 rounds | Shift Right-Click: -5 rounds"
        ));

        inventory.setItem(31, createItem(
            Material.SPECTRAL_ARROW,
            ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Max Ammo",
            ChatColor.GRAY + "Maximum reserve ammo: " + ChatColor.WHITE + stats.getMaxAmmo() + " rounds",
            "",
            ChatColor.GRAY + "Left-Click: +10 rounds | Right-Click: -10 rounds",
            ChatColor.GRAY + "Shift Left-Click: +30 rounds | Shift Right-Click: -30 rounds"
        ));

        inventory.setItem(33, createItem(
            Material.CLOCK,
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Reload Ticks",
            ChatColor.GRAY + "Reload duration: " + ChatColor.WHITE + stats.getReloadTicks() + " ticks (" + String.format("%.2f", stats.getReloadTicks() / 20.0) + "s)",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick",
            ChatColor.GRAY + "Shift Left-Click: +5 ticks | Shift Right-Click: -5 ticks"
        ));

        inventory.setItem(35, createItem(
            Material.BOW,
            ChatColor.WHITE + "" + ChatColor.BOLD + "Effective Range",
            ChatColor.GRAY + "Maximum range: " + ChatColor.WHITE + String.format("%.1f", stats.getRange()) + " blocks",
            "",
            ChatColor.GRAY + "Left-Click: +5.0 | Right-Click: -5.0",
            ChatColor.GRAY + "Shift Left-Click: +20.0 | Shift Right-Click: -20.0"
        ));

        inventory.setItem(49, createItem(
            Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "Back to Weapon Editor",
            ChatColor.GRAY + "Return to the weapon editor menu."
        ));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        WeaponStats stats = weapon.getStats();
        if (stats == null) {
            stats = new WeaponStats();
            weapon.setStats(stats);
        }

        if (rawSlot == 11) {
            double delta = event.isShiftClick() ? 5.0 : 1.0;
            if (event.isRightClick()) delta = -delta;
            stats.setDamage(Math.max(0.5, stats.getDamage() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 13) {
            double delta = event.isShiftClick() ? 1.0 : 0.25;
            if (event.isRightClick()) delta = -delta;
            stats.setHeadshotMultiplier(Math.max(1.0, stats.getHeadshotMultiplier() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 15) {
            int delta = event.isShiftClick() ? 5 : 1;
            if (event.isRightClick()) delta = -delta;
            stats.setFireRateTicks(Math.max(1, stats.getFireRateTicks() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 29) {
            int delta = event.isShiftClick() ? 5 : 1;
            if (event.isRightClick()) delta = -delta;
            stats.setClipSize(Math.max(1, stats.getClipSize() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 31) {
            int delta = event.isShiftClick() ? 30 : 10;
            if (event.isRightClick()) delta = -delta;
            stats.setMaxAmmo(Math.max(0, stats.getMaxAmmo() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 33) {
            int delta = event.isShiftClick() ? 5 : 1;
            if (event.isRightClick()) delta = -delta;
            stats.setReloadTicks(Math.max(5, stats.getReloadTicks() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 35) {
            double delta = event.isShiftClick() ? 20.0 : 5.0;
            if (event.isRightClick()) delta = -delta;
            stats.setRange(Math.max(5.0, stats.getRange() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openWeaponEditor(player, weapon);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
