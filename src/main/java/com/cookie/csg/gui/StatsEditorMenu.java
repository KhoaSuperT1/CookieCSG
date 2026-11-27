package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponRecoil;
import com.cookie.csg.model.WeaponStats;
import com.cookie.csg.model.WeaponType;
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
import java.util.List;

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
        WeaponRecoil recoil = weapon.getRecoil();
        if (recoil == null) {
            recoil = new WeaponRecoil();
            weapon.setRecoil(recoil);
        }
        WeaponType type = weapon.getType() != null ? weapon.getType() : WeaponType.RIFLE;

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

        inventory.setItem(19, createItem(
            Material.CROSSBOW,
            ChatColor.RED + "" + ChatColor.BOLD + "Recoil Vertical",
            ChatColor.GRAY + "Vertical kick: " + ChatColor.WHITE + String.format("%.2f", recoil.getVertical()),
            "",
            ChatColor.GRAY + "Left-Click: +0.10 | Right-Click: -0.10",
            ChatColor.GRAY + "Shift-Click: +/- 0.50"
        ));

        inventory.setItem(20, createItem(
            Material.COMPARATOR,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Recoil Horizontal",
            ChatColor.GRAY + "Horizontal sway: " + ChatColor.WHITE + String.format("%.2f", recoil.getHorizontal()),
            "",
            ChatColor.GRAY + "Left-Click: +0.05 | Right-Click: -0.05",
            ChatColor.GRAY + "Shift-Click: +/- 0.25"
        ));

        inventory.setItem(21, createItem(
            Material.SNOWBALL,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Hipfire Base Spread",
            ChatColor.GRAY + "Spread radius: " + ChatColor.WHITE + String.format("%.3f", recoil.getSpread()),
            "",
            ChatColor.GRAY + "Left-Click: +0.005 | Right-Click: -0.005",
            ChatColor.GRAY + "Shift-Click: +/- 0.020"
        ));

        inventory.setItem(22, createItem(
            Material.RABBIT_FOOT,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Sprint Spread Multiplier",
            ChatColor.GRAY + "Multiplier: " + ChatColor.WHITE + String.format("%.2f", recoil.getSprintSpreadMultiplier()) + "x",
            "",
            ChatColor.GRAY + "Left-Click: +0.10 | Right-Click: -0.10",
            ChatColor.GRAY + "Shift-Click: +/- 0.50"
        ));

        String modeText = type.isMelee()
            ? "MELEE (Combo / Heavy)"
            : stats.resolveSupportedFireModes(type) + " (Default: " + stats.resolveDefaultFireMode(type) + ")";
        inventory.setItem(23, createItem(
            Material.LEVER,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Fire Mode Preset",
            ChatColor.GRAY + "Modes: " + ChatColor.WHITE + modeText,
            "",
            type.isMelee() ? ChatColor.DARK_GRAY + "Melee weapons use slash/heavy controls" : ChatColor.YELLOW + "Click to cycle fire mode preset"
        ));

        inventory.setItem(24, createItem(
            Material.FEATHER,
            ChatColor.WHITE + "" + ChatColor.BOLD + "Move Speed Multiplier",
            ChatColor.GRAY + "Walk speed factor: " + ChatColor.WHITE + String.format("%.2f", stats.getMovementSpeedMultiplier()) + "x",
            "",
            ChatColor.GRAY + "Left-Click: +0.02 | Right-Click: -0.02",
            ChatColor.GRAY + "Shift-Click: +/- 0.10"
        ));

        if (type == WeaponType.KNIFE) {
            inventory.setItem(25, createItem(
                Material.DIAMOND_SWORD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Backstab Multiplier",
                ChatColor.GRAY + "Backstab bonus: " + ChatColor.WHITE + String.format("%.2f", stats.getBackstabMultiplier()) + "x",
                "",
                ChatColor.GRAY + "Left-Click: +0.25 | Right-Click: -0.25",
                ChatColor.GRAY + "Shift-Click: +/- 1.00"
            ));
        } else if (type == WeaponType.AXE) {
            inventory.setItem(25, createItem(
                Material.SHIELD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Shield Break Chance",
                ChatColor.GRAY + "Break chance: " + ChatColor.WHITE + String.format("%.0f%%", stats.getShieldBreakChance() * 100.0),
                "",
                ChatColor.GRAY + "Left-Click: +0.10 | Right-Click: -0.10",
                ChatColor.GRAY + "Shift-Click: +/- 0.25"
            ));
        } else {
            inventory.setItem(25, createItem(
                Material.REPEATER,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Tactical Reload Ticks",
                ChatColor.GRAY + "Tactical duration: " + ChatColor.WHITE + stats.getReloadTacticalTicks() + " ticks",
                "",
                ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick",
                ChatColor.GRAY + "Shift-Click: +/- 5 ticks"
            ));
        }

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
        WeaponRecoil recoil = weapon.getRecoil();
        if (recoil == null) {
            recoil = new WeaponRecoil();
            weapon.setRecoil(recoil);
        }
        WeaponType type = weapon.getType() != null ? weapon.getType() : WeaponType.RIFLE;

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

        if (rawSlot == 19) {
            double delta = event.isShiftClick() ? 0.5 : 0.1;
            if (event.isRightClick()) delta = -delta;
            recoil.setVertical(Math.max(0.0, Math.round((recoil.getVertical() + delta) * 100.0) / 100.0));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 20) {
            double delta = event.isShiftClick() ? 0.25 : 0.05;
            if (event.isRightClick()) delta = -delta;
            recoil.setHorizontal(Math.max(0.0, Math.round((recoil.getHorizontal() + delta) * 100.0) / 100.0));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 21) {
            double delta = event.isShiftClick() ? 0.02 : 0.005;
            if (event.isRightClick()) delta = -delta;
            recoil.setSpread(Math.max(0.0, Math.min(1.0, Math.round((recoil.getSpread() + delta) * 1000.0) / 1000.0)));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 22) {
            double delta = event.isShiftClick() ? 0.5 : 0.1;
            if (event.isRightClick()) delta = -delta;
            recoil.setSprintSpreadMultiplier(Math.max(1.0, Math.round((recoil.getSprintSpreadMultiplier() + delta) * 100.0) / 100.0));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 23) {
            if (!type.isMelee()) {
                List<List<FireMode>> presets = List.of(
                    List.of(FireMode.AUTO, FireMode.SEMI_AUTO),
                    List.of(FireMode.SEMI_AUTO),
                    List.of(FireMode.AUTO),
                    List.of(FireMode.SINGLE)
                );
                List<FireMode> cur = stats.resolveSupportedFireModes(type);
                int idx = presets.indexOf(cur);
                int nextIdx = event.isRightClick()
                    ? (idx <= 0 ? presets.size() - 1 : idx - 1)
                    : (idx + 1) % presets.size();
                List<FireMode> nextModes = presets.get(nextIdx);
                stats.setFireModes(nextModes);
                stats.setDefaultFireMode(nextModes.get(0));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                render();
            }
            return;
        }

        if (rawSlot == 24) {
            double delta = event.isShiftClick() ? 0.10 : 0.02;
            if (event.isRightClick()) delta = -delta;
            stats.setMovementSpeedMultiplier(Math.max(0.3, Math.min(1.5, Math.round((stats.getMovementSpeedMultiplier() + delta) * 100.0) / 100.0)));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 25) {
            if (type == WeaponType.KNIFE) {
                double delta = event.isShiftClick() ? 1.0 : 0.25;
                if (event.isRightClick()) delta = -delta;
                stats.setBackstabMultiplier(Math.max(1.0, Math.round((stats.getBackstabMultiplier() + delta) * 100.0) / 100.0));
            } else if (type == WeaponType.AXE) {
                double delta = event.isShiftClick() ? 0.25 : 0.10;
                if (event.isRightClick()) delta = -delta;
                stats.setShieldBreakChance(Math.max(0.0, Math.min(1.0, Math.round((stats.getShieldBreakChance() + delta) * 100.0) / 100.0)));
            } else {
                int delta = event.isShiftClick() ? 5 : 1;
                if (event.isRightClick()) delta = -delta;
                stats.setReloadTacticalTicks(Math.max(0, stats.getReloadTacticalTicks() + delta));
            }
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
