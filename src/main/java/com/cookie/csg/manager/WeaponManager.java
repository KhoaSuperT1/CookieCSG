package com.cookie.csg.manager;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponAds;
import com.cookie.csg.model.WeaponAnimation;
import com.cookie.csg.model.WeaponAudio;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponMode;
import com.cookie.csg.model.WeaponRecoil;
import com.cookie.csg.model.WeaponStats;
import com.cookie.csg.model.WeaponType;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponManager {

    private static final NamespacedKey NO_COOLDOWN_KEY = new NamespacedKey("cookiecsg", "no_attack_cooldown");
    private static final NamespacedKey NO_BLOCK_BREAK_KEY = new NamespacedKey("cookiecsg", "no_block_break");
    private static final Map<String, String> ID_ALIASES = Map.ofEntries(
        Map.entry("m4a1", "csgo_m4a1"),
        Map.entry("m4a1_carbine", "csgo_m4a1"),
        Map.entry("dark_star", "csgo_dark_star"),
        Map.entry("m249", "csgo_m249"),
        Map.entry("kar98k", "kar98"),
        Map.entry("karambit", "karambit_obsidian"),
        Map.entry("cobra", "cobra_vanguard"),
        Map.entry("usp", "usp_silencer"),
        Map.entry("usp_s", "usp_silencer"),
        Map.entry("elite", "dual_berettas"),
        Map.entry("dualies", "dual_berettas"),
        Map.entry("berettas", "dual_berettas"),
        Map.entry("mp5", "hk_mp5a5"),
        Map.entry("mp7", "hk_mp7a1"),
        Map.entry("m1014", "xm1014"),
        Map.entry("tec9", "tec_9"),
        Map.entry("m9", "m9amethyst"),
        Map.entry("dragons_fang", "quenching"),
        Map.entry("free_dark_star", "null_annihil"),
        Map.entry("ursus", "db_long"),
        Map.entry("evolys", "fn_evolys"),
        Map.entry("scarl", "scar_l"),
        Map.entry("mk20", "scar_mk20"),
        Map.entry("m1911", "p1911"),
        Map.entry("colt1911", "p1911")
    );

    private final Plugin plugin;
    private final File weaponsFolder;
    private final NamespacedKey weaponIdKey;
    private final NamespacedKey itemsAdderIdKey;
    private final Map<String, WeaponDefinition> weapons = new ConcurrentHashMap<>();
    private final Map<Material, Map<Integer, String>> cmdLookup = new ConcurrentHashMap<>();
    private final Map<String, NamespacedKey> modelKeyCache = new ConcurrentHashMap<>();

    public WeaponManager(CookieCSGPlugin plugin) {
        this((Plugin) plugin);
    }

    public WeaponManager(Plugin plugin) {
        this.plugin = plugin;
        this.weaponsFolder = new File(plugin != null ? plugin.getDataFolder() : new File("plugins/CookieCSG"), "weapons");
        this.weaponIdKey = new NamespacedKey("cookiecsg", "weapon_id");
        this.itemsAdderIdKey = new NamespacedKey("itemsadder", "id");
    }

    public WeaponManager(File weaponsFolder) {
        this.plugin = null;
        this.weaponsFolder = weaponsFolder;
        this.weaponIdKey = new NamespacedKey("cookiecsg", "weapon_id");
        this.itemsAdderIdKey = new NamespacedKey("itemsadder", "id");
    }

    public void loadWeapons() {
        if (!weaponsFolder.exists()) {
            weaponsFolder.mkdirs();
        }
        File[] files = weaponsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            createDefaultWeapons();
            return;
        }
        weapons.clear();
        cmdLookup.clear();
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                if (!yaml.contains("id") || yaml.getString("id", "").isBlank()) {
                    continue;
                }
                WeaponDefinition def = loadFromYaml(yaml, file.getName().replace(".yml", ""));
                registerWeapon(def);
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to load weapon from " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        if (plugin != null) {
            plugin.getLogger().info("Loaded " + weapons.size() + " weapons.");
        }
    }

    private WeaponDefinition loadFromYaml(YamlConfiguration yaml, String defaultId) {
        String id = yaml.getString("id", defaultId);
        String displayName = yaml.getString("display-name", id);
        WeaponMode fallbackMode = yaml.isConfigurationSection("animation") ? WeaponMode.MODEL : WeaponMode.DEFAULT;
        WeaponMode mode = yaml.contains("mode")
            ? WeaponMode.fromString(yaml.getString("mode"), fallbackMode)
            : fallbackMode;
        WeaponType type;
        try {
            type = WeaponType.valueOf(yaml.getString("type", "RIFLE").toUpperCase());
        } catch (IllegalArgumentException e) {
            type = WeaponType.RIFLE;
        }
        Material material = Material.matchMaterial(yaml.getString("material", type.isMelee() ? "IRON_SWORD" : "IRON_HORSE_ARMOR"));
        if (material == null) {
            material = type.isMelee() ? Material.IRON_SWORD : Material.IRON_HORSE_ARMOR;
        }

        WeaponAnimation animation;
        if (mode == WeaponMode.MODEL) {
            animation = new WeaponAnimation(
                yaml.getInt("animation.idle-model", 0),
                yaml.getInt("animation.deploy-model", 0),
                yaml.getInt("animation.deploy-ticks", 5),
                yaml.getInt("animation.ads-model", 0),
                yaml.getInt("animation.ads-fire-model", 0),
                yaml.getIntegerList("animation.fire-frames"),
                yaml.getIntegerList("animation.inspect-frames"),
                yaml.getIntegerList("animation.reload-frames"),
                yaml.getInt("animation.inspect-tick-rate", 1)
            );
            animation.setDeployFrames(yaml.getIntegerList("animation.deploy-frames"));
            animation.setDeployTickRate(yaml.getInt("animation.deploy-tick-rate", 1));
            animation.setAdsInFrames(yaml.getIntegerList("animation.ads-in-frames"));
            animation.setAdsFireFrames(yaml.getIntegerList("animation.ads-fire-frames"));
            animation.setAdsOutFrames(yaml.getIntegerList("animation.ads-out-frames"));
            animation.setSlash2Frames(yaml.getIntegerList("animation.slash2-frames"));
            animation.setHeavyFrames(yaml.getIntegerList("animation.heavy-frames"));
            animation.setFireTickRate(yaml.getInt("animation.fire-tick-rate", 1));
            animation.setAdsTickRate(yaml.getInt("animation.ads-tick-rate", 1));
            animation.setReloadTickRate(yaml.getInt("animation.reload-tick-rate", 1));
            animation.setReloadTacticalFrames(yaml.getIntegerList("animation.reload-tactical-frames"));
            animation.setReloadIntroFrames(yaml.getIntegerList("animation.reload-intro-frames"));
            animation.setReloadIntroTicks(yaml.getInt("animation.reload-intro-ticks", 0));
            animation.setReloadLoopFrames(yaml.getIntegerList("animation.reload-loop-frames"));
            animation.setReloadLoopTicks(yaml.getInt("animation.reload-loop-ticks", 0));
            animation.setReloadEndFrames(yaml.getIntegerList("animation.reload-end-frames"));
            animation.setReloadEndTicks(yaml.getInt("animation.reload-end-ticks", 0));
            animation.setItemModelNamespace(yaml.getString("animation.item-model-namespace", "cookie_csg"));
            ConfigurationSection frameModelsSection = yaml.getConfigurationSection("animation.frame-models");
            if (frameModelsSection != null) {
                for (String key : frameModelsSection.getKeys(false)) {
                    try {
                        int cmd = Integer.parseInt(key);
                        String modelKey = frameModelsSection.getString(key, "");
                        if (!modelKey.isBlank()) {
                            animation.putFrameModel(cmd, modelKey);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } else {
            animation = new WeaponAnimation();
        }

        double defaultDamage = type.getDefaultDamage();
        double damage = yaml.getDouble("stats.damage", defaultDamage);
        double headshotMultiplier = yaml.getDouble("stats.headshot-multiplier", 2.0);
        int fireRateTicks = yaml.getInt("stats.fire-rate-ticks", 2);
        int clipSize = yaml.getInt("stats.clip-size", 30);
        int maxAmmo = yaml.getInt("stats.max-ammo", 90);
        int reloadTicks = yaml.getInt("stats.reload-ticks", 40);
        double range = yaml.getDouble("stats.range", 64.0);
        double movementSpeedMultiplier = yaml.getDouble("stats.movement-speed-multiplier", 1.0);
        double adsMovementMultiplier = yaml.contains("stats.ads-movement-speed-multiplier")
            ? yaml.getDouble("stats.ads-movement-speed-multiplier")
            : yaml.getDouble("stats.ads-movement-multiplier", 0.75);
        double backstabMultiplier = yaml.getDouble("stats.backstab-multiplier", type == WeaponType.KNIFE ? 3.0 : 1.0);
        double shieldBreakChance = yaml.getDouble("stats.shield-break-chance", type == WeaponType.AXE ? 1.0 : 0.0);

        WeaponStats stats = new WeaponStats(
            damage, headshotMultiplier, fireRateTicks, clipSize, maxAmmo, reloadTicks,
            range, movementSpeedMultiplier, adsMovementMultiplier, backstabMultiplier, shieldBreakChance
        );
        stats.setReloadTacticalTicks(yaml.getInt("stats.reload-tactical-ticks", 0));
        List<FireMode> parsedFireModes = new ArrayList<>();
        Set<FireMode> seenModes = new LinkedHashSet<>();
        for (String rawMode : yaml.getStringList("stats.fire-modes")) {
            FireMode parsed = FireMode.fromString(rawMode);
            if (parsed != null && seenModes.add(parsed)) {
                parsedFireModes.add(parsed);
            }
        }
        stats.setFireModes(parsedFireModes);
        FireMode parsedDefaultMode = FireMode.fromString(yaml.getString("stats.default-fire-mode", ""));
        if (parsedDefaultMode != null) {
            stats.setDefaultFireMode(parsedDefaultMode);
        }
        stats.setBurstCount(yaml.getInt("stats.burst-count", 3));

        WeaponAds ads = new WeaponAds(
            yaml.getBoolean("ads.enabled", true),
            yaml.getInt("ads.zoom-level", 1),
            yaml.getDouble("ads.spread-multiplier", 0.15),
            yaml.getDouble("ads.recoil-multiplier", 0.60),
            yaml.getBoolean("ads.scope-overlay", false)
        );

        WeaponRecoil recoil = new WeaponRecoil(
            yaml.getDouble("recoil.vertical", 1.0),
            yaml.getDouble("recoil.horizontal", 0.3),
            yaml.getDouble("recoil.spread", 0.035),
            yaml.getDouble("recoil.sprint-spread-multiplier", 2.0)
        );

        WeaponAudio audio = new WeaponAudio(
            yaml.getString("audio.shoot", ""),
            yaml.getString("audio.ads-in", ""),
            yaml.getString("audio.ads-out", ""),
            yaml.getString("audio.deploy", ""),
            yaml.getString("audio.reload-clip-out", ""),
            yaml.getString("audio.reload-clip-in", ""),
            yaml.getString("audio.reload-bolt", ""),
            yaml.getString("audio.inspect", ""),
            yaml.getString("audio.empty", ""),
            yaml.getString("audio.backstab", ""),
            yaml.getString("audio.shield-break", "")
        );
        audio.setReloadClipInTick(yaml.getInt("audio.reload-clip-in-tick", -1));
        audio.setReloadBoltTick(yaml.getInt("audio.reload-bolt-tick", -1));

        WeaponDefinition def = new WeaponDefinition(id, displayName, type, material, animation, stats, ads, recoil, audio);
        def.setMode(mode);
        return def;
    }

    public void saveWeapon(WeaponDefinition weapon) {
        if (weapon == null || weapon.getId() == null) {
            return;
        }
        if (!weaponsFolder.exists()) {
            weaponsFolder.mkdirs();
        }
        File file = new File(weaponsFolder, weapon.getId().toLowerCase() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("id", weapon.getId());
        yaml.set("display-name", weapon.getDisplayName());
        yaml.set("mode", weapon.getMode().getConfigValue());
        yaml.set("type", weapon.getType().name());
        yaml.set("material", weapon.getMaterial().name());

        if (weapon.isModelMode()) {
            yaml.set("animation.idle-model", weapon.getAnimation().getIdleModelData());
            yaml.set("animation.deploy-model", weapon.getAnimation().getDeployModelData());
            yaml.set("animation.deploy-ticks", weapon.getAnimation().getDeployTicks());
            if (!weapon.getAnimation().getDeployFrames().isEmpty()) {
                yaml.set("animation.deploy-frames", weapon.getAnimation().getDeployFrames());
                yaml.set("animation.deploy-tick-rate", weapon.getAnimation().getDeployTickRate());
            }
            yaml.set("animation.ads-model", weapon.getAnimation().getAdsModelData());
            yaml.set("animation.ads-fire-model", weapon.getAnimation().getAdsFireModelData());
            if (!weapon.getAnimation().getAdsInFrames().isEmpty()) {
                yaml.set("animation.ads-in-frames", weapon.getAnimation().getAdsInFrames());
            }
            if (!weapon.getAnimation().getAdsFireFrames().isEmpty()) {
                yaml.set("animation.ads-fire-frames", weapon.getAnimation().getAdsFireFrames());
            }
            if (!weapon.getAnimation().getAdsOutFrames().isEmpty()) {
                yaml.set("animation.ads-out-frames", weapon.getAnimation().getAdsOutFrames());
            }
            yaml.set("animation.ads-tick-rate", weapon.getAnimation().getAdsTickRate());
            yaml.set("animation.fire-tick-rate", weapon.getAnimation().getFireTickRate());
            yaml.set("animation.inspect-tick-rate", weapon.getAnimation().getInspectTickRate());
            yaml.set("animation.reload-tick-rate", weapon.getAnimation().getReloadTickRate());
            yaml.set("animation.fire-frames", weapon.getAnimation().getFireFrames());
            if (!weapon.getAnimation().getSlash2Frames().isEmpty()) {
                yaml.set("animation.slash2-frames", weapon.getAnimation().getSlash2Frames());
            }
            if (!weapon.getAnimation().getHeavyFrames().isEmpty()) {
                yaml.set("animation.heavy-frames", weapon.getAnimation().getHeavyFrames());
            }
            yaml.set("animation.inspect-frames", weapon.getAnimation().getInspectFrames());
            yaml.set("animation.reload-frames", weapon.getAnimation().getReloadFrames());
            if (!weapon.getAnimation().getReloadTacticalFrames().isEmpty()) {
                yaml.set("animation.reload-tactical-frames", weapon.getAnimation().getReloadTacticalFrames());
            }
            if (!weapon.getAnimation().getReloadIntroFrames().isEmpty()) {
                yaml.set("animation.reload-intro-frames", weapon.getAnimation().getReloadIntroFrames());
            }
            if (weapon.getAnimation().getReloadIntroTicks() > 0) {
                yaml.set("animation.reload-intro-ticks", weapon.getAnimation().getReloadIntroTicks());
            }
            if (!weapon.getAnimation().getReloadLoopFrames().isEmpty()) {
                yaml.set("animation.reload-loop-frames", weapon.getAnimation().getReloadLoopFrames());
            }
            if (weapon.getAnimation().getReloadLoopTicks() > 0) {
                yaml.set("animation.reload-loop-ticks", weapon.getAnimation().getReloadLoopTicks());
            }
            if (!weapon.getAnimation().getReloadEndFrames().isEmpty()) {
                yaml.set("animation.reload-end-frames", weapon.getAnimation().getReloadEndFrames());
            }
            if (weapon.getAnimation().getReloadEndTicks() > 0) {
                yaml.set("animation.reload-end-ticks", weapon.getAnimation().getReloadEndTicks());
            }
            if (weapon.getAnimation().getItemModelNamespace() != null && !"cookie_csg".equalsIgnoreCase(weapon.getAnimation().getItemModelNamespace())) {
                yaml.set("animation.item-model-namespace", weapon.getAnimation().getItemModelNamespace());
            }
            if (!weapon.getAnimation().getFrameModels().isEmpty()) {
                for (Map.Entry<Integer, String> entry : weapon.getAnimation().getFrameModels().entrySet()) {
                    yaml.set("animation.frame-models." + entry.getKey(), entry.getValue());
                }
            }
        }

        yaml.set("stats.damage", weapon.getStats().getDamage());
        yaml.set("stats.headshot-multiplier", weapon.getStats().getHeadshotMultiplier());
        yaml.set("stats.fire-rate-ticks", weapon.getStats().getFireRateTicks());
        yaml.set("stats.clip-size", weapon.getStats().getClipSize());
        yaml.set("stats.max-ammo", weapon.getStats().getMaxAmmo());
        yaml.set("stats.reload-ticks", weapon.getStats().getReloadTicks());
        if (weapon.getStats().getReloadTacticalTicks() > 0) {
            yaml.set("stats.reload-tactical-ticks", weapon.getStats().getReloadTacticalTicks());
        }
        yaml.set("stats.range", weapon.getStats().getRange());
        yaml.set("stats.movement-speed-multiplier", weapon.getStats().getMovementSpeedMultiplier());
        yaml.set("stats.ads-movement-speed-multiplier", weapon.getStats().getAdsMovementMultiplier());
        yaml.set("stats.backstab-multiplier", weapon.getStats().getBackstabMultiplier());
        yaml.set("stats.shield-break-chance", weapon.getStats().getShieldBreakChance());
        if (!weapon.getStats().getFireModes().isEmpty()) {
            List<String> modeNames = new ArrayList<>();
            for (FireMode mode : weapon.getStats().getFireModes()) {
                modeNames.add(mode.name());
            }
            yaml.set("stats.fire-modes", modeNames);
        }
        if (weapon.getStats().getDefaultFireMode() != null) {
            yaml.set("stats.default-fire-mode", weapon.getStats().getDefaultFireMode().name());
        }
        if (weapon.getStats().getBurstCount() != 3) {
            yaml.set("stats.burst-count", weapon.getStats().getBurstCount());
        }

        yaml.set("ads.enabled", weapon.getAds().isEnabled());
        yaml.set("ads.zoom-level", weapon.getAds().getZoomLevel());
        yaml.set("ads.spread-multiplier", weapon.getAds().getSpreadMultiplier());
        yaml.set("ads.recoil-multiplier", weapon.getAds().getRecoilMultiplier());
        yaml.set("ads.scope-overlay", weapon.getAds().isScopeOverlay());

        yaml.set("recoil.vertical", weapon.getRecoil().getVertical());
        yaml.set("recoil.horizontal", weapon.getRecoil().getHorizontal());
        yaml.set("recoil.spread", weapon.getRecoil().getSpread());
        yaml.set("recoil.sprint-spread-multiplier", weapon.getRecoil().getSprintSpreadMultiplier());

        yaml.set("audio.shoot", weapon.getAudio().getShoot());
        yaml.set("audio.ads-in", weapon.getAudio().getAdsIn());
        yaml.set("audio.ads-out", weapon.getAudio().getAdsOut());
        yaml.set("audio.deploy", weapon.getAudio().getDeploy());
        yaml.set("audio.reload-clip-out", weapon.getAudio().getReloadClipOut());
        yaml.set("audio.reload-clip-in", weapon.getAudio().getReloadClipIn());
        yaml.set("audio.reload-bolt", weapon.getAudio().getReloadBolt());
        yaml.set("audio.inspect", weapon.getAudio().getInspect());
        yaml.set("audio.empty", weapon.getAudio().getEmpty());
        yaml.set("audio.backstab", weapon.getAudio().getBackstab());
        yaml.set("audio.shield-break", weapon.getAudio().getShieldBreak());
        if (weapon.getAudio().getReloadClipInTick() >= 0) {
            yaml.set("audio.reload-clip-in-tick", weapon.getAudio().getReloadClipInTick());
        }
        if (weapon.getAudio().getReloadBoltTick() >= 0) {
            yaml.set("audio.reload-bolt-tick", weapon.getAudio().getReloadBoltTick());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save weapon YAML to " + file.getName() + ": " + e.getMessage());
            }
        }
        registerWeapon(weapon);
    }

    private void registerWeapon(WeaponDefinition def) {
        if (def == null || def.getId() == null) {
            return;
        }
        String canonicalId = def.getId().toLowerCase();
        weapons.put(canonicalId, def);
        if (def.isDefaultMode()) {
            return;
        }
        Material mat = def.getMaterial() != null ? def.getMaterial() : Material.IRON_HOE;
        Map<Integer, String> matMap = cmdLookup.computeIfAbsent(mat, k -> new ConcurrentHashMap<>());
        WeaponAnimation anim = def.getAnimation();
        if (anim != null) {
            registerCmd(matMap, anim.getIdleModelData(), canonicalId);
            registerCmd(matMap, anim.getDeployModelData(), canonicalId);
            registerCmd(matMap, anim.getAdsModelData(), canonicalId);
            registerCmd(matMap, anim.getAdsFireModelData(), canonicalId);
            registerCmdList(matMap, anim.getDeployFrames(), canonicalId);
            registerCmdList(matMap, anim.getAdsInFrames(), canonicalId);
            registerCmdList(matMap, anim.getAdsFireFrames(), canonicalId);
            registerCmdList(matMap, anim.getAdsOutFrames(), canonicalId);
            registerCmdList(matMap, anim.getFireFrames(), canonicalId);
            registerCmdList(matMap, anim.getSlash2Frames(), canonicalId);
            registerCmdList(matMap, anim.getHeavyFrames(), canonicalId);
            registerCmdList(matMap, anim.getInspectFrames(), canonicalId);
            registerCmdList(matMap, anim.getReloadFrames(), canonicalId);
            registerCmdList(matMap, anim.getReloadTacticalFrames(), canonicalId);
            registerCmdList(matMap, anim.getReloadIntroFrames(), canonicalId);
            registerCmdList(matMap, anim.getReloadLoopFrames(), canonicalId);
            registerCmdList(matMap, anim.getReloadEndFrames(), canonicalId);
        }
        if ("csgo_m249".equals(canonicalId)) {
            registerCmd(matMap, 200401, canonicalId);
        }
    }

    private void registerCmdList(Map<Integer, String> matMap, List<Integer> cmds, String weaponId) {
        if (cmds != null) {
            for (int f : cmds) {
                registerCmd(matMap, f, weaponId);
            }
        }
    }

    private void registerCmd(Map<Integer, String> matMap, int cmd, String weaponId) {
        if (cmd > 0) {
            matMap.put(cmd, weaponId);
        }
    }

    public void createDefaultWeapons() {
        List<String> bundledIds = List.of("p1911", "bayonet");
        weapons.clear();
        cmdLookup.clear();
        for (String wid : bundledIds) {
            WeaponDefinition def = loadBundledWeapon(wid);
            if (def != null) {
                saveWeapon(def);
            }
        }
        if (plugin != null) {
            plugin.getLogger().info("Loaded " + weapons.size() + " bundled default weapons.");
        }
    }

    private WeaponDefinition loadBundledWeapon(String wid) {
        String resourcePath = "weapons/" + wid + ".yml";
        try (InputStream in = openResourceStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
                if (!yaml.contains("id") || yaml.getString("id", "").isBlank()) {
                    yaml.set("id", wid);
                }
                return loadFromYaml(yaml, wid);
            }
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to load bundled weapon " + wid + ": " + e.getMessage());
            }
            return null;
        }
    }

    private InputStream openResourceStream(String resourcePath) {
        if (plugin != null) {
            InputStream stream = plugin.getResource(resourcePath);
            if (stream != null) {
                return stream;
            }
        }
        ClassLoader cl = WeaponManager.class.getClassLoader();
        return cl != null ? cl.getResourceAsStream(resourcePath) : null;
    }

    public int extractBundledDefaultModels(File pluginsFolder) {
        int extracted = 0;
        List<String> manifestEntries = new ArrayList<>();
        try (InputStream manifestIn = openResourceStream("default_pack/manifest.txt")) {
            if (manifestIn == null) {
                return 0;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(manifestIn, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        manifestEntries.add(trimmed);
                    }
                }
            }
        } catch (IOException e) {
            return 0;
        }
        if (manifestEntries.isEmpty()) {
            return 0;
        }
        List<File> targets = new ArrayList<>();
        File pluginDataDir = plugin != null ? plugin.getDataFolder() : weaponsFolder.getParentFile();
        if (pluginDataDir != null) {
            File localDefaultPack = new File(pluginDataDir, "default-pack");
            File localMarker = new File(localDefaultPack, "ItemsAdder/contents/cookie_csg/configs/weapons.yml");
            if (!localMarker.exists()) {
                targets.add(localDefaultPack);
            }
        }
        if (pluginsFolder != null) {
            File iaDir = new File(pluginsFolder, "ItemsAdder");
            File iaMarker = new File(iaDir, "contents/cookie_csg/configs/weapons.yml");
            if (iaDir.isDirectory() && !iaMarker.exists()) {
                targets.add(pluginsFolder);
            }
        }
        for (File targetRoot : targets) {
            for (String relPath : manifestEntries) {
                try (InputStream assetIn = openResourceStream("default_pack/" + relPath)) {
                    if (assetIn == null) {
                        continue;
                    }
                    File outFile = new File(targetRoot, relPath);
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    Files.copy(assetIn, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    extracted++;
                } catch (IOException ignored) {
                }
            }
        }
        return extracted;
    }

    public ItemStack buildItemStack(WeaponDefinition weapon) {
        if (weapon == null) {
            return null;
        }
        Material mat = weapon.getMaterial() != null
            ? weapon.getMaterial()
            : (weapon.getType().isMelee() ? Material.IRON_SWORD : Material.IRON_HORSE_ARMOR);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName()));
            if (!weapon.isDefaultMode() && weapon.getAnimation() != null) {
                int idleCmd = weapon.getAnimation().getIdleModelData();
                if (idleCmd > 0) {
                    meta.setCustomModelData(idleCmd);
                }
                NamespacedKey resolvedModel = resolveItemModel(weapon, idleCmd);
                if (resolvedModel != null) {
                    meta.setItemModel(resolvedModel);
                }
            }
            if (mat.getMaxDurability() > 0) {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            ensureNoAttackCooldown(meta);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "------------------------");
            lore.add(ChatColor.GRAY + "Mode: " + (weapon.isDefaultMode() ? ChatColor.YELLOW + "default" : ChatColor.AQUA + "model"));
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + weapon.getType().name());
            lore.add(ChatColor.GRAY + "Damage: " + ChatColor.RED + weapon.getStats().getDamage());
            if (weapon.getType().isGun()) {
                lore.add(ChatColor.GRAY + "Ammo: " + ChatColor.AQUA + weapon.getStats().getClipSize() + " / " + weapon.getStats().getMaxAmmo());
            }
            if (weapon.getType() == WeaponType.KNIFE) {
                lore.add(ChatColor.GRAY + "Backstab: " + ChatColor.GOLD + "x" + weapon.getStats().getBackstabMultiplier());
            }
            if (weapon.getType() == WeaponType.AXE) {
                lore.add(ChatColor.GRAY + "Shield Break: " + ChatColor.GOLD + (int) (weapon.getStats().getShieldBreakChance() * 100) + "%");
            }
            lore.add(ChatColor.DARK_GRAY + "------------------------");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(this.weaponIdKey, PersistentDataType.STRING, weapon.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey resolveItemModel(WeaponDefinition weapon, int modelData) {
        if (weapon == null || weapon.isDefaultMode()) {
            return null;
        }
        WeaponAnimation anim = weapon.getAnimation();
        if (anim != null && anim.isItemModelDisabled()) {
            return null;
        }
        if (modelData <= 0 && (anim == null || !anim.hasCustomVisuals())) {
            return null;
        }
        String ns = (anim != null && anim.getItemModelNamespace() != null && !anim.getItemModelNamespace().isBlank())
            ? anim.getItemModelNamespace().trim().toLowerCase()
            : "cookie_csg";
        String key = anim != null
            ? anim.resolveModelKey(weapon.getId(), weapon.getType(), modelData)
            : weapon.getId().toLowerCase();
        String cacheKey = ns + "|" + key;
        NamespacedKey cached = modelKeyCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        NamespacedKey resolved;
        String lookup = key;
        if (lookup.contains(":")) {
            NamespacedKey parsed = NamespacedKey.fromString(lookup);
            if (parsed != null) {
                modelKeyCache.putIfAbsent(cacheKey, parsed);
                return parsed;
            }
            lookup = lookup.substring(lookup.indexOf(':') + 1);
        }
        if (lookup.startsWith("item/")) {
            lookup = lookup.substring("item/".length());
        }
        resolved = new NamespacedKey(ns, lookup.toLowerCase());
        modelKeyCache.putIfAbsent(cacheKey, resolved);
        return resolved;
    }

    public void applyWeaponVisual(ItemStack item, WeaponDefinition weapon, int modelData) {
        if (item == null || item.getType() == Material.AIR || weapon == null) {
            return;
        }
        PersistentDataContainerView pdcView = item.getPersistentDataContainer();
        String directId = pdcView != null && pdcView.has(this.weaponIdKey, PersistentDataType.STRING)
            ? pdcView.get(this.weaponIdKey, PersistentDataType.STRING)
            : null;
        String itemWeaponId = directId != null ? directId : getWeaponId(item);
        if (itemWeaponId == null || !itemWeaponId.equalsIgnoreCase(weapon.getId())) {
            return;
        }
        int effectiveCmd = weapon.isDefaultMode() ? 0 : modelData;
        NamespacedKey itemModelKey = weapon.isDefaultMode() ? null : resolveItemModel(weapon, effectiveCmd);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean pdcMissing = !pdc.has(this.weaponIdKey, PersistentDataType.STRING);
        boolean cmdMatches = effectiveCmd <= 0
            ? !meta.hasCustomModelData()
            : (meta.hasCustomModelData() && meta.getCustomModelData() == effectiveCmd);
        boolean modelMatches = itemModelKey == null
            ? !meta.hasItemModel()
            : (meta.hasItemModel() && itemModelKey.equals(meta.getItemModel()));
        boolean hasCooldownMod = hasNoAttackCooldown(meta);
        if (cmdMatches && modelMatches && !pdcMissing && hasCooldownMod) {
            return;
        }
        if (effectiveCmd > 0) {
            meta.setCustomModelData(effectiveCmd);
        } else if (meta.hasCustomModelData()) {
            meta.setCustomModelData(null);
        }
        meta.setItemModel(itemModelKey);
        if (pdcMissing) {
            pdc.set(this.weaponIdKey, PersistentDataType.STRING, weapon.getId());
        }
        ensureNoAttackCooldown(meta);
        item.setItemMeta(meta);
    }

    private boolean hasNoAttackCooldown(ItemMeta meta) {
        return hasAttributeKey(meta, Attribute.ATTACK_SPEED, NO_COOLDOWN_KEY)
            && hasAttributeKey(meta, Attribute.BLOCK_BREAK_SPEED, NO_BLOCK_BREAK_KEY);
    }

    private boolean hasAttributeKey(ItemMeta meta, Attribute attribute, NamespacedKey expectedKey) {
        Collection<AttributeModifier> mods = meta.getAttributeModifiers(attribute);
        if (mods == null || mods.isEmpty()) {
            return false;
        }
        for (AttributeModifier mod : mods) {
            if (expectedKey.equals(mod.getKey())) {
                return true;
            }
        }
        return false;
    }

    private void ensureNoAttackCooldown(ItemMeta meta) {
        if (!hasAttributeKey(meta, Attribute.ATTACK_SPEED, NO_COOLDOWN_KEY)) {
            meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                    NO_COOLDOWN_KEY,
                    1024.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
                )
            );
        }
        if (!hasAttributeKey(meta, Attribute.BLOCK_BREAK_SPEED, NO_BLOCK_BREAK_KEY)) {
            meta.addAttributeModifier(
                Attribute.BLOCK_BREAK_SPEED,
                new AttributeModifier(
                    NO_BLOCK_BREAK_KEY,
                    -1.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    EquipmentSlotGroup.MAINHAND
                )
            );
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    public static String resolveSoundKey(String rawSound) {
        if (rawSound == null || rawSound.isEmpty()) {
            return "";
        }
        if (rawSound.indexOf(':') >= 0) {
            return rawSound;
        }
        return "cookie_csg:" + rawSound;
    }

    public boolean isWeapon(ItemStack item) {
        return getWeaponId(item) != null;
    }

    public String getWeaponId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainerView pdcView = item.getPersistentDataContainer();
        if (pdcView != null) {
            if (pdcView.has(this.weaponIdKey, PersistentDataType.STRING)) {
                return pdcView.get(this.weaponIdKey, PersistentDataType.STRING);
            }
            if (pdcView.has(this.itemsAdderIdKey, PersistentDataType.STRING)) {
                String iaId = pdcView.get(this.itemsAdderIdKey, PersistentDataType.STRING);
                WeaponDefinition resolved = resolveByRawId(iaId);
                if (resolved != null) {
                    return resolved.getId();
                }
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(this.weaponIdKey, PersistentDataType.STRING)) {
            return pdc.get(this.weaponIdKey, PersistentDataType.STRING);
        }
        if (pdc.has(this.itemsAdderIdKey, PersistentDataType.STRING)) {
            String iaId = pdc.get(this.itemsAdderIdKey, PersistentDataType.STRING);
            WeaponDefinition resolved = resolveByRawId(iaId);
            if (resolved != null) {
                return resolved.getId();
            }
        }
        for (NamespacedKey key : pdc.getKeys()) {
            if ("itemsadder".equals(key.getNamespace())) {
                try {
                    String val = pdc.get(key, PersistentDataType.STRING);
                    WeaponDefinition resolved = resolveByRawId(val);
                    if (resolved != null) {
                        return resolved.getId();
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (meta.hasCustomModelData()) {
            Map<Integer, String> matMap = cmdLookup.get(item.getType());
            if (matMap != null) {
                String byCmd = matMap.get(meta.getCustomModelData());
                if (byCmd != null) {
                    WeaponDefinition def = weapons.get(byCmd);
                    return def != null ? def.getId() : byCmd;
                }
            }
        }
        if (meta.hasItemModel()) {
            NamespacedKey itemModel = meta.getItemModel();
            if (itemModel != null && "cookie_csg".equals(itemModel.getNamespace())) {
                String modelKey = itemModel.getKey();
                if (modelKey.startsWith("item/")) {
                    modelKey = modelKey.substring("item/".length());
                }
                WeaponDefinition direct = getWeapon(modelKey);
                if (direct != null) {
                    return direct.getId();
                }
                WeaponDefinition bestMatch = null;
                int bestLen = -1;
                for (WeaponDefinition def : weapons.values()) {
                    String wid = def.getId().toLowerCase();
                    if ((modelKey.equals(wid) || modelKey.startsWith(wid + "_")) && wid.length() > bestLen) {
                        bestMatch = def;
                        bestLen = wid.length();
                    }
                }
                if (bestMatch != null) {
                    return bestMatch.getId();
                }
            }
        }
        return null;
    }

    private WeaponDefinition resolveByRawId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            trimmed = trimmed.substring(colon + 1);
        }
        return getWeapon(trimmed);
    }

    public WeaponDefinition getWeapon(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase();
        WeaponDefinition weapon = weapons.get(normalized);
        if (weapon == null) {
            String aliasTarget = ID_ALIASES.get(normalized);
            if (aliasTarget != null) {
                return weapons.get(aliasTarget);
            }
        }
        return weapon;
    }

    public WeaponDefinition getWeapon(ItemStack item) {
        String id = getWeaponId(item);
        return id != null ? getWeapon(id) : null;
    }

    public Map<String, WeaponDefinition> getWeapons() {
        return weapons;
    }

    public WeaponDefinition createWeapon(String id, WeaponType type) {
        return createWeapon(id, type, WeaponMode.MODEL);
    }

    public WeaponDefinition createWeapon(String id, WeaponType type, WeaponMode mode) {
        if (id == null || type == null) {
            return null;
        }
        Material defaultMat = switch (type) {
            case RIFLE, PISTOL, SMG, SNIPER, SHOTGUN -> Material.IRON_HORSE_ARMOR;
            case KNIFE -> Material.IRON_SWORD;
            case AXE -> Material.IRON_AXE;
        };
        WeaponDefinition def = new WeaponDefinition();
        def.setId(id.toLowerCase());
        def.setDisplayName(ChatColor.WHITE + id);
        def.setMode(mode != null ? mode : WeaponMode.MODEL);
        def.setType(type);
        def.setMaterial(defaultMat);
        def.getStats().setDamage(type.getDefaultDamage());
        if (type == WeaponType.KNIFE) {
            def.getStats().setBackstabMultiplier(3.0);
        } else if (type == WeaponType.AXE) {
            def.getStats().setShieldBreakChance(1.0);
        }
        saveWeapon(def);
        return def;
    }

    public void deleteWeapon(String id) {
        if (id == null) {
            return;
        }
        weapons.remove(id.toLowerCase());
        File file = new File(weaponsFolder, id.toLowerCase() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public File getWeaponsFolder() {
        return weaponsFolder;
    }

    public NamespacedKey getWeaponIdKey() {
        return weaponIdKey;
    }
}
