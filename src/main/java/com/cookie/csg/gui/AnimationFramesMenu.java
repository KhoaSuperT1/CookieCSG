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
import java.util.function.Consumer;
import java.util.function.IntConsumer;

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

    private ItemStack createModelSlotItem(Material material, String title, String label, int currentId) {
        return createItem(
            material,
            title,
            ChatColor.GRAY + label + ": " + ChatColor.WHITE + currentId,
            "",
            ChatColor.GRAY + "Left-Click: +1 | Right-Click: -1",
            ChatColor.GRAY + "Shift Left-Click: +10 | Shift Right-Click: -10",
            ChatColor.YELLOW + "Click with a cursor item to assign its ModelData"
        );
    }

    private ItemStack createFrameListSlotItem(Material material, String title, List<Integer> frames) {
        List<Integer> safe = frames != null ? frames : List.of();
        return createItem(
            material,
            title + " (" + safe.size() + ")",
            ChatColor.GRAY + "Frame count: " + ChatColor.WHITE + safe.size(),
            ChatColor.DARK_GRAY + (safe.size() > 8 ? safe.subList(0, 8) + "..." : safe.toString()),
            "",
            ChatColor.GRAY + "Left-Click: +1 frame | Shift Left-Click: +5 frames",
            ChatColor.GRAY + "Right-Click: Remove last | Shift Right-Click: Clear",
            ChatColor.YELLOW + "Click with a cursor item to append its ModelData"
        );
    }

    private ItemStack createTickRateSlotItem(Material material, String title, int rate) {
        return createItem(
            material,
            title,
            ChatColor.GRAY + "Playback rate: " + ChatColor.WHITE + rate + " ticks/frame",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick"
        );
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

        inventory.setItem(10, createModelSlotItem(Material.ITEM_FRAME, ChatColor.GOLD + "" + ChatColor.BOLD + "Idle Model Data", "Current Model ID", anim.getIdleModelData()));
        inventory.setItem(11, createFrameListSlotItem(Material.FEATHER, ChatColor.YELLOW + "" + ChatColor.BOLD + "Deploy Frames", anim.getDeployFrames()));
        inventory.setItem(12, createModelSlotItem(Material.IRON_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Deploy Model Data", "Deploy Model ID", anim.getDeployModelData()));
        inventory.setItem(13, createItem(
            Material.CLOCK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Deploy Ticks",
            ChatColor.GRAY + "Deploy duration: " + ChatColor.WHITE + anim.getDeployTicks() + " ticks (" + String.format("%.2f", anim.getDeployTicks() / 20.0) + "s)",
            "",
            ChatColor.GRAY + "Left-Click: +1 tick | Right-Click: -1 tick",
            ChatColor.GRAY + "Shift-Click: +/- 5 ticks"
        ));
        inventory.setItem(14, createModelSlotItem(Material.SPYGLASS, ChatColor.GREEN + "" + ChatColor.BOLD + "ADS Model Data", "Iron Sights ADS Model ID", anim.getAdsModelData()));
        inventory.setItem(15, createFrameListSlotItem(Material.ENDER_PEARL, ChatColor.GREEN + "" + ChatColor.BOLD + "ADS-In / Stance Frames", anim.getAdsInFrames()));
        inventory.setItem(16, createModelSlotItem(Material.BLAZE_ROD, ChatColor.RED + "" + ChatColor.BOLD + "ADS Fire Model Data", "ADS recoil fire Model ID", anim.getAdsFireModelData()));

        inventory.setItem(19, createTickRateSlotItem(Material.REPEATER, ChatColor.YELLOW + "" + ChatColor.BOLD + "Deploy Tick Rate", anim.getDeployTickRate()));
        inventory.setItem(20, createTickRateSlotItem(Material.REPEATER, ChatColor.GOLD + "" + ChatColor.BOLD + "Fire Tick Rate", anim.getFireTickRate()));
        inventory.setItem(21, createTickRateSlotItem(Material.REPEATER, ChatColor.GREEN + "" + ChatColor.BOLD + "ADS Tick Rate", anim.getAdsTickRate()));
        inventory.setItem(22, createFrameListSlotItem(Material.FIREWORK_STAR, ChatColor.RED + "" + ChatColor.BOLD + "ADS-Fire Frames", anim.getAdsFireFrames()));
        inventory.setItem(23, createFrameListSlotItem(Material.SLIME_BALL, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "ADS-Out Frames", anim.getAdsOutFrames()));
        inventory.setItem(24, createFrameListSlotItem(Material.STONE_SWORD, ChatColor.AQUA + "" + ChatColor.BOLD + "Melee Slash-2 Frames", anim.getSlash2Frames()));
        inventory.setItem(25, createFrameListSlotItem(Material.DIAMOND_AXE, ChatColor.DARK_RED + "" + ChatColor.BOLD + "Melee Heavy Frames", anim.getHeavyFrames()));

        inventory.setItem(28, createFrameListSlotItem(Material.FIRE_CHARGE, ChatColor.GOLD + "" + ChatColor.BOLD + "Fire Animation Frames", anim.getFireFrames()));
        inventory.setItem(30, createFrameListSlotItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Inspect Frames", anim.getInspectFrames()));
        inventory.setItem(31, createTickRateSlotItem(Material.REPEATER, ChatColor.WHITE + "" + ChatColor.BOLD + "Inspect Tick Rate", anim.getInspectTickRate()));
        inventory.setItem(32, createFrameListSlotItem(Material.SPECTRAL_ARROW, ChatColor.BLUE + "" + ChatColor.BOLD + "Tactical Reload Frames", anim.getReloadTacticalFrames()));
        inventory.setItem(33, createTickRateSlotItem(Material.REPEATER, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Reload Tick Rate", anim.getReloadTickRate()));
        inventory.setItem(34, createFrameListSlotItem(Material.CROSSBOW, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Reload Frames", anim.getReloadFrames()));

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

    private void adjustModelDataSlot(InventoryClickEvent event, int cursorModel, int current, IntConsumer setter) {
        if (cursorModel >= 0) {
            setter.accept(cursorModel);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
            int delta = event.isShiftClick() ? 10 : 1;
            if (event.isRightClick()) delta = -delta;
            setter.accept(Math.max(0, current + delta));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
        render();
    }

    private void adjustFrameListSlot(InventoryClickEvent event, int cursorModel, List<Integer> existing, int seedOffset, Consumer<List<Integer>> setter) {
        List<Integer> list = new ArrayList<>(existing != null ? existing : List.of());
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
            int count = event.isShiftClick() ? 5 : 1;
            int idleBase = weapon.getAnimation() != null ? weapon.getAnimation().getIdleModelData() : 0;
            int next = list.isEmpty() ? (idleBase > 0 ? idleBase + seedOffset : 100000 + seedOffset) : list.get(list.size() - 1) + 1;
            for (int i = 0; i < count; i++) {
                list.add(next + i);
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        }
        setter.accept(list);
        render();
    }

    private void adjustTickRateSlot(InventoryClickEvent event, int current, IntConsumer setter) {
        int delta = event.isRightClick() ? -1 : 1;
        setter.accept(Math.max(1, current + delta));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        render();
    }

    public void handleClick(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        WeaponAnimation anim = weapon.getAnimation();
        if (anim == null) {
            anim = new WeaponAnimation();
            weapon.setAnimation(anim);
        }

        int cursorModel = extractCursorModel(event.getCursor());

        switch (rawSlot) {
            case 10 -> adjustModelDataSlot(event, cursorModel, anim.getIdleModelData(), anim::setIdleModelData);
            case 11 -> adjustFrameListSlot(event, cursorModel, anim.getDeployFrames(), 1, anim::setDeployFrames);
            case 12 -> adjustModelDataSlot(event, cursorModel, anim.getDeployModelData(), anim::setDeployModelData);
            case 13 -> {
                int delta = event.isShiftClick() ? 5 : 1;
                if (event.isRightClick()) delta = -delta;
                anim.setDeployTicks(Math.max(1, anim.getDeployTicks() + delta));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                render();
            }
            case 14 -> adjustModelDataSlot(event, cursorModel, anim.getAdsModelData(), anim::setAdsModelData);
            case 15 -> adjustFrameListSlot(event, cursorModel, anim.getAdsInFrames(), 30, anim::setAdsInFrames);
            case 16 -> adjustModelDataSlot(event, cursorModel, anim.getAdsFireModelData(), anim::setAdsFireModelData);
            case 19 -> adjustTickRateSlot(event, anim.getDeployTickRate(), anim::setDeployTickRate);
            case 20 -> adjustTickRateSlot(event, anim.getFireTickRate(), anim::setFireTickRate);
            case 21 -> adjustTickRateSlot(event, anim.getAdsTickRate(), anim::setAdsTickRate);
            case 22 -> adjustFrameListSlot(event, cursorModel, anim.getAdsFireFrames(), 40, anim::setAdsFireFrames);
            case 23 -> adjustFrameListSlot(event, cursorModel, anim.getAdsOutFrames(), 45, anim::setAdsOutFrames);
            case 24 -> adjustFrameListSlot(event, cursorModel, anim.getSlash2Frames(), 50, anim::setSlash2Frames);
            case 25 -> adjustFrameListSlot(event, cursorModel, anim.getHeavyFrames(), 60, anim::setHeavyFrames);
            case 28 -> adjustFrameListSlot(event, cursorModel, anim.getFireFrames(), 1, anim::setFireFrames);
            case 30 -> adjustFrameListSlot(event, cursorModel, anim.getInspectFrames(), 10, anim::setInspectFrames);
            case 31 -> adjustTickRateSlot(event, anim.getInspectTickRate(), anim::setInspectTickRate);
            case 32 -> adjustFrameListSlot(event, cursorModel, anim.getReloadTacticalFrames(), 70, anim::setReloadTacticalFrames);
            case 33 -> adjustTickRateSlot(event, anim.getReloadTickRate(), anim::setReloadTickRate);
            case 34 -> adjustFrameListSlot(event, cursorModel, anim.getReloadFrames(), 20, anim::setReloadFrames);
            case 49 -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getGuiManager().openWeaponEditor(player, weapon);
            }
            default -> {
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
