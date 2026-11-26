package com.cookie.csg.gui;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.entity.Player;

public class GuiManager {

    private final CookieCSGPlugin plugin;

    public GuiManager(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        openMainMenu(player, 0);
    }

    public void openMainMenu(Player player, int page) {
        MainMenu menu = new MainMenu(plugin, player, page);
        player.openInventory(menu.getInventory());
    }

    public void openWeaponEditor(Player player, WeaponDefinition weapon) {
        WeaponEditorMenu menu = new WeaponEditorMenu(plugin, player, weapon);
        player.openInventory(menu.getInventory());
    }

    public void openAnimationFramesMenu(Player player, WeaponDefinition weapon) {
        AnimationFramesMenu menu = new AnimationFramesMenu(plugin, player, weapon);
        player.openInventory(menu.getInventory());
    }

    public void openFramesEditor(Player player, WeaponDefinition weapon) {
        openAnimationFramesMenu(player, weapon);
    }

    public void openStatsEditor(Player player, WeaponDefinition weapon) {
        StatsEditorMenu menu = new StatsEditorMenu(plugin, player, weapon);
        player.openInventory(menu.getInventory());
    }

    public void openAdsEditor(Player player, WeaponDefinition weapon) {
        AdsEditorMenu menu = new AdsEditorMenu(plugin, player, weapon);
        player.openInventory(menu.getInventory());
    }
}
