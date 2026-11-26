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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainMenu implements InventoryHolder {

    private final CookieCSGPlugin plugin;
    private final Player player;
    private final int page;
    private final Inventory inventory;

    public MainMenu(CookieCSGPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, page);
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "CS:GO Weapons Manager");
        render();
    }

    private void render() {
        inventory.clear();
        List<WeaponDefinition> weapons = new ArrayList<>(plugin.getWeaponManager().getWeapons().values());
        int totalPages = Math.max(1, (int) Math.ceil((double) weapons.size() / 45.0));
        int currentPage = Math.min(page, totalPages - 1);
        int startIndex = currentPage * 45;
        int endIndex = Math.min(startIndex + 45, weapons.size());

        for (int i = startIndex; i < endIndex; i++) {
            WeaponDefinition weapon = weapons.get(i);
            int slot = i - startIndex;
            inventory.setItem(slot, buildWeaponIcon(weapon));
        }

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, border);
        }

        if (currentPage > 0) {
            inventory.setItem(45, createItem(
                Material.ARROW,
                ChatColor.YELLOW + "« Previous Page (" + currentPage + "/" + totalPages + ")"
            ));
        }

        inventory.setItem(48, createItem(
            Material.COMPASS,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Weapons List",
            ChatColor.GRAY + "Total: " + ChatColor.WHITE + weapons.size() + " weapons",
            ChatColor.GRAY + "Current page: " + ChatColor.WHITE + (currentPage + 1) + "/" + totalPages
        ));

        inventory.setItem(49, createItem(
            Material.NETHER_STAR,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Create New Weapon",
            ChatColor.GRAY + "Click to create a new weapon",
            ChatColor.GRAY + "and open the editor menu."
        ));

        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createItem(
                Material.ARROW,
                ChatColor.YELLOW + "Next Page (" + (currentPage + 2) + "/" + totalPages + ") »"
            ));
        }
    }

    private ItemStack buildWeaponIcon(WeaponDefinition weapon) {
        Material mat = weapon.getMaterial();
        if (mat == null || mat.isAir()) {
            mat = Material.IRON_HOE;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName()));
            int modelData = weapon.getAnimation() != null ? weapon.getAnimation().getIdleModelData() : 0;
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
            meta.setItemModel(plugin.getWeaponManager().resolveItemModel(weapon, modelData));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + weapon.getId());
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + (weapon.getType() != null ? weapon.getType().name() : "RIFLE"));
            if (weapon.getStats() != null) {
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.RED + weapon.getStats().getDamage());
                lore.add(ChatColor.GRAY + "Clip Size: " + ChatColor.AQUA + weapon.getStats().getClipSize() + " rounds");
            }
            lore.add(ChatColor.GRAY + "ModelData: " + ChatColor.GOLD + modelData);
            lore.add("");
            lore.add(ChatColor.GREEN + "Left-Click: Receive weapon to test");
            lore.add(ChatColor.YELLOW + "Right-Click: Edit weapon");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
        List<WeaponDefinition> weapons = new ArrayList<>(plugin.getWeaponManager().getWeapons().values());
        int totalPages = Math.max(1, (int) Math.ceil((double) weapons.size() / 45.0));
        int currentPage = Math.min(page, totalPages - 1);

        if (rawSlot == 45 && currentPage > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openMainMenu(player, currentPage - 1);
            return;
        }

        if (rawSlot == 53 && currentPage < totalPages - 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            plugin.getGuiManager().openMainMenu(player, currentPage + 1);
            return;
        }

        if (rawSlot == 49) {
            String newId = "weapon_" + (weapons.size() + 1);
            int counter = 1;
            while (plugin.getWeaponManager().getWeapon(newId) != null) {
                newId = "weapon_" + (weapons.size() + counter++);
            }
            WeaponDefinition created = plugin.getWeaponManager().createWeapon(newId, WeaponType.RIFLE);
            plugin.getWeaponManager().saveWeapon(created);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Created new weapon: " + newId);
            plugin.getGuiManager().openWeaponEditor(player, created);
            return;
        }

        if (rawSlot >= 0 && rawSlot < 45) {
            int index = currentPage * 45 + rawSlot;
            if (index < weapons.size()) {
                WeaponDefinition selected = weapons.get(index);
                if (event.isRightClick()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    plugin.getGuiManager().openWeaponEditor(player, selected);
                } else {
                    ItemStack testItem = plugin.getWeaponManager().buildItemStack(selected);
                    if (testItem != null) {
                        player.getInventory().addItem(testItem);
                        if (plugin.getAnimationEngine() != null) {
                            plugin.getAnimationEngine().syncHeldWeaponState(player);
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "Received weapon: " + ChatColor.translateAlternateColorCodes('&', selected.getDisplayName()));
                    }
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
