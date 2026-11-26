package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponAnimation;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnimationFramesMenu implements InventoryHolder {

    private final CookieCSGPlugin plugin;
    private final Player player;
    private final WeaponDefinition weapon;
    private final Inventory inventory;

    public AnimationFramesMenu(CookieCSGPlugin plugin, Player player, WeaponDefinition weapon) {
        this.plugin = plugin;
        this.player = player;
        this.weapon = weapon;
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Frames: " + weapon.getId());
        render();
    }

    public void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        WeaponAnimation anim = weapon.getAnimation();
        if (anim == null) {
            anim = new WeaponAnimation();
            weapon.setAnimation(anim);
        }

        inventory.setItem(10, createItem(
            Material.ITEM_FRAME,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Idle Model Data",
            ChatColor.GRAY + "Current Model ID: " + ChatColor.WHITE + anim.getIdleModelData(),
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1",
            ChatColor.GRAY + "Shift Left-Click: +10 | Shift Right-Click: -10",
            ChatColor.YELLOW + "Click with a cursor item to assign its ModelData"
        ));

        inventory.setItem(12, createItem(
            Material.IRON_SWORD,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Deploy Model Data",
            ChatColor.GRAY + "Deploy Model ID: " + ChatColor.WHITE + anim.getDeployModelData(),
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1",
            ChatColor.GRAY + "Shift Left-Click: +10 | Shift Right-Click: -10",
            ChatColor.YELLOW + "Click with a cursor item to assign its ModelData"
        ));

        inventory.setItem(13, createItem(
            Material.CLOCK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Deploy Ticks",
            ChatColor.GRAY + "Deploy duration: " + ChatColor.WHITE + anim.getDeployTicks() + " ticks (" + String.format("%.2f", anim.getDeployTicks() / 20.0) + "s)",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick",
            ChatColor.GRAY + "Shift-Click: +/- 5 ticks"
        ));

        inventory.setItem(14, createItem(
            Material.SPYGLASS,
            ChatColor.GREEN + "" + ChatColor.BOLD + "ADS Model Data",
            ChatColor.GRAY + "Iron Sights ADS Model ID: " + ChatColor.WHITE + anim.getAdsModelData(),
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1",
            ChatColor.GRAY + "Shift Left-Click: +10 | Shift Right-Click: -10",
            ChatColor.YELLOW + "Click with a cursor item to assign its ModelData"
        ));

        inventory.setItem(16, createItem(
            Material.BLAZE_ROD,
            ChatColor.RED + "" + ChatColor.BOLD + "ADS Fire Model Data",
            ChatColor.GRAY + "ADS recoil fire Model ID: " + ChatColor.WHITE + anim.getAdsFireModelData(),
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1",
            ChatColor.GRAY + "Shift Left-Click: +10 | Shift Right-Click: -10",
            ChatColor.YELLOW + "Click with a cursor item to assign its ModelData"
        ));

        List<Integer> fireFrames = anim.getFireFrames() != null ? anim.getFireFrames() : new ArrayList<>();
        inventory.setItem(28, createItem(
            Material.FIRE_CHARGE,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Fire Animation Frames (" + fireFrames.size() + ")",
            ChatColor.GRAY + "Frames: " + ChatColor.WHITE + fireFrames,
            "",
            ChatColor.GRAY + "Left-Click: Append frame (last + 1)",
            ChatColor.GRAY + "Right-Click: Remove last frame",
            ChatColor.GRAY + "Shift Right-Click: Clear all frames",
            ChatColor.YELLOW + "Click with a cursor item to append its ModelData"
        ));

        List<Integer> inspectFrames = anim.getInspectFrames() != null ? anim.getInspectFrames() : new ArrayList<>();
        inventory.setItem(30, createItem(
            Material.AMETHYST_SHARD,
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Inspect Frames (" + inspectFrames.size() + ")",
            ChatColor.GRAY + "Frame count: " + ChatColor.WHITE + inspectFrames.size(),
            ChatColor.DARK_GRAY + (inspectFrames.size() > 8 ? inspectFrames.subList(0, 8) + "..." : inspectFrames.toString()),
            "",
            ChatColor.GRAY + "Left-Click: Append frame",
            ChatColor.GRAY + "Right-Click: Remove last frame",
            ChatColor.GRAY + "Shift Right-Click: Clear all frames",
            ChatColor.YELLOW + "Click with a cursor item to append its ModelData"
        ));

        inventory.setItem(31, createItem(
            Material.REPEATER,
            ChatColor.WHITE + "" + ChatColor.BOLD + "Inspect Tick Rate",
            ChatColor.GRAY + "Inspect playback rate: " + ChatColor.WHITE + anim.getInspectTickRate() + " ticks/frame",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick"
        ));

        List<Integer> reloadFrames = anim.getReloadFrames() != null ? anim.getReloadFrames() : new ArrayList<>();
        inventory.setItem(34, createItem(
            Material.CROSSBOW,
            ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Reload Frames (" + reloadFrames.size() + ")",
            ChatColor.GRAY + "Frame count: " + ChatColor.WHITE + reloadFrames.size(),
            ChatColor.DARK_GRAY + (reloadFrames.size() > 8 ? reloadFrames.subList(0, 8) + "..." : reloadFrames.toString()),
            "",
            ChatColor.GRAY + "Left-Click: Append frame",
            ChatColor.GRAY + "Right-Click: Remove last frame",
            ChatColor.GRAY + "Shift Right-Click: Clear all frames",
            ChatColor.YELLOW + "Click with a cursor item to append its ModelData"
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

    private int extractCursorModel(ItemStack cursor) {
        if (cursor != null && cursor.hasItemMeta() && cursor.getItemMeta().hasCustomModelData()) {
            return cursor.getItemMeta().getCustomModelData();
        }
        return -1;
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        WeaponAnimation anim = weapon.getAnimation();
        if (anim == null) {
            anim = new WeaponAnimation();
            weapon.setAnimation(anim);
        }

        ItemStack cursor = event.getCursor();
        int cursorModel = extractCursorModel(cursor);

        if (rawSlot == 10) {
            if (cursorModel >= 0) {
                anim.setIdleModelData(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) delta = -delta;
                anim.setIdleModelData(Math.max(0, anim.getIdleModelData() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            render();
            return;
        }

        if (rawSlot == 12) {
            if (cursorModel >= 0) {
                anim.setDeployModelData(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) delta = -delta;
                anim.setDeployModelData(Math.max(0, anim.getDeployModelData() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            render();
            return;
        }

        if (rawSlot == 13) {
            int delta = event.isShiftClick() ? 5 : 1;
            if (event.isRightClick()) delta = -delta;
            anim.setDeployTicks(Math.max(1, anim.getDeployTicks() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 14) {
            if (cursorModel >= 0) {
                anim.setAdsModelData(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) delta = -delta;
                anim.setAdsModelData(Math.max(0, anim.getAdsModelData() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            render();
            return;
        }

        if (rawSlot == 16) {
            if (cursorModel >= 0) {
                anim.setAdsFireModelData(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) delta = -delta;
                anim.setAdsFireModelData(Math.max(0, anim.getAdsFireModelData() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            render();
            return;
        }

        if (rawSlot == 28) {
            List<Integer> list = new ArrayList<>(anim.getFireFrames() != null ? anim.getFireFrames() : List.of());
            if (cursorModel >= 0) {
                list.add(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else if (event.isShiftClick() && event.isRightClick()) {
                list.clear();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            } else if (event.isRightClick()) {
                if (!list.isEmpty()) {
                    list.remove(list.size() - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                }
            } else {
                int next = list.isEmpty() ? (anim.getIdleModelData() > 0 ? anim.getIdleModelData() + 1 : 100001) : list.get(list.size() - 1) + 1;
                list.add(next);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }
            anim.setFireFrames(list);
            render();
            return;
        }

        if (rawSlot == 30) {
            List<Integer> list = new ArrayList<>(anim.getInspectFrames() != null ? anim.getInspectFrames() : List.of());
            if (cursorModel >= 0) {
                list.add(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else if (event.isShiftClick() && event.isRightClick()) {
                list.clear();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            } else if (event.isRightClick()) {
                if (!list.isEmpty()) {
                    list.remove(list.size() - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                }
            } else {
                int next = list.isEmpty() ? (anim.getIdleModelData() > 0 ? anim.getIdleModelData() + 10 : 100110) : list.get(list.size() - 1) + 1;
                list.add(next);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }
            anim.setInspectFrames(list);
            render();
            return;
        }

        if (rawSlot == 31) {
            int delta = event.isRightClick() ? -1 : 1;
            anim.setInspectTickRate(Math.max(1, anim.getInspectTickRate() + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            render();
            return;
        }

        if (rawSlot == 34) {
            List<Integer> list = new ArrayList<>(anim.getReloadFrames() != null ? anim.getReloadFrames() : List.of());
            if (cursorModel >= 0) {
                list.add(cursorModel);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else if (event.isShiftClick() && event.isRightClick()) {
                list.clear();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            } else if (event.isRightClick()) {
                if (!list.isEmpty()) {
                    list.remove(list.size() - 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                }
            } else {
                int next = list.isEmpty() ? (anim.getIdleModelData() > 0 ? anim.getIdleModelData() + 20 : 100120) : list.get(list.size() - 1) + 1;
                list.add(next);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }
            anim.setReloadFrames(list);
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
