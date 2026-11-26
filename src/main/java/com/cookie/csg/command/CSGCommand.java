package com.cookie.csg.command;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CSGCommand implements CommandExecutor, TabCompleter {

    private final CookieCSGPlugin plugin;

    public CSGCommand(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getGuiManager().openMainMenu(player);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui", "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open GUI Editor.");
                    return true;
                }
                plugin.getGuiManager().openMainMenu(player);
                return true;
            }
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can edit weapons.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /csg edit <weapon_id>");
                    return true;
                }
                WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(args[1]);
                if (weapon == null) {
                    player.sendMessage(ChatColor.RED + "Weapon not found: " + args[1]);
                    return true;
                }
                plugin.getGuiManager().openWeaponEditor(player, weapon);
                return true;
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /csg give <player> <weapon_id>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player offline: " + args[1]);
                    return true;
                }
                WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(args[2]);
                if (weapon == null) {
                    sender.sendMessage(ChatColor.RED + "Weapon not found: " + args[2]);
                    return true;
                }
                ItemStack item = plugin.getWeaponManager().buildItemStack(weapon);
                target.getInventory().addItem(item);
                if (plugin.getAnimationEngine() != null) {
                    plugin.getAnimationEngine().syncHeldWeaponState(target);
                }
                String coloredName = ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName());
                target.sendMessage(ChatColor.GREEN + "Received weapon: " + coloredName);
                sender.sendMessage(ChatColor.GREEN + "Gave " + coloredName + ChatColor.GREEN + " to " + target.getName());
                return true;
            }
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "=== Registered CSG Weapons (" + plugin.getWeaponManager().getWeapons().size() + ") ===");
                for (WeaponDefinition w : plugin.getWeaponManager().getWeapons().values()) {
                    sender.sendMessage(ChatColor.YELLOW + "- " + w.getId() + " (" + w.getType().name() + "): " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', w.getDisplayName()));
                }
                return true;
            }
            case "reload" -> {
                plugin.reloadPluginConfig();
                plugin.getWeaponManager().loadWeapons();
                sender.sendMessage(ChatColor.GREEN + "CookieCSG configurations and weapons reloaded.");
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== CookieCSG Weapon System ===");
        sender.sendMessage(ChatColor.YELLOW + "/csg editor" + ChatColor.GRAY + " - Open In-Game GUI Editor");
        sender.sendMessage(ChatColor.YELLOW + "/csg edit <id>" + ChatColor.GRAY + " - Edit weapon directly in GUI");
        sender.sendMessage(ChatColor.YELLOW + "/csg give <player> <id>" + ChatColor.GRAY + " - Give weapon item");
        sender.sendMessage(ChatColor.YELLOW + "/csg list" + ChatColor.GRAY + " - List all weapons");
        sender.sendMessage(ChatColor.YELLOW + "/csg reload" + ChatColor.GRAY + " - Hot-reload weapons from YAML");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("editor", "gui", "edit", "give", "list", "reload");
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.startsWith(args[0].toLowerCase())) {
                    filtered.add(s);
                }
            }
            return filtered;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("edit")) {
                return filterWeaponCompletions(args[1]);
            }
            if (args[0].equalsIgnoreCase("give")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(p.getName());
                    }
                }
                return names;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filterWeaponCompletions(args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filterWeaponCompletions(String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        List<String> candidates = new ArrayList<>(plugin.getWeaponManager().getWeapons().keySet());
        addAliasCompletion(candidates, "csgo_m249", "m249");
        addAliasCompletion(candidates, "csgo_m4a1", "m4a1");
        addAliasCompletion(candidates, "csgo_dark_star", "dark_star");
        addAliasCompletion(candidates, "kar98", "kar98k");
        addAliasCompletion(candidates, "karambit_obsidian", "karambit");
        addAliasCompletion(candidates, "cobra_vanguard", "cobra");
        List<String> filtered = new ArrayList<>();
        for (String s : candidates) {
            if (s.toLowerCase().startsWith(lowerPrefix)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    private void addAliasCompletion(List<String> candidates, String canonicalId, String alias) {
        if (plugin.getWeaponManager().getWeapons().containsKey(canonicalId) && !candidates.contains(alias)) {
            candidates.add(alias);
        }
    }
}
