package com.cookie.csg.manager;

import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponAnimation;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponMode;
import com.cookie.csg.model.WeaponType;
import org.bukkit.Material;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeaponManagerTest {

    @TempDir
    File tempFolder;

    @Test
    public void testCreateDefaultWeaponsAndLoad() {
        WeaponManager manager = new WeaponManager(tempFolder);
        manager.loadWeapons();

        assertEquals(2, manager.getWeapons().size());

        WeaponDefinition p1911 = manager.getWeapon("p1911");
        assertNotNull(p1911);
        assertEquals("p1911", p1911.getId());
        assertEquals(WeaponMode.MODEL, p1911.getMode());
        assertEquals(WeaponType.PISTOL, p1911.getType());
        assertEquals(Material.IRON_HOE, p1911.getMaterial());
        assertEquals(8.0, p1911.getStats().getDamage());
        assertEquals(7, p1911.getStats().getClipSize());
        assertEquals(240100, p1911.getAnimation().getIdleModelData());
        assertEquals(15, p1911.getAnimation().getDeployTicks());
        assertEquals(p1911, manager.getWeapon("m1911"));
        assertEquals(p1911, manager.getWeapon("colt1911"));

        WeaponDefinition bayonet = manager.getWeapon("bayonet");
        assertNotNull(bayonet);
        assertEquals("bayonet", bayonet.getId());
        assertEquals(WeaponMode.MODEL, bayonet.getMode());
        assertEquals(WeaponType.KNIFE, bayonet.getType());
        assertEquals(Material.IRON_SWORD, bayonet.getMaterial());
        assertEquals(7.0, bayonet.getStats().getDamage());
        assertEquals(3.0, bayonet.getStats().getBackstabMultiplier());
        assertEquals(240200, bayonet.getAnimation().getIdleModelData());

        WeaponManager reloadManager = new WeaponManager(tempFolder);
        reloadManager.loadWeapons();
        assertEquals(2, reloadManager.getWeapons().size());
        assertNotNull(reloadManager.getWeapon("p1911"));
        assertNotNull(reloadManager.getWeapon("bayonet"));
    }

    @Test
    public void testCreateAndSaveWeapon() {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition custom = manager.createWeapon("m4a4", WeaponType.RIFLE);
        assertNotNull(custom);
        assertEquals("m4a4", custom.getId());
        assertEquals(WeaponType.RIFLE, custom.getType());

        WeaponDefinition retrieved = manager.getWeapon("m4a4");
        assertNotNull(retrieved);
        assertEquals("m4a4", retrieved.getId());
    }

    @Test
    public void testNeutralizedYamlIsIgnored() throws IOException {
        Files.writeString(tempFolder.toPath().resolve("deprecated.yml"), "info:\n  namespace: cookie_csg\nitems: {}\n");
        WeaponManager manager = new WeaponManager(tempFolder);
        manager.createWeapon("ak47", WeaponType.RIFLE);
        manager.loadWeapons();

        assertEquals(1, manager.getWeapons().size());
        assertNotNull(manager.getWeapon("ak47"));
    }

    @Test
    public void testLoadAllPluginWeapons() {
        File weaponsFolder = new File("CookieCSG_DONE/Pack 1/CookieCSG/weapons");
        Assumptions.assumeTrue(weaponsFolder.exists(), "Pack 1 external assets not present");

        WeaponManager manager = new WeaponManager(weaponsFolder);
        manager.loadWeapons();

        assertEquals(18, manager.getWeapons().size());

        List<String> authoredIds = List.of(
            "ak47", "aug", "awp", "kar98", "m700", "deagle", "deagle_golden", "taurus500",
            "dual_berettas", "p90", "m870", "vector",
            "combat_knife", "tactical_axe", "cobra_vanguard"
        );
        for (String id : authoredIds) {
            WeaponDefinition def = manager.getWeapon(id);
            assertNotNull(def, "Missing authored weapon: " + id);
            assertEquals(id, def.getId());
        }

        assertNull(manager.getWeapon("karambit_obsidian"));
        assertNull(manager.getWeapon("usp_silencer"));
        assertNull(manager.getWeapon("spas12"));

        WeaponDefinition kar98 = manager.getWeapon("kar98");
        assertNotNull(kar98);
        assertEquals("kar98", kar98.getId());
        assertEquals(kar98, manager.getWeapon("kar98k"));
        assertEquals(WeaponType.SNIPER, kar98.getType());
        assertEquals(200500, kar98.getAnimation().getIdleModelData());
        assertEquals(18.0, kar98.getStats().getDamage());
        assertEquals(5, kar98.getStats().getClipSize());

        WeaponDefinition m870 = manager.getWeapon("m870");
        assertNotNull(m870);
        assertEquals(WeaponType.SHOTGUN, m870.getType());
        assertEquals(200600, m870.getAnimation().getIdleModelData());

        WeaponDefinition deagleGolden = manager.getWeapon("deagle_golden");
        assertNotNull(deagleGolden);
        assertEquals(WeaponType.PISTOL, deagleGolden.getType());
        assertEquals(200700, deagleGolden.getAnimation().getIdleModelData());

        WeaponDefinition taurus = manager.getWeapon("taurus500");
        assertNotNull(taurus);
        assertEquals(WeaponType.PISTOL, taurus.getType());
        assertEquals(200800, taurus.getAnimation().getIdleModelData());

        WeaponDefinition cobra = manager.getWeapon("cobra_vanguard");
        assertNotNull(cobra);
        assertEquals(cobra, manager.getWeapon("cobra"));
        assertEquals(WeaponType.KNIFE, cobra.getType());
        assertEquals(201000, cobra.getAnimation().getIdleModelData());

        WeaponDefinition darkStar = manager.getWeapon("csgo_dark_star");
        assertNotNull(darkStar);
        assertEquals("csgo_dark_star", darkStar.getId());
        assertEquals(darkStar, manager.getWeapon("dark_star"));
        assertEquals(WeaponType.KNIFE, darkStar.getType());
        assertEquals(Material.IRON_SWORD, darkStar.getMaterial());
        assertEquals(200200, darkStar.getAnimation().getIdleModelData());
        assertEquals(7.5, darkStar.getStats().getDamage());
        assertEquals(3.5, darkStar.getStats().getBackstabMultiplier());

        WeaponDefinition m4a1 = manager.getWeapon("csgo_m4a1");
        assertNotNull(m4a1);
        assertEquals("csgo_m4a1", m4a1.getId());
        assertEquals(m4a1, manager.getWeapon("m4a1"));
        assertEquals(m4a1, manager.getWeapon("m4a1_carbine"));
        assertEquals(WeaponType.RIFLE, m4a1.getType());
        assertEquals(200300, m4a1.getAnimation().getIdleModelData());
        assertEquals(7.2, m4a1.getStats().getDamage());
        assertEquals(25, m4a1.getStats().getClipSize());

        WeaponDefinition m249 = manager.getWeapon("csgo_m249");
        assertNotNull(m249);
        assertEquals("csgo_m249", m249.getId());
        assertEquals(m249, manager.getWeapon("m249"));
        assertEquals(WeaponType.RIFLE, m249.getType());
        assertEquals(200400, m249.getAnimation().getIdleModelData());
        assertEquals(6.5, m249.getStats().getDamage());
        assertEquals(100, m249.getStats().getClipSize());
        assertEquals(List.of(FireMode.AUTO, FireMode.SEMI_AUTO), m249.getStats().resolveSupportedFireModes(m249.getType()));
        assertEquals(FireMode.AUTO, m249.getStats().resolveDefaultFireMode(m249.getType()));

        WeaponDefinition aug = manager.getWeapon("aug");
        assertNotNull(aug);
        assertEquals(WeaponType.RIFLE, aug.getType());
        assertEquals(201100, aug.getAnimation().getIdleModelData());
        assertTrue(aug.getAds().isScopeOverlay());

        WeaponDefinition m700 = manager.getWeapon("m700");
        assertNotNull(m700);
        assertEquals(WeaponType.SNIPER, m700.getType());
        assertEquals(201200, m700.getAnimation().getIdleModelData());
        assertTrue(m700.getAds().isScopeOverlay());

        WeaponDefinition elite = manager.getWeapon("dual_berettas");
        assertNotNull(elite);
        assertEquals(elite, manager.getWeapon("elite"));
        assertEquals(elite, manager.getWeapon("dualies"));
        assertEquals(elite, manager.getWeapon("berettas"));
        assertEquals(WeaponType.PISTOL, elite.getType());
        assertEquals(201400, elite.getAnimation().getIdleModelData());

        Map<Material, Set<Integer>> usedByMaterial = new HashMap<>();
        for (Map.Entry<String, WeaponDefinition> entry : manager.getWeapons().entrySet()) {
            WeaponDefinition def = entry.getValue();
            Set<Integer> used = usedByMaterial.computeIfAbsent(def.getMaterial(), k -> new HashSet<>());
            Set<Integer> weaponUnique = new HashSet<>();
            var anim = def.getAnimation();
            assertUniqueModel(weaponUnique, anim.getIdleModelData(), entry.getKey());
            if (anim.getDeployFrames() != null && !anim.getDeployFrames().isEmpty()) {
                for (int frame : anim.getDeployFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            } else {
                assertUniqueModel(weaponUnique, anim.getDeployModelData(), entry.getKey());
            }
            if (anim.getAdsInFrames() != null && !anim.getAdsInFrames().isEmpty()) {
                for (int frame : anim.getAdsInFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (!def.getType().isMelee() || anim.getAdsInFrames() == null || anim.getAdsInFrames().isEmpty()) {
                assertUniqueModel(weaponUnique, anim.getAdsModelData(), entry.getKey());
            }
            if (anim.getAdsFireFrames() != null && !anim.getAdsFireFrames().isEmpty()) {
                for (int frame : anim.getAdsFireFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            } else {
                assertUniqueModel(weaponUnique, anim.getAdsFireModelData(), entry.getKey());
            }
            if (anim.getAdsOutFrames() != null) {
                for (int frame : anim.getAdsOutFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getFireFrames() != null) {
                for (int frame : anim.getFireFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getInspectFrames() != null) {
                for (int frame : anim.getInspectFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getReloadFrames() != null) {
                for (int frame : anim.getReloadFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getReloadTacticalFrames() != null) {
                for (int frame : anim.getReloadTacticalFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getReloadIntroFrames() != null) {
                for (int frame : anim.getReloadIntroFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getReloadLoopFrames() != null) {
                for (int frame : anim.getReloadLoopFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            if (anim.getReloadEndFrames() != null) {
                for (int frame : anim.getReloadEndFrames()) {
                    assertUniqueModel(weaponUnique, frame, entry.getKey());
                }
            }
            for (int cmd : weaponUnique) {
                assertUniqueModel(used, cmd, entry.getKey());
            }
        }
    }

    @Test
    public void testPerWeaponTimingAndShotgunYamlRoundTrip() {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition shotgun = manager.createWeapon("spas12_sim", WeaponType.SHOTGUN);
        shotgun.getStats().setReloadTicks(80);
        shotgun.getStats().setReloadTacticalTicks(42);
        shotgun.getAnimation().setAdsTickRate(2);
        shotgun.getAnimation().setFireTickRate(2);
        shotgun.getAnimation().setInspectTickRate(1);
        shotgun.getAnimation().setReloadTickRate(2);
        shotgun.getAnimation().setReloadTacticalFrames(List.of(9101, 9102, 9103));
        shotgun.getAnimation().setReloadIntroFrames(List.of(9201, 9202));
        shotgun.getAnimation().setReloadIntroTicks(10);
        shotgun.getAnimation().setReloadLoopFrames(List.of(9301, 9302, 9303));
        shotgun.getAnimation().setReloadLoopTicks(12);
        shotgun.getAnimation().setReloadEndFrames(List.of(9401, 9402));
        shotgun.getAnimation().setReloadEndTicks(14);
        shotgun.getAnimation().setFrameModels(Map.of(9301, "spas12_sim_reload_loop_01"));
        shotgun.getAudio().setReloadClipInTick(16);
        shotgun.getAudio().setReloadBoltTick(34);
        manager.saveWeapon(shotgun);

        WeaponManager reloaded = new WeaponManager(tempFolder);
        reloaded.loadWeapons();
        WeaponDefinition loaded = reloaded.getWeapon("spas12_sim");
        assertNotNull(loaded);
        assertEquals(80, loaded.getStats().getReloadTicks());
        assertEquals(42, loaded.getStats().getReloadTacticalTicks());
        assertEquals(2, loaded.getAnimation().getAdsTickRate());
        assertEquals(2, loaded.getAnimation().getFireTickRate());
        assertEquals(1, loaded.getAnimation().getInspectTickRate());
        assertEquals(2, loaded.getAnimation().getReloadTickRate());
        assertEquals(List.of(9101, 9102, 9103), loaded.getAnimation().getReloadTacticalFrames());
        assertEquals(List.of(9201, 9202), loaded.getAnimation().getReloadIntroFrames());
        assertEquals(10, loaded.getAnimation().getReloadIntroTicks());
        assertEquals(List.of(9301, 9302, 9303), loaded.getAnimation().getReloadLoopFrames());
        assertEquals(12, loaded.getAnimation().getReloadLoopTicks());
        assertEquals(List.of(9401, 9402), loaded.getAnimation().getReloadEndFrames());
        assertEquals(14, loaded.getAnimation().getReloadEndTicks());
        assertEquals("spas12_sim_reload_loop_01", loaded.getAnimation().resolveModelKey("spas12_sim", loaded.getType(), 9301));
        assertEquals(16, loaded.getAudio().getReloadClipInTick());
        assertEquals(34, loaded.getAudio().getReloadBoltTick());
    }

    @Test
    public void testMeleeSlash2AndHeavyFramesYamlRoundTrip() throws IOException {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition knife = manager.createWeapon("sim_knife", WeaponType.KNIFE);
        knife.getAnimation().setIdleModelData(4000);
        knife.getAnimation().setFireFrames(List.of(4001, 4002));
        knife.getAnimation().setSlash2Frames(List.of(4101, 4102));
        knife.getAnimation().setHeavyFrames(List.of(4201, 4202));
        manager.saveWeapon(knife);

        String savedYaml = Files.readString(tempFolder.toPath().resolve("sim_knife.yml"));
        assertTrue(savedYaml.contains("slash2-frames:"));
        assertTrue(savedYaml.contains("heavy-frames:"));

        WeaponManager reloaded = new WeaponManager(tempFolder);
        reloaded.loadWeapons();
        WeaponDefinition loaded = reloaded.getWeapon("sim_knife");
        assertNotNull(loaded);
        assertEquals(List.of(4001, 4002), loaded.getAnimation().getFireFrames());
        assertEquals(List.of(4101, 4102), loaded.getAnimation().getSlash2Frames());
        assertEquals(List.of(4201, 4202), loaded.getAnimation().getHeavyFrames());
        assertEquals(List.of(4101, 4102), loaded.getAnimation().resolveSlash2Frames());
        assertEquals(List.of(4201, 4202), loaded.getAnimation().resolveHeavyFrames());
    }

    @Test
    public void testFireModeYamlRoundTrip() {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition rifle = manager.createWeapon("sim_m4", WeaponType.RIFLE);
        rifle.getStats().setFireModes(List.of(FireMode.SEMI_AUTO, FireMode.AUTO));
        rifle.getStats().setDefaultFireMode(FireMode.SEMI_AUTO);
        manager.saveWeapon(rifle);

        WeaponManager reloaded = new WeaponManager(tempFolder);
        reloaded.loadWeapons();
        WeaponDefinition loaded = reloaded.getWeapon("sim_m4");
        assertNotNull(loaded);
        assertEquals(List.of(FireMode.SEMI_AUTO, FireMode.AUTO), loaded.getStats().resolveSupportedFireModes(loaded.getType()));
        assertEquals(FireMode.SEMI_AUTO, loaded.getStats().resolveDefaultFireMode(loaded.getType()));
    }

    @Test
    public void testPack2AndPack3Aliases() {
        WeaponManager manager = new WeaponManager(tempFolder);
        Map<String, WeaponType> canonicalTargets = Map.ofEntries(
            Map.entry("hk_mp5a5", WeaponType.SMG),
            Map.entry("hk_mp7a1", WeaponType.SMG),
            Map.entry("xm1014", WeaponType.SHOTGUN),
            Map.entry("tec_9", WeaponType.PISTOL),
            Map.entry("m9amethyst", WeaponType.KNIFE),
            Map.entry("quenching", WeaponType.KNIFE),
            Map.entry("null_annihil", WeaponType.KNIFE),
            Map.entry("db_long", WeaponType.KNIFE),
            Map.entry("fn_evolys", WeaponType.RIFLE),
            Map.entry("scar_l", WeaponType.RIFLE),
            Map.entry("scar_mk20", WeaponType.SNIPER)
        );
        for (Map.Entry<String, WeaponType> entry : canonicalTargets.entrySet()) {
            manager.createWeapon(entry.getKey(), entry.getValue());
        }

        Map<String, String> aliases = Map.ofEntries(
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
            Map.entry("mk20", "scar_mk20")
        );
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            WeaponDefinition resolved = manager.getWeapon(entry.getKey());
            assertNotNull(resolved, "Missing alias target for " + entry.getKey());
            assertEquals(entry.getValue(), resolved.getId());
            assertEquals(resolved, manager.getWeapon(entry.getKey().toUpperCase()));
        }
    }

    @Test
    public void testAllThreeDeploymentPacksLoadCleanly() {
        File doneRoot = new File("CookieCSG_DONE");
        Assumptions.assumeTrue(doneRoot.isDirectory(), "CookieCSG_DONE deployment packs not present");
        Map<String, Integer> expectedCounts = Map.of(
            "Pack 1", 18,
            "Pack 2", 16,
            "Pack 3", 16
        );
        Map<Material, Map<Integer, String>> globalCmds = new HashMap<>();

        for (Map.Entry<String, Integer> packEntry : expectedCounts.entrySet()) {
            String packName = packEntry.getKey();
            File packDir = new File(doneRoot, packName);
            File weaponsDir = new File(packDir, "CookieCSG/weapons");
            File itemsDir = new File(packDir, "ItemsAdder/contents/cookie_csg/resourcepack/assets/cookie_csg/items");
            assertTrue(weaponsDir.isDirectory(), "Missing weapons dir in " + packName);
            assertTrue(itemsDir.isDirectory(), "Missing items dir in " + packName);

            WeaponManager manager = new WeaponManager(weaponsDir);
            manager.loadWeapons();
            assertEquals(packEntry.getValue(), manager.getWeapons().size(), "Unexpected weapon count in " + packName);

            if ("Pack 1".equals(packName)) {
                assertNull(manager.getWeapon("karambit_obsidian"));
                assertNull(manager.getWeapon("usp_silencer"));
                assertNull(manager.getWeapon("spas12"));
            }

            for (WeaponDefinition def : manager.getWeapons().values()) {
                WeaponAnimation anim = def.getAnimation();
                assertNotNull(anim, "Missing animation in " + packName + "/" + def.getId());
                Map<Integer, String> matCmds = globalCmds.computeIfAbsent(def.getMaterial(), k -> new HashMap<>());

                Set<Integer> allCmds = new HashSet<>();
                if (anim.getIdleModelData() > 0) {
                    allCmds.add(anim.getIdleModelData());
                }
                if (anim.getDeployModelData() > 0) {
                    allCmds.add(anim.getDeployModelData());
                }
                if (anim.getAdsModelData() > 0) {
                    allCmds.add(anim.getAdsModelData());
                }
                if (anim.getAdsFireModelData() > 0) {
                    allCmds.add(anim.getAdsFireModelData());
                }
                allCmds.addAll(anim.getDeployFrames());
                allCmds.addAll(anim.getAdsInFrames());
                allCmds.addAll(anim.getAdsFireFrames());
                allCmds.addAll(anim.getAdsOutFrames());
                allCmds.addAll(anim.getFireFrames());
                allCmds.addAll(anim.getSlash2Frames());
                allCmds.addAll(anim.getHeavyFrames());
                allCmds.addAll(anim.getInspectFrames());
                allCmds.addAll(anim.getReloadFrames());
                allCmds.addAll(anim.getReloadTacticalFrames());
                allCmds.addAll(anim.getReloadIntroFrames());
                allCmds.addAll(anim.getReloadLoopFrames());
                allCmds.addAll(anim.getReloadEndFrames());

                for (int cmd : allCmds) {
                    if (cmd <= 0) {
                        continue;
                    }
                    String owner = packName + ":" + def.getId();
                    String prev = matCmds.putIfAbsent(cmd, owner);
                    assertTrue(prev == null || prev.equals(owner), "Global CMD collision " + cmd + " between " + prev + " and " + owner);
                    String modelKey = anim.resolveModelKey(def.getId(), def.getType(), cmd);
                    File itemFile = new File(itemsDir, modelKey + ".json");
                    assertTrue(itemFile.isFile(), "Missing ItemsAdder item json " + itemFile.getPath() + " for cmd " + cmd);
                }
            }
        }
    }

    @Test
    public void testNoModelVanillaWeaponClearsItemModelAndCmd() {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition vanillaPistol = manager.createWeapon("custom_glock", WeaponType.PISTOL);
        assertNotNull(vanillaPistol);
        assertEquals(0, vanillaPistol.getAnimation().getIdleModelData());
        assertNull(manager.resolveItemModel(vanillaPistol, 0));
    }

    @Test
    public void testItemModelDisabledYamlRoundTrip() {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition custom = manager.createWeapon("user_gun", WeaponType.RIFLE);
        custom.getAnimation().setIdleModelData(5001);
        custom.getAnimation().setItemModelNamespace("none");
        manager.saveWeapon(custom);

        WeaponManager reloaded = new WeaponManager(tempFolder);
        reloaded.loadWeapons();
        WeaponDefinition loaded = reloaded.getWeapon("user_gun");
        assertNotNull(loaded);
        assertEquals("none", loaded.getAnimation().getItemModelNamespace());
        assertTrue(loaded.getAnimation().isItemModelDisabled());
        assertNull(reloaded.resolveItemModel(loaded, 5001));
    }

    @Test
    public void testDefaultModeOmitsAnimationSectionAndModelModeIncludesIt() throws IOException {
        WeaponManager manager = new WeaponManager(tempFolder);
        WeaponDefinition defaultGun = manager.createWeapon("simple_glock", WeaponType.PISTOL, WeaponMode.DEFAULT);
        assertNotNull(defaultGun);
        assertTrue(defaultGun.isDefaultMode());
        assertTrue(!defaultGun.isModelMode());

        File defaultFile = new File(tempFolder, "simple_glock.yml");
        assertTrue(defaultFile.isFile());
        String defaultRaw = Files.readString(defaultFile.toPath());
        assertTrue(defaultRaw.contains("mode: default"));
        assertTrue(!defaultRaw.contains("animation:"), "DEFAULT mode YAML must not contain animation section");

        WeaponDefinition modelGun = manager.createWeapon("custom_3d_rifle", WeaponType.RIFLE, WeaponMode.MODEL);
        assertNotNull(modelGun);
        assertTrue(modelGun.isModelMode());
        File modelFile = new File(tempFolder, "custom_3d_rifle.yml");
        assertTrue(modelFile.isFile());
        String modelRaw = Files.readString(modelFile.toPath());
        assertTrue(modelRaw.contains("mode: model"));
        assertTrue(modelRaw.contains("animation:"), "MODEL mode YAML must contain animation section");

        WeaponManager reloaded = new WeaponManager(tempFolder);
        reloaded.loadWeapons();
        assertTrue(reloaded.getWeapon("simple_glock").isDefaultMode());
        assertTrue(reloaded.getWeapon("custom_3d_rifle").isModelMode());
    }

    @Test
    public void testExtractBundledDefaultModelsWritesManifestFiles() {
        File pluginsDir = new File(tempFolder, "plugins");
        File weaponsDir = new File(pluginsDir, "CookieCSG/weapons");
        File iaDir = new File(pluginsDir, "ItemsAdder");
        assertTrue(iaDir.mkdirs());

        WeaponManager manager = new WeaponManager(weaponsDir);
        int extracted = manager.extractBundledDefaultModels(pluginsDir);
        assertTrue(extracted > 0);
        assertTrue(new File(pluginsDir, "CookieCSG/default-pack/ItemsAdder/contents/cookie_csg/configs/weapons.yml").isFile());
        assertTrue(new File(iaDir, "contents/cookie_csg/configs/weapons.yml").isFile());
        assertTrue(new File(iaDir, "contents/cookie_csg/resourcepack/assets/cookie_csg/items/p1911.json").isFile());
        assertTrue(new File(iaDir, "contents/cookie_csg/resourcepack/assets/cookie_csg/items/bayonet.json").isFile());
        assertTrue(new File(iaDir, "contents/cookie_csg/resourcepack/assets/cookie_csg/textures/item/p1911_icon.png").isFile());
        assertTrue(new File(iaDir, "contents/cookie_csg/resourcepack/assets/cookie_csg/textures/item/bayonet_icon.png").isFile());

        assertEquals(0, manager.extractBundledDefaultModels(pluginsDir));
    }

    @Test
    public void testDefaultPackLoadedWithLodNoArmsAndHotbarIcons() throws IOException {
        File doneRoot = new File("CookieCSG_DONE");
        File doneDefault = new File(doneRoot, "Default");
        File defaultDir = doneDefault.isDirectory() ? doneDefault : new File("Default");
        Assumptions.assumeTrue(defaultDir.isDirectory(), "Default pack directory not present");

        File weaponsDir = new File(defaultDir, "CookieCSG/weapons");
        File rpAssets = new File(defaultDir, "ResourcePack/assets/cookie_csg");
        File rpItems = new File(rpAssets, "items");
        File rpModels = new File(rpAssets, "models/item");
        File rpTex = new File(rpAssets, "textures/item");
        File iaCookieCsg = new File(defaultDir, "ItemsAdder/contents/cookie_csg/resourcepack/assets/cookie_csg");
        File iaItems = new File(iaCookieCsg, "items");
        File iaModels = new File(iaCookieCsg, "models/item");
        File iaTex = new File(iaCookieCsg, "textures/item");
        assertTrue(weaponsDir.isDirectory());
        assertTrue(iaItems.isDirectory());

        assertTrue(new File(iaTex, "p1911_icon.png").isFile());
        assertTrue(new File(iaTex, "bayonet_icon.png").isFile());
        assertTrue(new File(iaModels, "p1911_icon.json").isFile());
        assertTrue(new File(iaModels, "bayonet_icon.json").isFile());
        assertTrue(!new File(iaModels, "tactical_arm.json").exists(), "Default pack must not contain arm model");
        assertTrue(!new File(iaTex, "tactical_sleeve.png").exists(), "Default pack must not contain sleeve texture");

        if (rpItems.isDirectory()) {
            assertTrue(new File(defaultDir, "ResourcePack/pack.mcmeta").isFile());
            assertTrue(new File(defaultDir, "ResourcePack/pack.png").isFile());
            assertTrue(new File(defaultDir, "CookieCSG_Default_ResourcePack.zip").isFile());
            assertTrue(new File(defaultDir, "ResourcePack/assets/minecraft/items/iron_hoe.json").isFile());
            assertTrue(new File(defaultDir, "ResourcePack/assets/minecraft/items/iron_sword.json").isFile());
            assertTrue(new File(rpTex, "p1911_icon.png").isFile());
            assertTrue(new File(rpTex, "bayonet_icon.png").isFile());
            assertTrue(new File(rpModels, "p1911_icon.json").isFile());
            assertTrue(new File(rpModels, "bayonet_icon.json").isFile());
            assertTrue(!new File(rpModels, "tactical_arm.json").exists(), "Default pack must not contain arm model");
            assertTrue(!new File(rpTex, "tactical_sleeve.png").exists(), "Default pack must not contain sleeve texture");
        }

        WeaponManager defaultMgr = new WeaponManager(weaponsDir);
        defaultMgr.loadWeapons();
        assertEquals(2, defaultMgr.getWeapons().size());

        WeaponDefinition p1911 = defaultMgr.getWeapon("p1911");
        WeaponDefinition bayonet = defaultMgr.getWeapon("bayonet");
        assertNotNull(p1911);
        assertNotNull(bayonet);
        assertEquals(WeaponMode.MODEL, p1911.getMode());
        assertEquals(WeaponMode.MODEL, bayonet.getMode());
        assertEquals(p1911, defaultMgr.getWeapon("m1911"));
        assertEquals(p1911, defaultMgr.getWeapon("colt1911"));
        assertEquals(WeaponType.PISTOL, p1911.getType());
        assertEquals(WeaponType.KNIFE, bayonet.getType());
        assertTrue(!p1911.getAnimation().getFireFrames().isEmpty());
        assertTrue(!p1911.getAnimation().getInspectFrames().isEmpty());
        assertTrue(!p1911.getAnimation().getReloadFrames().isEmpty());
        assertTrue(!p1911.getAnimation().getAdsInFrames().isEmpty());
        assertTrue(!bayonet.getAnimation().getFireFrames().isEmpty());
        assertTrue(!bayonet.getAnimation().getInspectFrames().isEmpty());

        Map<Material, Set<Integer>> existingCmds = new HashMap<>();
        if (doneRoot.isDirectory()) {
            for (String existingPack : List.of("Pack 1", "Pack 2", "Pack 3", "Cookieee")) {
                File wf = new File(doneRoot, existingPack + "/CookieCSG/weapons");
                if (!wf.isDirectory()) {
                    continue;
                }
                WeaponManager mgr = new WeaponManager(wf);
                mgr.loadWeapons();
                for (WeaponDefinition d : mgr.getWeapons().values()) {
                    Set<Integer> s = existingCmds.computeIfAbsent(d.getMaterial(), k -> new HashSet<>());
                    collectAllCmds(d.getAnimation(), s);
                }
            }
        }

        for (WeaponDefinition def : defaultMgr.getWeapons().values()) {
            Set<Integer> cmds = new HashSet<>();
            collectAllCmds(def.getAnimation(), cmds);
            Set<Integer> used = existingCmds.getOrDefault(def.getMaterial(), Set.of());
            for (int c : cmds) {
                assertTrue(!used.contains(c), "CMD collision with existing packs: " + c);
                String key = def.getAnimation().resolveModelKey(def.getId(), def.getType(), c);
                File iaJson = new File(iaItems, key + ".json");
                assertTrue(iaJson.isFile(), "Missing IA item json: " + iaJson.getPath());
                String content = Files.readString(iaJson.toPath());
                assertTrue(content.contains("cookie_csg:item/" + def.getId() + "_icon"), "Missing GUI hotbar icon in " + key);
                assertTrue(!content.contains("_ra_") && !content.contains("_la_"), "Arm reference found in " + key);
                if (rpItems.isDirectory()) {
                    File rpJson = new File(rpItems, key + ".json");
                    assertTrue(rpJson.isFile(), "Missing RP item json: " + rpJson.getPath());
                }
            }
        }
    }

    private void collectAllCmds(WeaponAnimation anim, Set<Integer> out) {
        if (anim.getIdleModelData() > 0) out.add(anim.getIdleModelData());
        if (anim.getDeployModelData() > 0) out.add(anim.getDeployModelData());
        if (anim.getAdsModelData() > 0) out.add(anim.getAdsModelData());
        if (anim.getAdsFireModelData() > 0) out.add(anim.getAdsFireModelData());
        out.addAll(anim.getDeployFrames());
        out.addAll(anim.getAdsInFrames());
        out.addAll(anim.getAdsFireFrames());
        out.addAll(anim.getAdsOutFrames());
        out.addAll(anim.getFireFrames());
        out.addAll(anim.getSlash2Frames());
        out.addAll(anim.getHeavyFrames());
        out.addAll(anim.getInspectFrames());
        out.addAll(anim.getReloadFrames());
        out.addAll(anim.getReloadTacticalFrames());
        out.addAll(anim.getReloadIntroFrames());
        out.addAll(anim.getReloadLoopFrames());
        out.addAll(anim.getReloadEndFrames());
    }

    private void assertUniqueModel(Set<Integer> used, int modelData, String weaponId) {
        if (modelData > 0) {
            assertTrue(used.add(modelData), "Duplicate model data " + modelData + " in " + weaponId);
        }
    }
}
