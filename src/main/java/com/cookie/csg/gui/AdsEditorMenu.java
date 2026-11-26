package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponAds;
import com.cookie.csg.model.WeaponDefinition;
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

public class AdsEditorMenu implements InventoryHolder {

    private final CookieCSGPlugin plugin;
    private final Player player;
    private final WeaponDefinition weapon;
    private final Inventory inventory;

    public AdsEditorMenu(CookieCSGPlugin plugin, Player player, WeaponDefinition weapon) {
        this.plugin = plugin;
        this.player = player;
        this.weapon = weapon;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "ADS: " + weapon.getId());
        render();
    }

    public void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        WeaponAds ads = weapon.getAdsConfig();
        if (ads == null) {
            ads = new WeaponAds();
            weapon.setAdsConfig(ads);
        }

        inventory.setItem(11, createItem(
            ads.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
            ChatColor.GREEN + "" + ChatColor.BOLD + "ADS Aiming: " + (ads.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"),
            ChatColor.GRAY + "Centers the weapon sights for aiming",
            ChatColor.GRAY + (plugin.isRightClickFireForGuns() ? "when Left-Clicking." : "when Right-Clicking."),
            "",
            ChatColor.YELLOW + "Click to Toggle"
        ));

        inventory.setItem(13, createItem(
            Material.SPYGLASS,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Zoom Level",
            ChatColor.GRAY + "FOV magnification: " + ChatColor.WHITE + "Level " + ads.getZoomLevel(),
            ChatColor.DARK_GRAY + "(Level 1: Iron Sights, Level 2: 1.5x Optic, Level 4+: 8x Scope)",
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1"
        ));

        inventory.setItem(15, createItem(
            ads.isScopeOverlay() ? Material.ENDER_EYE : Material.TINTED_GLASS,
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Scope HUD Overlay: " + (ads.isScopeOverlay() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"),
            ChatColor.GRAY + "Uses sniper scope optical lens configuration.",
            "",
            ChatColor.YELLOW + "Click to Toggle"
        ));

        int spreadPct = (int) Math.round((1.0 - ads.getSpreadMultiplier()) * 100.0);
        inventory.setItem(29, createItem(
            Material.TARGET,
            ChatColor.GOLD + "" + ChatColor.BOLD + "ADS Spread Multiplier",
            ChatColor.GRAY + "Spread factor while aiming: " + ChatColor.WHITE + String.format("%.2f", ads.getSpreadMultiplier()),
            ChatColor.GRAY + "Accuracy bonus: " + ChatColor.GREEN + spreadPct + "%",
            "",
            ChatColor.GRAY + "Left-Click: +0.05 | Right-Click: -0.05",
            ChatColor.GRAY + "Shift Left-Click: +0.10 | Shift Right-Click: -0.10"
        ));

        int recoilPct = (int) Math.round((1.0 - ads.getRecoilMultiplier()) * 100.0);
        inventory.setItem(33, createItem(
            Material.PISTON,
            ChatColor.RED + "" + ChatColor.BOLD + "ADS Recoil Multiplier",
            ChatColor.GRAY + "Recoil factor while aiming: " + ChatColor.WHITE + String.format("%.2f", ads.getRecoilMultiplier()),
            ChatColor.GRAY + "Stability bonus: " + ChatColor.GREEN + recoilPct + "%",
            "",
            ChatColor.GRAY + "Left-Click: +0.05 | Right-Click: -0.05",
            ChatColor.GRAY + "Shift Left-Click: +0.10 | Shift Right-Click: -0.10"
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
        WeaponAds ads = weapon.getAdsConfig();
        if (ads == null) {
            ads = new WeaponAds();
            weapon.setAdsConfig(ads);
        }

        if (rawSlot == 11) {
            ads.setEnabled(!ads.isEnabled());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            render();
            return;
        }

        if (rawSlot == 13) {
            int delta = event.isRightClick() ? -1 : 1;
            int newZoom = Math.max(1, Math.min(10, ads.getZoomLevel() + delta));
            ads.setZoomLevel(newZoom);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 15) {
            ads.setScopeOverlay(!ads.isScopeOverlay());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            render();
            return;
        }

        if (rawSlot == 29) {
            double delta = event.isShiftClick() ? 0.10 : 0.05;
            if (event.isRightClick()) delta = -delta;
            double newVal = Math.max(0.0, Math.min(1.0, Math.round((ads.getSpreadMultiplier() + delta) * 100.0) / 100.0));
            ads.setSpreadMultiplier(newVal);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 33) {
            double delta = event.isShiftClick() ? 0.10 : 0.05;
            if (event.isRightClick()) delta = -delta;
            double newVal = Math.max(0.0, Math.min(1.0, Math.round((ads.getRecoilMultiplier() + delta) * 100.0) / 100.0));
            ads.setRecoilMultiplier(newVal);
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
