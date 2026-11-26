package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeaponEditorMenu implements InventoryHolder {

    private final CookieCSGPlugin plugin;
    private final Player player;
    private final WeaponDefinition weapon;
    private final Inventory inventory;

    public WeaponEditorMenu(CookieCSGPlugin plugin, Player player, WeaponDefinition weapon) {
        this.plugin = plugin;
        this.player = player;
        this.weapon = weapon;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Weapon: " + weapon.getId());
        render();
    }

    public void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack previewItem = plugin.getWeaponManager().buildItemStack(weapon);
        if (previewItem == null) {
            Material mat = weapon.getMaterial() != null ? weapon.getMaterial() : Material.IRON_HOE;
            previewItem = new ItemStack(mat);
            ItemMeta meta = previewItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName()));
                if (weapon.getAnimation() != null && weapon.getAnimation().getIdleModelData() > 0) {
                    meta.setCustomModelData(weapon.getAnimation().getIdleModelData());
                }
                int fallbackCmd = weapon.getAnimation() != null ? weapon.getAnimation().getIdleModelData() : 0;
                meta.setItemModel(plugin.getWeaponManager().resolveItemModel(weapon, fallbackCmd));
                previewItem.setItemMeta(meta);
            }
        }
        inventory.setItem(4, previewItem);

        int idleModel = weapon.getAnimation() != null ? weapon.getAnimation().getIdleModelData() : 0;
        inventory.setItem(8, createItem(
            Material.HOPPER,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Quick Import Slot",
            ChatColor.YELLOW + "Drag and drop or click an item here",
            ChatColor.GRAY + "Auto-detects Material & CustomModelData",
            ChatColor.GRAY + "Current material: " + ChatColor.AQUA + weapon.getMaterial().name(),
            ChatColor.GRAY + "Current ModelData: " + ChatColor.GREEN + idleModel
        ));

        WeaponType currentType = weapon.getType() != null ? weapon.getType() : WeaponType.RIFLE;
        inventory.setItem(19, createItem(
            Material.DIAMOND_SWORD,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Weapon Type: " + currentType.name(),
            ChatColor.GRAY + "Left-Click: Next type",
            ChatColor.GRAY + "Right-Click: Previous type",
            ChatColor.DARK_GRAY + "RIFLE, PISTOL, SNIPER, SHOTGUN, SMG, KNIFE, AXE"
        ));

        inventory.setItem(21, createItem(
            Material.ARMOR_STAND,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Animation Frames Editor",
            ChatColor.GRAY + "Configure CustomModelData for states:",
            ChatColor.GRAY + "Idle, Deploy, ADS, Fire, Inspect, Reload.",
            "",
            ChatColor.YELLOW + "Click to open Animation Frames Menu"
        ));

        inventory.setItem(23, createItem(
            Material.REDSTONE,
            ChatColor.RED + "" + ChatColor.BOLD + "Combat Stats Editor",
            ChatColor.GRAY + "Configure combat attributes:",
            ChatColor.GRAY + "Damage, Headshot Multiplier, FireRate,",
            ChatColor.GRAY + "ClipSize, MaxAmmo, ReloadTicks.",
            "",
            ChatColor.YELLOW + "Click to open Combat Stats Menu"
        ));

        inventory.setItem(25, createItem(
            Material.SPYGLASS,
            ChatColor.GREEN + "" + ChatColor.BOLD + "ADS & Scope Editor",
            ChatColor.GRAY + "Configure aiming mechanics:",
            ChatColor.GRAY + "Toggle ADS, Zoom FOV,",
            ChatColor.GRAY + "Spread Multiplier, Recoil Multiplier, Scope HUD.",
            "",
            ChatColor.YELLOW + "Click to open ADS Editor Menu"
        ));

        inventory.setItem(45, createItem(
            Material.EMERALD_BLOCK,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Save Config (Save YAML)",
            ChatColor.GRAY + "Write all settings to YAML file:",
            ChatColor.DARK_GREEN + "plugins/CookieCSG/weapons/" + weapon.getId() + ".yml",
            "",
            ChatColor.YELLOW + "Click to save"
        ));

        inventory.setItem(49, createItem(
            Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "Back to Weapons List",
            ChatColor.GRAY + "Return to the main weapons manager menu."
        ));

        inventory.setItem(53, createItem(
            Material.CHEST,
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Get Test Weapon",
            ChatColor.GRAY + "Receive this weapon item in your inventory",
            ChatColor.GRAY + "to test combat mechanics and animations.",
            "",
            ChatColor.YELLOW + "Click to receive"
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

    public void handleQuickImport(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }

        Material newMaterial = cursor.getType();
        weapon.setMaterial(newMaterial);

        int customModel = 0;
        if (cursor.hasItemMeta() && cursor.getItemMeta().hasCustomModelData()) {
            customModel = cursor.getItemMeta().getCustomModelData();
        }
        if (weapon.getAnimation() != null && customModel > 0) {
            weapon.getAnimation().setIdleModelData(customModel);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Quick Import succeeded: " + newMaterial.name() + " [ModelData: " + customModel + "]");
        render();
    }

    public void handleQuickImportDrag(InventoryDragEvent event) {
        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType().isAir()) {
            return;
        }

        Material newMaterial = dragged.getType();
        weapon.setMaterial(newMaterial);

        int customModel = 0;
        if (dragged.hasItemMeta() && dragged.getItemMeta().hasCustomModelData()) {
            customModel = dragged.getItemMeta().getCustomModelData();
        }
        if (weapon.getAnimation() != null && customModel > 0) {
            weapon.getAnimation().setIdleModelData(customModel);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Quick Import succeeded: " + newMaterial.name() + " [ModelData: " + customModel + "]");
        render();
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot == 8) {
            handleQuickImport(event);
            return;
        }

        if (rawSlot == 19) {
            WeaponType[] types = WeaponType.values();
            WeaponType current = weapon.getType() != null ? weapon.getType() : WeaponType.RIFLE;
            int currentIndex = current.ordinal();
            int nextIndex;
            if (event.isRightClick()) {
                nextIndex = (currentIndex - 1 + types.length) % types.length;
            } else {
                nextIndex = (currentIndex + 1) % types.length;
            }
            weapon.setType(types[nextIndex]);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 21) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openFramesEditor(player, weapon);
            return;
        }

        if (rawSlot == 23) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openStatsEditor(player, weapon);
            return;
        }

        if (rawSlot == 25) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openAdsEditor(player, weapon);
            return;
        }

        if (rawSlot == 45) {
            plugin.getWeaponManager().saveWeapon(weapon);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Saved weapon config [" + weapon.getId() + "] to YAML!");
            return;
        }

        if (rawSlot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openMainMenu(player);
            return;
        }

        if (rawSlot == 53) {
            ItemStack testItem = plugin.getWeaponManager().buildItemStack(weapon);
            if (testItem != null) {
                player.getInventory().addItem(testItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "Received test weapon: " + ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName()));
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
