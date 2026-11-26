package com.cookie.csg.engine;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.command.CSGCommand;
import com.cookie.csg.gui.GuiManager;
import com.cookie.csg.gui.MainMenu;
import com.cookie.csg.gui.WeaponEditorMenu;
import com.cookie.csg.listener.WeaponInteractionListener;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponAnimation;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.papermc.paper.entity.TeleportFlag;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class AnimationSimulatorTest {

    private static final List<String> ALL_18_WEAPONS = List.of(
        "ak47", "aug", "awp", "kar98", "m700", "deagle", "deagle_golden", "taurus500",
        "dual_berettas", "p90", "m870", "vector",
        "combat_knife", "tactical_axe", "cobra_vanguard",
        "csgo_dark_star", "csgo_m4a1", "csgo_m249"
    );

    private static FakeScheduler sharedScheduler;
    private static Map<UUID, Player> onlinePlayers;
    private static UnsafeValues unsafeValues;
    private static ItemFactory itemFactory;
    private static Server serverProxy;

    private CookieCSGPlugin plugin;
    private WeaponManager weaponManager;
    private AdsManager adsManager;
    private RaycastRecoilEngine recoilEngine;
    private MeleeEngine meleeEngine;
    private AnimationEngine animationEngine;
    private WeaponInteractionListener listener;

    private FakePlayerState playerState;
    private Player player;

    @BeforeAll
    static void bootstrapPaperEnv() throws Exception {
        installMockRegistryAccess();
        installMockServerBuildInfo();

        sharedScheduler = new FakeScheduler();
        onlinePlayers = new HashMap<>();

        unsafeValues = (UnsafeValues) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{UnsafeValues.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("createEmptyStack".equals(name)) {
                    return new FakeItemStack(Material.AIR, 0);
                }
                if ("isSupportedApiVersion".equals(name)) {
                    return true;
                }
                return defaultValue(method.getReturnType());
            }
        );

        itemFactory = (ItemFactory) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{ItemFactory.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("getItemMeta".equals(name)) {
                    return FakeItemMetaState.newProxy();
                }
                if ("isApplicable".equals(name)) {
                    return true;
                }
                if ("equals".equals(name)) {
                    return args[0] == args[1];
                }
                if ("asMetaFor".equals(name)) {
                    ItemMeta m = (ItemMeta) args[0];
                    return m != null ? m.clone() : FakeItemMetaState.newProxy();
                }
                return defaultValue(method.getReturnType());
            }
        );

        PluginManager pluginManager = (PluginManager) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{PluginManager.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );

        Logger logger = Logger.getLogger("CookieCSG-Sim");

        if (Bukkit.getServer() == null) {
            serverProxy = (Server) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getScheduler" -> sharedScheduler.asBukkitScheduler();
                    case "getItemFactory" -> itemFactory;
                    case "getUnsafe" -> unsafeValues;
                    case "getPluginManager" -> pluginManager;
                    case "getLogger" -> logger;
                    case "getPlayer" -> args != null && args.length == 1 && args[0] instanceof UUID id
                        ? onlinePlayers.get(id) : null;
                    case "getOnlinePlayers" -> new ArrayList<>(onlinePlayers.values());
                    case "createInventory" -> {
                        InventoryHolder holder = args != null && args.length >= 1 ? (InventoryHolder) args[0] : null;
                        int size = args != null && args.length >= 2 && args[1] instanceof Integer s ? s : 54;
                        ItemStack[] contents = new ItemStack[size];
                        yield (Inventory) Proxy.newProxyInstance(
                            AnimationSimulatorTest.class.getClassLoader(),
                            new Class<?>[]{Inventory.class},
                            (ip, im, ia) -> switch (im.getName()) {
                                case "getHolder" -> holder;
                                case "getSize" -> size;
                                case "getItem" -> {
                                    int idx = (int) ia[0];
                                    yield idx >= 0 && idx < contents.length ? contents[idx] : null;
                                }
                                case "setItem" -> {
                                    int idx = (int) ia[0];
                                    if (idx >= 0 && idx < contents.length) {
                                        contents[idx] = (ItemStack) ia[1];
                                    }
                                    yield null;
                                }
                                case "clear" -> {
                                    Arrays.fill(contents, null);
                                    yield null;
                                }
                                case "getContents" -> contents;
                                default -> defaultValue(im.getReturnType());
                            }
                        );
                    }
                    case "getVersion", "getBukkitVersion", "getName" -> "Paper-1.21.4-Sim";
                    case "isPrimaryThread" -> true;
                    default -> defaultValue(method.getReturnType());
                }
            );
            Bukkit.setServer(serverProxy);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void installMockRegistryAccess() throws Exception {
        Class<?> holderClass = Class.forName("io.papermc.paper.registry.RegistryAccessHolder");
        Field instanceField = holderClass.getDeclaredField("INSTANCE");

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        Map<NamespacedKey, Object> itemTypeCache = new HashMap<>();
        Map<Class<?>, Registry<?>> registryCache = new HashMap<>();

        RegistryAccess mockAccess = (RegistryAccess) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{RegistryAccess.class},
            (proxy, method, args) -> {
                Object keyObj = args != null && args.length > 0 ? args[0] : null;
                Class<?> targetClass = keyObj instanceof Class<?> c ? c : Object.class;
                if (keyObj instanceof RegistryKey<?> rk) {
                    if (rk == RegistryKey.ITEM) {
                        targetClass = ItemType.class;
                    } else if (rk == RegistryKey.MOB_EFFECT) {
                        targetClass = PotionEffectType.class;
                    } else if (rk == RegistryKey.ATTRIBUTE) {
                        targetClass = Attribute.class;
                    }
                }
                Class<?> finalTarget = targetClass;
                return registryCache.computeIfAbsent(finalTarget, cls -> createMockRegistry(finalTarget, itemTypeCache));
            }
        );

        Object staticBase = unsafe.staticFieldBase(instanceField);
        long staticOffset = unsafe.staticFieldOffset(instanceField);
        unsafe.putObject(staticBase, staticOffset, Optional.of(mockAccess));
    }

    private static void installMockServerBuildInfo() throws Exception {
        Class<?> holderClass = Class.forName("io.papermc.paper.ServerBuildInfo$1Holder");
        Field instanceField = holderClass.getDeclaredField("INSTANCE");

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        io.papermc.paper.ServerBuildInfo mockInfo = (io.papermc.paper.ServerBuildInfo) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{io.papermc.paper.ServerBuildInfo.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "brandId" -> io.papermc.paper.ServerBuildInfo.BRAND_PAPER_ID;
                case "brandName" -> "Paper-Sim";
                case "minecraftVersionId", "minecraftVersionName" -> "1.21.4";
                case "asString" -> "Paper-1.21.4-Sim";
                case "isBrandCompatible" -> true;
                case "buildNumber" -> java.util.OptionalInt.of(1);
                case "buildTime" -> java.time.Instant.now();
                case "gitBranch", "gitCommit" -> Optional.empty();
                default -> defaultValue(method.getReturnType());
            }
        );

        Object staticBase = unsafe.staticFieldBase(instanceField);
        long staticOffset = unsafe.staticFieldOffset(instanceField);
        unsafe.putObject(staticBase, staticOffset, Optional.of(mockInfo));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry createMockRegistry(Class<?> targetClass, Map<NamespacedKey, Object> itemTypeCache) {
        Map<NamespacedKey, Object> keyedCache = new HashMap<>();
        return (Registry) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{Registry.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("get".equals(name) || "getOrThrow".equals(name)) {
                    NamespacedKey key = (NamespacedKey) args[0];
                    if (targetClass == ItemType.class) {
                        return itemTypeCache.computeIfAbsent(key, k -> createMockItemType(k));
                    }
                    if (targetClass == PotionEffectType.class) {
                        return keyedCache.computeIfAbsent(key, k -> createMockPotionEffectType(k));
                    }
                    return keyedCache.computeIfAbsent(key, k -> createKeyedProxy(targetClass, k));
                }
                if ("iterator".equals(name)) {
                    return Collections.emptyIterator();
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static ItemType createMockItemType(NamespacedKey key) {
        Material mat = Material.matchMaterial(key.getKey());
        return (ItemType) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{ItemType.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "createItemStack" -> {
                    int amt = (args != null && args.length > 0 && args[0] instanceof Integer i) ? i : 1;
                    yield new FakeItemStack(mat != null ? mat : Material.STONE, amt);
                }
                case "getKey" -> key;
                case "asMaterial" -> mat;
                case "getMaxStackSize" -> 64;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static PotionEffectType createMockPotionEffectType(NamespacedKey key) {
        return new PotionEffectType() {
            @Override public PotionEffect createEffect(int duration, int amplifier) { return new PotionEffect(this, duration, amplifier); }
            @Override public boolean isInstant() { return false; }
            @Override public org.bukkit.potion.PotionEffectTypeCategory getCategory() { return org.bukkit.potion.PotionEffectTypeCategory.NEUTRAL; }
            @Override public org.bukkit.Color getColor() { return org.bukkit.Color.GRAY; }
            @Override public double getDurationModifier() { return 1.0; }
            @Override public int getId() { return Math.abs(key.hashCode()); }
            @Override public String getName() { return key.getKey().toUpperCase(); }
            @Override public NamespacedKey getKey() { return key; }
            @Override public String getTranslationKey() { return "effect." + key.getNamespace() + "." + key.getKey(); }
            @Override public String translationKey() { return getTranslationKey(); }
            @Override public Map<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> getEffectAttributes() { return Map.of(); }
            @Override public double getAttributeModifierAmount(org.bukkit.attribute.Attribute attribute, int effectAmplifier) { return 0; }
            @Override public Category getEffectCategory() { return Category.NEUTRAL; }
        };
    }

    private static Object createKeyedProxy(Class<?> iface, NamespacedKey key) {
        if (!iface.isInterface()) {
            return null;
        }
        return Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{iface},
            (proxy, method, args) -> switch (method.getName()) {
                case "getKey" -> key;
                case "equals" -> args[0] == proxy;
                case "hashCode" -> key.hashCode();
                case "toString" -> key.toString();
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        sharedScheduler.reset();
        onlinePlayers.clear();

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        plugin = (CookieCSGPlugin) unsafe.allocateInstance(CookieCSGPlugin.class);

        setField(plugin, "weaponManager", new WeaponManager(new File("CookieCSG_DONE/Pack 1/CookieCSG/weapons")));
        plugin.getWeaponManager().loadWeapons();

        adsManager = new AdsManager(plugin);
        recoilEngine = new RaycastRecoilEngine(plugin);
        meleeEngine = new MeleeEngine(plugin);
        animationEngine = new AnimationEngine(plugin, adsManager);

        setField(plugin, "adsManager", adsManager);
        setField(plugin, "recoilEngine", recoilEngine);
        setField(plugin, "meleeEngine", meleeEngine);
        setField(plugin, "animationEngine", animationEngine);
        setField(plugin, "guiManager", new GuiManager(plugin));
        setField(plugin, "rightClickFireForGuns", false);
        setField(plugin, "holdToAds", true);
        setField(plugin, "holdAdsReleaseMs", 350L);

        weaponManager = plugin.getWeaponManager();
        listener = new WeaponInteractionListener(plugin);

        playerState = new FakePlayerState();
        player = playerState.proxy;
        onlinePlayers.put(playerState.uuid, player);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void testAll17WeaponsLoaded() {
        assertEquals(18, weaponManager.getWeapons().size(), "Expected 18 weapons loaded from CookieCSG_DONE/Pack 1/CookieCSG/weapons");
        for (String id : ALL_18_WEAPONS) {
            assertNotNull(weaponManager.getWeapon(id), "Missing weapon definition: " + id);
        }
    }

    @Test
    void testCsgGiveSetsItemModelAndCmdAll17() {
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            ItemStack csgItem = weaponManager.buildItemStack(weapon);
            assertTrue(weaponManager.isWeapon(csgItem), id + ": /csg give item not recognized");
            assertEquals(id, weaponManager.getWeaponId(csgItem));
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(csgItem), id + ": idle CMD mismatch on /csg give");
            assertNotNull(getItemModel(csgItem), id + ": /csg give item must set 1.21.4+ ItemModel");
            assertEquals("cookie_csg", getItemModel(csgItem).getNamespace(), id + ": ItemModel namespace must be cookie_csg");
        }
    }

    @Test
    void testItemsAdderItemWithoutPdcRecognizedAndDeploysAll17() {
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            ItemStack iaCmdOnlyItem = new FakeItemStack(weapon.getMaterial(), 1);
            iaCmdOnlyItem.editMeta(meta -> meta.setCustomModelData(weapon.getAnimation().getIdleModelData()));
            assertTrue(weaponManager.isWeapon(iaCmdOnlyItem), id + ": CMD-only /ia item MUST be recognized");
            assertEquals(id, weaponManager.getWeapon(iaCmdOnlyItem).getId(), id + ": CMD-only /ia item mapped to wrong weapon");

            ItemStack iaItem = buildFakeItemsAdderItem(weapon);
            assertTrue(weaponManager.isWeapon(iaItem), id + ": /ia item without cookiecsg:weapon_id PDC MUST be recognized");
            assertNotNull(weaponManager.getWeapon(iaItem), id + ": /ia item getWeapon() returned null");
            assertEquals(id, weaponManager.getWeapon(iaItem).getId(), id + ": /ia item mapped to wrong weapon");

            playerState.inventorySlots[0] = iaItem;
            playerState.inventorySlots[1] = new FakeItemStack(Material.AIR, 0);
            playerState.heldSlot = 1;

            switchHotbarSlot(1, 0);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertNotNull(st, id + ": PlayerWeaponState missing after deploy");
            assertEquals(WeaponState.DRAWING, st.getCurrentState(), id + ": expected DRAWING state on hotbar equip");
            List<Integer> deployFrames = weapon.getAnimation().getDeployFrames();
            if (deployFrames != null && !deployFrames.isEmpty()) {
                assertEquals(deployFrames.get(0), getCmd(iaItem), id + ": first deploy frame CMD not applied at t=0");
            } else if (weapon.getAnimation().getDeployModelData() > 0) {
                assertEquals(weapon.getAnimation().getDeployModelData(), getCmd(iaItem), id + ": deploy CMD not applied");
            }

            int deployTicks = weapon.getAnimation().getDeployTicks() > 0 ? weapon.getAnimation().getDeployTicks() : 6;
            List<Integer> observedDeployCmds = new ArrayList<>();
            for (int t = 0; t <= deployTicks + 1; t++) {
                observedDeployCmds.add(getCmd(iaItem));
                sharedScheduler.stepOneTick();
            }
            if (deployFrames != null && !deployFrames.isEmpty()) {
                for (int expectedCmd : deployFrames) {
                    assertTrue(observedDeployCmds.contains(expectedCmd), id + ": deploy frame CMD " + expectedCmd + " never rendered");
                }
            }

            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": failed to settle to IDLE after deployTicks");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(iaItem), id + ": failed to settle to idle CMD after deploy");
            assertNotNull(getItemModel(iaItem), id + ": ItemModel null after deploy settle");
            assertEquals("cookie_csg", getItemModel(iaItem).getNamespace(), id + ": ItemModel namespace mismatch after deploy");
        }
    }

    @Test
    void testInspectDurationAndFrameProgressionAll17() {
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            ItemStack item = equipWeaponAtSlot0(weapon);

            List<Integer> inspectFrames = weapon.getAnimation().getInspectFrames();
            assertNotNull(inspectFrames, id + ": inspectFrames null");
            assertTrue(inspectFrames.size() >= 56, id + ": inspectFrames too few (" + inspectFrames.size() + ")");

            int rate = weapon.getAnimation().getInspectTickRate();
            int totalTicks = inspectFrames.size() * rate;
            assertTrue(totalTicks >= 56, id + ": inspect duration too short (" + totalTicks + " ticks = " + (totalTicks * 0.05) + "s)");

            fireSwapHand();
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": F key did not enter INSPECTING");

            List<Integer> observedCmds = new ArrayList<>();
            Set<NamespacedKey> observedModels = new LinkedHashSet<>();
            for (int t = 0; t <= totalTicks + 2; t++) {
                observedCmds.add(getCmd(item));
                if (getItemModel(item) != null) {
                    observedModels.add(getItemModel(item));
                }
                sharedScheduler.stepOneTick();
            }

            for (int expectedCmd : inspectFrames) {
                assertTrue(observedCmds.contains(expectedCmd), id + ": inspect frame CMD " + expectedCmd + " never rendered");
            }
            File itemsDir = new File("CookieCSG_DONE/Pack 1/ItemsAdder/contents/cookie_csg/resourcepack/assets/cookie_csg/items");
            for (NamespacedKey modelKey : observedModels) {
                assertEquals("cookie_csg", modelKey.getNamespace(), id + ": unexpected ItemModel namespace " + modelKey);
                File itemDef = new File(itemsDir, modelKey.getKey() + ".json");
                assertTrue(itemDef.exists(), id + ": runtime ItemModel missing 1.21.4+ dispatch file: " + itemDef.getPath());
            }
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": did not return to IDLE after inspect completed");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": CMD did not reset to idle after inspect");
            assertTrue(observedModels.size() >= 2, id + ": inspect never changed 1.21.4+ ItemModel across frames");
            assertNotNull(getItemModel(item), id + ": ItemModel null after inspect");
        }
    }

    @Test
    void testInspectSecondPressFCancelsBackToIdleAll17() {
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            ItemStack item = equipWeaponAtSlot0(weapon);
            NamespacedKey idleModel = getItemModel(item);

            fireSwapHand();
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": inspect start failed");
            int rate = Math.max(1, weapon.getAnimation().getInspectTickRate());
            sharedScheduler.advanceTicks(rate * 2);
            assertNotEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": should be on non-idle inspect frame");

            fireSwapHand();
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": pressing F again while INSPECTING must cancel back to IDLE");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": pressing F again left inspect CMD frozen");
            assertNotNull(idleModel, id + ": idle ItemModel missing");
            assertEquals(idleModel, getItemModel(item), id + ": pressing F again left inspect ItemModel frozen");
        }
    }

    @Test
    void testInspectInterruptLeftClickShootAndCooldownNeverFreezeAll17() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            setUpQuietly();
            ItemStack item = equipWeaponAtSlot0(weapon);
            NamespacedKey idleModel = getItemModel(item);

            int fireTicks = Math.max(20, weapon.getAnimation().getFireFrames().size() * Math.max(1, weapon.getAnimation().getFireTickRate()) + 2);
            Action shootAction = Action.LEFT_CLICK_AIR;
            startInspectAndAdvanceTwoFrames(weapon, item);
            fireInteract(shootAction, EquipmentSlot.HAND);
            sharedScheduler.advanceTicks(fireTicks);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertNotEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": Shoot did not exit INSPECTING");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": Shoot left frozen non-idle CMD");

            setUpQuietly();
            item = equipWeaponAtSlot0(weapon);
            fireInteract(shootAction, EquipmentSlot.HAND);
            sharedScheduler.advanceTicks(fireTicks);
            animationEngine.getOrCreateState(playerState.uuid).setLastFireTime(System.currentTimeMillis());
            startInspectAndAdvanceTwoFrames(weapon, item);
            int frozenInspectCmd = getCmd(item);
            fireInteract(shootAction, EquipmentSlot.HAND);
            st = animationEngine.getState(playerState.uuid);
            assertNotEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": Left-Click during cooldown stayed in INSPECTING");
            assertNotEquals(frozenInspectCmd, getCmd(item), id + ": Left-Click during cooldown froze on inspect frame " + frozenInspectCmd);
            if (!weapon.getType().isGun()) {
                int meleeTicks = Math.max(1, weapon.getAnimation().getFireFrames().size() * Math.max(1, weapon.getAnimation().getFireTickRate()));
                sharedScheduler.advanceTicks(meleeTicks + 2);
            }
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": Left-Click during cooldown did not reset CMD to idle");
            assertNotNull(idleModel, id + ": idle ItemModel missing");
            assertEquals(idleModel, getItemModel(item), id + ": Left-Click during cooldown did not reset ItemModel to idle");
        }
    }

    @Test
    void testInspectInterruptLeftClickEmptyClipNeverFreezeAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun()) {
                continue;
            }

            setUpQuietly();
            ItemStack item = equipWeaponAtSlot0(weapon);
            NamespacedKey idleModel = getItemModel(item);
            PlayerWeaponState st = animationEngine.getOrCreateState(playerState.uuid);
            st.setAmmoInClip(0);
            startInspectAndAdvanceTwoFrames(weapon, item);
            int inspectCmdBeforeEmptyClick = getCmd(item);
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertNotEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": Fire on empty clip stayed in INSPECTING");
            assertNotEquals(inspectCmdBeforeEmptyClick, getCmd(item), id + ": Fire on empty clip froze on inspect frame");
            sharedScheduler.advanceTicks(effectiveReloadDurationTicks(weapon, 0) + 5);
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": empty clip auto-reload did not settle to idle CMD");
            assertNotNull(idleModel, id + ": idle ItemModel missing");
            assertEquals(idleModel, getItemModel(item), id + ": empty clip auto-reload did not settle to idle ItemModel");
        }
    }

    @Test
    void testInspectInterruptRightClickAdsOrMeleeNeverFreezeAll17() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            setUpQuietly();
            ItemStack item = equipWeaponAtSlot0(weapon);
            NamespacedKey idleModel = getItemModel(item);
            startInspectAndAdvanceTwoFrames(weapon, item);
            int inspectCmdBeforeAdsOrHeavy = getCmd(item);
            Action secondaryAction = Action.RIGHT_CLICK_AIR;
            fireInteract(secondaryAction, EquipmentSlot.HAND);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertNotEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": ADS/Heavy did not exit INSPECTING");
            assertNotEquals(inspectCmdBeforeAdsOrHeavy, getCmd(item), id + ": ADS/Heavy froze on inspect frame");
            if (weapon.getType().isGun() && weapon.getAdsConfig().isEnabled()) {
                assertTrue(adsManager.isAiming(player), id + ": Right-Click while inspecting did not activate ADS");
                List<Integer> adsIn = weapon.getAnimation().getAdsInFrames();
                if (adsIn != null && !adsIn.isEmpty()) {
                    assertEquals(adsIn.get(0), getCmd(item), id + ": Right-Click ADS entry frame mismatch");
                    int adsRate = Math.max(1, weapon.getAnimation().getAdsTickRate());
                    sharedScheduler.advanceTicks(adsIn.size() * adsRate + 1);
                }
                assertEquals(weapon.getAnimation().getAdsModelData(), getCmd(item), id + ": Right-Click ADS CMD mismatch");
            } else {
                int fireRate = Math.max(1, weapon.getAnimation().getFireTickRate());
                int heavyTicks = weapon.getAnimation().resolveHeavyFrames().size() * fireRate;
                sharedScheduler.advanceTicks(Math.max(12, heavyTicks + 2));
                assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": Melee Right-Click did not return to idle CMD");
                assertNotNull(idleModel, id + ": idle ItemModel missing");
                assertEquals(idleModel, getItemModel(item), id + ": Melee Right-Click did not return to idle ItemModel");
            }
        }
    }

    @Test
    void testInspectInterruptHotbarSwitchResetsPreviousSlotAll17() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            setUpQuietly();
            ItemStack item = equipWeaponAtSlot0(weapon);
            NamespacedKey idleModel = getItemModel(item);
            startInspectAndAdvanceTwoFrames(weapon, item);
            switchHotbarSlot(0, 1);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": Hotbar switch did not clear INSPECTING state");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": Hotbar switch left previous slot frozen on inspect CMD");
            assertNotNull(idleModel, id + ": idle ItemModel missing");
            assertEquals(idleModel, getItemModel(item), id + ": Hotbar switch left previous slot frozen on inspect ItemModel");
        }
    }

    @Test
    void testRightClickAdsDoubleFireSingleTickAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun() || !weapon.getAdsConfig().isEnabled()) {
                continue;
            }
            setUpQuietly();
            ItemStack item = equipWeaponAtSlot0(weapon);

            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND);

            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            assertTrue(adsManager.isAiming(player), id + ": Double HAND+OFF_HAND interact in 1 tick failed to enter ADS");
            assertTrue(st.isAiming(), id + ": PlayerWeaponState.isAiming() false after HAND+OFF_HAND interact");
            assertEquals(WeaponState.AIMING_ADS, st.getCurrentState(), id + ": expected AIMING_ADS state");
            List<Integer> adsIn = weapon.getAnimation().getAdsInFrames();
            int adsRate = Math.max(1, weapon.getAnimation().getAdsTickRate());
            if (adsIn != null && !adsIn.isEmpty()) {
                assertEquals(adsIn.get(0), getCmd(item), id + ": ADS entry frame 0 mismatch");
                List<Integer> observedIn = new ArrayList<>();
                for (int t = 0; t <= adsIn.size() * adsRate + 1; t++) {
                    if (t > 0 && t % 4 == 0) {
                        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                    }
                    observedIn.add(getCmd(item));
                    sharedScheduler.stepOneTick();
                }
                for (int expectedCmd : adsIn) {
                    assertTrue(observedIn.contains(expectedCmd), id + ": ads-in frame CMD " + expectedCmd + " never rendered");
                }
            }
            assertEquals(weapon.getAnimation().getAdsModelData(), getCmd(item), id + ": ADS CMD not applied after HAND+OFF_HAND");
            assertNotNull(getItemModel(item), id + ": ADS ItemModel null");

            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(adsManager.isAiming(player), id + ": Second Right-Click hold pulse must keep ADS active in holdToAds mode");

            int holdTicks = Math.max(4, (int) Math.ceil(plugin.getHoldAdsReleaseMs() / 50.0));
            List<Integer> adsOut = weapon.getAnimation().getAdsOutFrames();
            int outTicks = adsOut != null ? adsOut.size() * adsRate : 0;
            List<Integer> observedOut = new ArrayList<>();
            for (int t = 0; t <= holdTicks + outTicks + 2; t++) {
                observedOut.add(getCmd(item));
                sharedScheduler.stepOneTick();
            }
            assertFalse(adsManager.isAiming(player), id + ": Hold-ADS did not release after timeout");
            if (adsOut != null && !adsOut.isEmpty()) {
                for (int expectedCmd : adsOut) {
                    assertTrue(observedOut.contains(expectedCmd), id + ": ads-out frame CMD " + expectedCmd + " never rendered");
                }
            }
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": ADS release did not reset to idle CMD");

            setUpQuietly();
            plugin.setHoldToAds(false);
            try {
                item = equipWeaponAtSlot0(weapon);
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND);
                st = animationEngine.getState(playerState.uuid);
                assertTrue(adsManager.isAiming(player), id + ": Toggle-mode HAND+OFF_HAND interact toggled ADS ON->OFF");
                assertTrue(st.isAiming());
                assertEquals(WeaponState.AIMING_ADS, st.getCurrentState());
                if (adsIn != null && !adsIn.isEmpty()) {
                    sharedScheduler.advanceTicks(adsIn.size() * adsRate + 1);
                }
                assertEquals(weapon.getAnimation().getAdsModelData(), getCmd(item));

                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                assertFalse(adsManager.isAiming(player), id + ": Toggle-mode second Right-Click did not exit ADS");
                if (adsOut != null && !adsOut.isEmpty()) {
                    List<Integer> toggleObservedOut = new ArrayList<>();
                    for (int t = 0; t <= adsOut.size() * adsRate + 1; t++) {
                        toggleObservedOut.add(getCmd(item));
                        sharedScheduler.stepOneTick();
                    }
                    for (int expectedCmd : adsOut) {
                        assertTrue(toggleObservedOut.contains(expectedCmd), id + ": toggle ads-out frame CMD " + expectedCmd + " never rendered");
                    }
                }
                assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": Toggle ADS exit did not reset to idle CMD");
            } finally {
                plugin.setHoldToAds(true);
            }
        }
    }

    @Test
    void testRound4MultiFrameFireReloadAndMeleeSequencesAll17() {
        File itemsDir = new File("CookieCSG_DONE/Pack 1/ItemsAdder/contents/cookie_csg/resourcepack/assets/cookie_csg/items");
        File modelsDir = new File("CookieCSG_DONE/Pack 1/ItemsAdder/contents/cookie_csg/resourcepack/assets/cookie_csg/models/item");

        WeaponDefinition m4a1 = weaponManager.getWeapon("csgo_m4a1");
        WeaponDefinition m249 = weaponManager.getWeapon("csgo_m249");
        assertNotNull(m4a1);
        assertNotNull(m249);
        assertNotEquals(m4a1.getAnimation().getIdleModelData(), m249.getAnimation().getIdleModelData(), "csgo_m249 idle CMD must differ from csgo_m4a1");
        assertNotEquals(m4a1.getAnimation().getInspectFrames(), m249.getAnimation().getInspectFrames(), "csgo_m249 inspect CMDs must differ from csgo_m4a1");
        assertNotEquals(
            weaponManager.resolveItemModel(m4a1, m4a1.getAnimation().getIdleModelData()),
            weaponManager.resolveItemModel(m249, m249.getAnimation().getIdleModelData()),
            "csgo_m249 ItemModel must differ from csgo_m4a1"
        );
        File m4ModelFile = new File(modelsDir, "csgo_m4a1_body.json");
        File m249ModelFile = new File(modelsDir, "csgo_m249_body.json");
        assertTrue(m4ModelFile.exists() && m249ModelFile.exists(), "csgo_m4a1_body.json and csgo_m249_body.json must exist");
        assertNotEquals(m4ModelFile.length(), m249ModelFile.length(), "csgo_m249_body.json must not reuse csgo_m4a1_body.json geometry");

        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);

            List<Integer> deployFrames = weapon.getAnimation().getDeployFrames();
            assertTrue(deployFrames != null && deployFrames.size() >= 10, id + ": expected >= 10 deployFrames");
            assertTrue(weapon.getAnimation().getDeployTicks() >= 10, id + ": expected deployTicks >= 10");
            List<Integer> inspectFrames = weapon.getAnimation().getInspectFrames();
            assertTrue(inspectFrames != null && inspectFrames.size() >= 56, id + ": expected >= 56 inspectFrames");
            assertEquals(1, weapon.getAnimation().getInspectTickRate(), id + ": expected 20 FPS inspectTickRate=1");

            List<Integer> fireFrames = weapon.getAnimation().getFireFrames();
            assertNotNull(fireFrames, id + ": fireFrames null");
            boolean boltOrPump = id.equals("awp") || id.equals("kar98") || id.equals("m700") || id.equals("m870");
            int minFire = !weapon.getType().isGun() ? 7 : (boltOrPump ? 14 : 5);
            assertTrue(fireFrames.size() >= minFire, id + ": expected >= " + minFire + " fireFrames, got " + fireFrames.size());
            int fireRate = Math.max(1, weapon.getAnimation().getFireTickRate());

            Set<NamespacedKey> observedModels = new LinkedHashSet<>();
            Action fireAction = Action.LEFT_CLICK_AIR;
            fireInteract(fireAction, EquipmentSlot.HAND);
            List<Integer> observedFire = new ArrayList<>();
            for (int t = 0; t <= fireFrames.size() * fireRate + 2; t++) {
                observedFire.add(getCmd(item));
                if (getItemModel(item) != null) {
                    observedModels.add(getItemModel(item));
                }
                sharedScheduler.stepOneTick();
            }
            for (int expectedCmd : fireFrames) {
                assertTrue(observedFire.contains(expectedCmd), id + ": fire/slash frame CMD " + expectedCmd + " never rendered");
            }
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": failed to settle to IDLE after fire/slash sequence");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": failed to reset CMD to idle after fire/slash");

            if (weapon.getType().isGun() && weapon.getAdsConfig().isEnabled()) {
                assertTrue(weapon.getAnimation().getAdsInFrames().size() >= 4, id + ": expected >= 4 adsInFrames");
                assertTrue(weapon.getAnimation().getAdsFireFrames().size() >= 4, id + ": expected >= 4 adsFireFrames");
                assertTrue(weapon.getAnimation().getAdsOutFrames().size() >= 3, id + ": expected >= 3 adsOutFrames");

                boolean heavyReload = id.equals("awp") || id.equals("kar98") || id.equals("m700") || id.equals("p90") || id.equals("m870") || id.equals("csgo_m249");
                int minReload = heavyReload ? 60 : 44;
                boolean usesShotgunLoop = weapon.getType() == WeaponType.SHOTGUN
                    && weapon.getAnimation().getReloadLoopFrames() != null
                    && !weapon.getAnimation().getReloadLoopFrames().isEmpty();
                int fullReloadFrameCount = usesShotgunLoop
                    ? weapon.getAnimation().getReloadIntroFrames().size()
                        + (weapon.getStats().getClipSize() * weapon.getAnimation().getReloadLoopFrames().size())
                        + weapon.getAnimation().getReloadEndFrames().size()
                    : weapon.getAnimation().getReloadFrames().size();
                assertTrue(fullReloadFrameCount >= minReload, id + ": expected >= " + minReload + " reload frames, got " + fullReloadFrameCount);
                int fullReloadTicks = effectiveReloadDurationTicks(weapon, 0);
                double reloadFps = (fullReloadFrameCount * 20.0) / fullReloadTicks;
                assertTrue(reloadFps >= 15.0, id + ": reload FPS too low (" + reloadFps + " FPS)");

                int testAmmo = Math.max(0, weapon.getStats().getClipSize() - 1);
                int activeReloadTicks = effectiveReloadDurationTicks(weapon, testAmmo);
                int expectedDistinctFrames = effectiveDistinctReloadFrameCount(weapon, testAmmo);
                st.setAmmoInClip(testAmmo);
                fireShiftFReload();
                Set<Integer> observedReload = new LinkedHashSet<>();
                for (int t = 0; t <= activeReloadTicks + 2; t++) {
                    if (st.getCurrentState() == WeaponState.RELOADING) {
                        observedReload.add(getCmd(item));
                    }
                    if (getItemModel(item) != null) {
                        observedModels.add(getItemModel(item));
                    }
                    sharedScheduler.stepOneTick();
                }
                assertTrue(observedReload.size() >= Math.min(activeReloadTicks, expectedDistinctFrames), id + ": reload continuity broken, saw only " + observedReload.size() + " frames over " + activeReloadTicks + " ticks");
                assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": reload did not settle to IDLE");

                st.setLastFireTime(0L);
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                int adsRate = Math.max(1, weapon.getAnimation().getAdsTickRate());
                int inTicks = weapon.getAnimation().getAdsInFrames().size() * adsRate;
                for (int t = 0; t <= inTicks + 1; t++) {
                    if (t > 0 && t % 4 == 0) {
                        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                    }
                    if (getItemModel(item) != null) {
                        observedModels.add(getItemModel(item));
                    }
                    sharedScheduler.stepOneTick();
                }
                assertEquals(weapon.getAnimation().getAdsModelData(), getCmd(item), id + ": failed to settle on ADS pose before ADS fire");

                List<Integer> adsFireFrames = weapon.getAnimation().getAdsFireFrames();
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                List<Integer> observedAdsFire = new ArrayList<>();
                for (int t = 0; t <= adsFireFrames.size() * fireRate + 2; t++) {
                    if (t % 4 == 0) {
                        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                    }
                    observedAdsFire.add(getCmd(item));
                    if (getItemModel(item) != null) {
                        observedModels.add(getItemModel(item));
                    }
                    sharedScheduler.stepOneTick();
                }
                for (int expectedCmd : adsFireFrames) {
                    assertTrue(observedAdsFire.contains(expectedCmd), id + ": ads-fire frame CMD " + expectedCmd + " never rendered");
                }
                assertEquals(weapon.getAnimation().getAdsModelData(), getCmd(item), id + ": failed to return to ADS pose after ADS fire");
            } else if (!weapon.getType().isGun()) {
                List<Integer> slash2Frames = weapon.getAnimation().resolveSlash2Frames();
                List<Integer> heavyFrames = weapon.getAnimation().resolveHeavyFrames();
                assertTrue(slash2Frames.size() >= 4, id + ": expected >= 4 melee attack 2 frames");
                assertTrue(heavyFrames.size() >= 7, id + ": expected >= 7 melee heavy frames");

                st.setLastMeleeAttackMs(0L);
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                List<Integer> observedHeavy = new ArrayList<>();
                for (int t = 0; t <= heavyFrames.size() * fireRate + 2; t++) {
                    observedHeavy.add(getCmd(item));
                    if (getItemModel(item) != null) {
                        observedModels.add(getItemModel(item));
                    }
                    sharedScheduler.stepOneTick();
                }
                for (int expectedCmd : heavyFrames) {
                    assertTrue(observedHeavy.contains(expectedCmd), id + ": melee heavy frame CMD " + expectedCmd + " never rendered");
                }
                for (int attack2Cmd : slash2Frames) {
                    if (!heavyFrames.contains(attack2Cmd)) {
                        assertFalse(observedHeavy.contains(attack2Cmd), id + ": melee heavy concatenated Attack 2 frame CMD " + attack2Cmd);
                    }
                }
                assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": melee heavy did not return to IDLE");
                assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": melee heavy did not reset CMD to idle");
            }

            for (NamespacedKey modelKey : observedModels) {
                assertEquals("cookie_csg", modelKey.getNamespace(), id + ": unexpected ItemModel namespace " + modelKey);
                File itemDef = new File(itemsDir, modelKey.getKey() + ".json");
                assertTrue(itemDef.exists(), id + ": missing 1.21.4+ dispatch file: " + itemDef.getPath());
            }
        }
    }

    @Test
    void testReloadFullClipWorksAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun()) {
                continue;
            }
            int clipSize = weapon.getStats().getClipSize();
            int reloadTicks = effectiveReloadDurationTicks(weapon, clipSize);
            int firstFrame = effectiveFirstReloadFrame(weapon, clipSize);
            assertTrue(firstFrame > 0, id + ": missing first reload frame");

            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            st.setAmmoInClip(clipSize);
            NamespacedKey idleModel = getItemModel(item);

            fireShiftFReload();
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": pressing Shift+F on FULL clip (" + clipSize + "/" + clipSize + ") did nothing");
            Set<Integer> fullReloadCmds = new HashSet<>();
            for (int t = 0; t <= reloadTicks + 2; t++) {
                fullReloadCmds.add(getCmd(item));
                sharedScheduler.stepOneTick();
            }
            assertTrue(fullReloadCmds.contains(firstFrame), id + ": full-clip reload never displayed reload frames");
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": full-clip reload did not return to IDLE");
            assertEquals(clipSize, st.getAmmoInClip(), id + ": full-clip reload corrupted clip ammo");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": full-clip reload did not reset CMD to idle");
            assertNotNull(idleModel, id + ": idle ItemModel missing");
            assertEquals(idleModel, getItemModel(item), id + ": full-clip reload did not reset ItemModel to idle");
        }
    }

    @Test
    void testReloadPartialAndEmptyClipAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun()) {
                continue;
            }
            int clipSize = weapon.getStats().getClipSize();
            int partialAmmo = Math.max(1, clipSize / 2);
            int partialTicks = effectiveReloadDurationTicks(weapon, partialAmmo);
            int emptyTicks = effectiveReloadDurationTicks(weapon, 0);

            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            st.setAmmoInClip(partialAmmo);
            fireShiftFReload();
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": partial-clip Shift+F failed to start reload");
            sharedScheduler.advanceTicks(partialTicks + 2);
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": partial-clip reload did not finish");
            assertEquals(clipSize, st.getAmmoInClip(), id + ": partial-clip reload did not refill clip");

            setUpQuietly();
            item = equipWeaponAtSlot0(weapon);
            st = animationEngine.getState(playerState.uuid);
            st.setAmmoInClip(0);
            fireShiftFReload();
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": empty-clip Shift+F failed to start reload");
            sharedScheduler.advanceTicks(emptyTicks + 2);
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": empty-clip reload did not finish");
            assertEquals(clipSize, st.getAmmoInClip(), id + ": empty-clip reload did not refill clip");
        }
    }

    @Test
    void testReloadMidInterruptNoOrphanSoundOrAmmoMutationAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun()) {
                continue;
            }
            int clipSize = weapon.getStats().getClipSize();
            int partialAmmo = Math.max(1, clipSize / 2);
            int reloadTicks = effectiveReloadDurationTicks(weapon, partialAmmo);

            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            st.setAmmoInClip(partialAmmo);
            NamespacedKey idleModel = getItemModel(item);

            fireShiftFReload();
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": interrupt test failed to enter RELOADING");
            sharedScheduler.advanceTicks(3);

            switchHotbarSlot(0, 1);
            int soundsAtCancel = playerState.playedSounds.size();
            int actionBarsAtCancel = playerState.actionBars.size();
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": hotbar cancel mid-reload did not reset state");
            assertEquals(weapon.getAnimation().getIdleModelData(), getCmd(item), id + ": hotbar cancel mid-reload did not reset CMD");
            if (idleModel != null) {
                assertEquals(idleModel, getItemModel(item), id + ": hotbar cancel mid-reload did not reset ItemModel");
            }

            sharedScheduler.advanceTicks(reloadTicks + 10);
            assertEquals(soundsAtCancel, playerState.playedSounds.size(), id + ": orphan reload sound task fired after hotbar cancel: " + playerState.playedSounds.subList(soundsAtCancel, playerState.playedSounds.size()));
            assertEquals(actionBarsAtCancel, playerState.actionBars.size(), id + ": orphan reload completion actionBar fired after hotbar cancel");
            assertEquals(partialAmmo, st.getAmmoInClip(), id + ": orphan reload completion mutated ammo after hotbar cancel");

            setUpQuietly();
            item = equipWeaponAtSlot0(weapon);
            st = animationEngine.getState(playerState.uuid);
            st.setAmmoInClip(partialAmmo);
            fireShiftFReload();
            sharedScheduler.advanceTicks(5);
            switchHotbarSlot(0, 1);
            switchHotbarSlot(1, 0);
            int deployTicks = weapon.getAnimation().getDeployTicks() > 0 ? weapon.getAnimation().getDeployTicks() : 6;
            sharedScheduler.advanceTicks(deployTicks + 1);
            fireShiftFReload();
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": second reload failed to start");
            boolean usesShotgunLoop = weapon.getType() == WeaponType.SHOTGUN
                && weapon.getAnimation().getReloadLoopFrames() != null
                && !weapon.getAnimation().getReloadLoopFrames().isEmpty();
            sharedScheduler.advanceTicks(Math.max(1, reloadTicks - 8));
            assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": orphan reload #1 timer prematurely ended reload #2");
            if (!usesShotgunLoop) {
                assertEquals(partialAmmo, st.getAmmoInClip(), id + ": orphan reload #1 timer prematurely refilled ammo during reload #2");
            } else {
                assertTrue(st.getAmmoInClip() >= partialAmmo && st.getAmmoInClip() <= clipSize, id + ": shotgun ammo out of bounds during reload #2");
            }
        }
    }

    @Test
    void testLinearTacticalVsEmptyReloadAndCustomAudioTicks(@TempDir File tempSimDir) throws Exception {
        WeaponManager simManager = new WeaponManager(tempSimDir);
        setField(plugin, "weaponManager", simManager);
        weaponManager = simManager;

        WeaponDefinition rifle = simManager.createWeapon("sim_tactical_rifle", WeaponType.RIFLE);
        rifle.getStats().setClipSize(30);
        rifle.getStats().setReloadTicks(20);
        rifle.getStats().setReloadTacticalTicks(12);
        rifle.getAnimation().setIdleModelData(5000);
        rifle.getAnimation().setDeployTicks(4);
        rifle.getAnimation().setDeployFrames(List.of(5001, 5002));
        rifle.getAnimation().setReloadTickRate(2);
        rifle.getAnimation().setReloadFrames(List.of(6001, 6002, 6003, 6004, 6005, 6006, 6007, 6008, 6009, 6010));
        rifle.getAnimation().setReloadTacticalFrames(List.of(7001, 7002, 7003, 7004, 7005, 7006));
        rifle.getAudio().setReloadClipOut("csg:sim_mag_out");
        rifle.getAudio().setReloadClipIn("csg:sim_clip_in");
        rifle.getAudio().setReloadBolt("csg:sim_bolt");
        rifle.getAudio().setReloadClipInTick(5);
        rifle.getAudio().setReloadBoltTick(14);
        simManager.saveWeapon(rifle);

        ItemStack item = equipWeaponAtSlot0(rifle);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        st.setAmmoInClip(10);
        playerState.playedSounds.clear();

        fireShiftFReload();
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        assertEquals(7001, getCmd(item));
        sharedScheduler.stepOneTick();
        assertEquals(7001, getCmd(item));
        sharedScheduler.stepOneTick();
        assertEquals(7001, getCmd(item));
        sharedScheduler.stepOneTick();
        assertEquals(7002, getCmd(item));
        sharedScheduler.advanceTicks(3);
        assertTrue(playerState.playedSounds.contains("csg:sim_clip_in"));
        sharedScheduler.advanceTicks(8);
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(30, st.getAmmoInClip());
        assertEquals(rifle.getAnimation().getIdleModelData(), getCmd(item));
        assertFalse(playerState.playedSounds.contains("csg:sim_bolt"));

        playerState.playedSounds.clear();
        st.setAmmoInClip(0);
        fireShiftFReload();
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        assertEquals(6001, getCmd(item));
        sharedScheduler.advanceTicks(3);
        assertEquals(6002, getCmd(item));
        sharedScheduler.advanceTicks(12);
        assertTrue(playerState.playedSounds.contains("csg:sim_clip_in"));
        assertTrue(playerState.playedSounds.contains("csg:sim_bolt"));
        sharedScheduler.advanceTicks(7);
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(30, st.getAmmoInClip());
    }

    @Test
    void testShotgunPerShellLoopReloadAndFireInterrupt(@TempDir File tempSimDir) throws Exception {
        WeaponManager simManager = new WeaponManager(tempSimDir);
        setField(plugin, "weaponManager", simManager);
        weaponManager = simManager;

        WeaponDefinition sg = simManager.createWeapon("sim_shotgun", WeaponType.SHOTGUN);
        sg.getStats().setClipSize(6);
        sg.getStats().setFireRateTicks(4);
        sg.getAnimation().setIdleModelData(8000);
        sg.getAnimation().setDeployTicks(4);
        sg.getAnimation().setDeployFrames(List.of(8001, 8002));
        sg.getAnimation().setFireFrames(List.of(8011, 8012, 8013));
        sg.getAnimation().setReloadTickRate(1);
        sg.getAnimation().setReloadIntroFrames(List.of(8101, 8102, 8103, 8104));
        sg.getAnimation().setReloadIntroTicks(4);
        sg.getAnimation().setReloadLoopFrames(List.of(8201, 8202, 8203, 8204, 8205));
        sg.getAnimation().setReloadLoopTicks(5);
        sg.getAnimation().setReloadEndFrames(List.of(8301, 8302, 8303, 8304));
        sg.getAnimation().setReloadEndTicks(4);
        sg.getAudio().setReloadClipOut("csg:sg_start");
        sg.getAudio().setReloadClipIn("csg:sg_shell");
        sg.getAudio().setReloadBolt("csg:sg_pump");
        simManager.saveWeapon(sg);

        ItemStack item = equipWeaponAtSlot0(sg);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        st.setAmmoInClip(3);
        playerState.playedSounds.clear();

        fireShiftFReload();
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        assertEquals(8101, getCmd(item));
        sharedScheduler.advanceTicks(5);
        assertEquals(3, st.getAmmoInClip());
        assertEquals(8201, getCmd(item));

        sharedScheduler.advanceTicks(5);
        assertEquals(4, st.getAmmoInClip());
        assertEquals(1, playerState.playedSounds.stream().filter("csg:sg_shell"::equals).count());

        sharedScheduler.advanceTicks(5);
        assertEquals(5, st.getAmmoInClip());
        assertEquals(2, playerState.playedSounds.stream().filter("csg:sg_shell"::equals).count());

        sharedScheduler.advanceTicks(5);
        assertEquals(6, st.getAmmoInClip());
        assertEquals(3, playerState.playedSounds.stream().filter("csg:sg_shell"::equals).count());
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        assertEquals(8301, getCmd(item));

        sharedScheduler.advanceTicks(4);
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(1, playerState.playedSounds.stream().filter("csg:sg_pump"::equals).count());
        assertEquals(sg.getAnimation().getIdleModelData(), getCmd(item));

        playerState.playedSounds.clear();
        st.setAmmoInClip(0);
        fireShiftFReload();
        sharedScheduler.advanceTicks(3);
        assertEquals(0, st.getAmmoInClip());
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(WeaponState.RELOADING, st.getCurrentState());

        sharedScheduler.advanceTicks(7);
        assertEquals(1, st.getAmmoInClip());
        assertEquals(WeaponState.RELOADING, st.getCurrentState());

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(WeaponState.FIRING, st.getCurrentState());
        assertEquals(0, st.getAmmoInClip());
        int soundsAtInterrupt = playerState.playedSounds.size();
        sharedScheduler.advanceTicks(60);
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(0, st.getAmmoInClip());
        assertEquals(0, playerState.playedSounds.subList(soundsAtInterrupt, playerState.playedSounds.size()).stream().filter(s -> s.equals("csg:sg_shell") || s.equals("csg:sg_pump")).count());

        st.setAmmoInClip(6);
        fireShiftFReload();
        sharedScheduler.advanceTicks(15);
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(6, st.getAmmoInClip());
    }

    @Test
    void testNoAttackCooldownModifierPresentAll17() {
        NamespacedKey expectedKey = new NamespacedKey("cookiecsg", "no_attack_cooldown");
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            ItemStack csgItem = weaponManager.buildItemStack(weapon);
            assertAttackSpeedFixed(csgItem, expectedKey, id + " (/csg give)");

            ItemStack iaItem = buildFakeItemsAdderItem(weapon);
            playerState.inventorySlots[0] = iaItem;
            playerState.inventorySlots[1] = new FakeItemStack(Material.AIR, 0);
            playerState.heldSlot = 1;
            switchHotbarSlot(1, 0);
            assertAttackSpeedFixed(playerState.inventorySlots[0], expectedKey, id + " (/ia equip)");
        }
    }

    private static void assertAttackSpeedFixed(ItemStack item, NamespacedKey expectedKey, String label) {
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta, label + ": missing meta");
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES), label + ": missing HIDE_ATTRIBUTES");
        Collection<AttributeModifier> mods = meta.getAttributeModifiers(Attribute.ATTACK_SPEED);
        assertNotNull(mods, label + ": ATTACK_SPEED modifiers null");
        AttributeModifier match = mods.stream()
            .filter(m -> expectedKey.equals(m.getKey()))
            .findFirst()
            .orElse(null);
        assertNotNull(match, label + ": missing cookiecsg:no_attack_cooldown modifier");
        assertEquals(1024.0, match.getAmount(), 0.0001, label + ": wrong modifier amount");
        assertEquals(EquipmentSlotGroup.MAINHAND, match.getSlotGroup(), label + ": wrong slot group");

        Collection<AttributeModifier> breakMods = meta.getAttributeModifiers(Attribute.BLOCK_BREAK_SPEED);
        assertNotNull(breakMods, label + ": BLOCK_BREAK_SPEED modifiers null");
        NamespacedKey noBreakKey = new NamespacedKey("cookiecsg", "no_block_break");
        AttributeModifier breakMatch = breakMods.stream()
            .filter(m -> noBreakKey.equals(m.getKey()))
            .findFirst()
            .orElse(null);
        assertNotNull(breakMatch, label + ": missing cookiecsg:no_block_break modifier");
        assertEquals(-1.0, breakMatch.getAmount(), 0.0001, label + ": wrong BLOCK_BREAK_SPEED amount");
        assertEquals(AttributeModifier.Operation.MULTIPLY_SCALAR_1, breakMatch.getOperation(), label + ": wrong BLOCK_BREAK_SPEED operation");
        assertEquals(EquipmentSlotGroup.MAINHAND, breakMatch.getSlotGroup(), label + ": wrong BLOCK_BREAK_SPEED slot group");
    }

    @Test
    void testGunControlsDefaultAndLegacyToggle() {
        YamlConfiguration configYaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        assertFalse(configYaml.getBoolean("gun-controls.right-click-fire", true), "config.yml default gun-controls.right-click-fire must be false");
        assertTrue(configYaml.getBoolean("gun-controls.hold-to-ads", false), "config.yml default gun-controls.hold-to-ads must be true");
        assertEquals(350L, configYaml.getLong("gun-controls.hold-ads-release-ms", 0L), "config.yml default gun-controls.hold-ads-release-ms must be 350");
        assertFalse(plugin.isRightClickFireForGuns(), "CookieCSGPlugin default isRightClickFireForGuns must be false");
        assertTrue(plugin.isHoldToAds(), "CookieCSGPlugin default isHoldToAds must be true");

        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        ItemStack item = equipWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int startAmmo = st.getAmmoInClip();

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(startAmmo - 1, st.getAmmoInClip());
        assertEquals(WeaponState.FIRING, st.getCurrentState());

        sharedScheduler.advanceTicks(20);
        assertEquals(WeaponState.IDLE, st.getCurrentState());

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player));
        assertTrue(st.isAiming());
        assertFalse(playerState.moveSpeedMods.isEmpty());
        assertTrue(((playerState.moveSpeedAttr.getValue() / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0 < 0.99);
        assertEquals(WeaponState.AIMING_ADS, st.getCurrentState());

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player), "Hold-to-ADS must remain aiming on RMB pulse");

        int holdTicks = Math.max(4, (int) Math.ceil(plugin.getHoldAdsReleaseMs() / 50.0));
        sharedScheduler.advanceTicks(holdTicks + 2);
        assertFalse(adsManager.isAiming(player));
        assertFalse(st.isAiming());
        assertTrue(playerState.moveSpeedMods.isEmpty());
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);
        sharedScheduler.advanceTicks(10);
        assertEquals(WeaponState.IDLE, st.getCurrentState());

        plugin.setHoldToAds(false);
        try {
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(adsManager.isAiming(player));
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertFalse(adsManager.isAiming(player));
            assertFalse(st.isAiming());
            assertTrue(playerState.moveSpeedMods.isEmpty());
            assertEquals(0.2f, playerState.walkSpeed, 1e-4f);
            sharedScheduler.advanceTicks(10);
            assertEquals(WeaponState.IDLE, st.getCurrentState());
        } finally {
            plugin.setHoldToAds(true);
        }

        fireSwapHand();
        assertEquals(WeaponState.INSPECTING, st.getCurrentState());
        fireSwapHand();
        assertEquals(WeaponState.IDLE, st.getCurrentState());

        st.setAmmoInClip(startAmmo - 5);
        fireShiftFReload();
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        sharedScheduler.advanceTicks(effectiveReloadDurationTicks(ak, startAmmo - 5) + 2);
        assertEquals(startAmmo, st.getAmmoInClip());
        assertEquals(WeaponState.IDLE, st.getCurrentState());

        plugin.setRightClickFireForGuns(true);
        try {
            assertTrue(plugin.isRightClickFireForGuns());
            st.setLastFireTime(0L);
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(startAmmo - 1, st.getAmmoInClip());
            assertEquals(WeaponState.FIRING, st.getCurrentState());
            sharedScheduler.advanceTicks(20);

            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(adsManager.isAiming(player));
            assertEquals(WeaponState.AIMING_ADS, st.getCurrentState());
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertFalse(adsManager.isAiming(player));
        } finally {
            plugin.setRightClickFireForGuns(false);
        }
    }

    @Test
    void testDropQAllowsNormalItemDropAndCleansUpState() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        ItemStack item = equipWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player));

        playerState.inventorySlots[0] = new FakeItemStack(Material.AIR, 0);
        Item adsDropEntity = (Item) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{Item.class},
            new java.lang.reflect.InvocationHandler() {
                private ItemStack stack = item;

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    if ("getItemStack".equals(method.getName())) {
                        return stack;
                    }
                    if ("setItemStack".equals(method.getName())) {
                        stack = (ItemStack) args[0];
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
        PlayerDropItemEvent adsDropEvent = new PlayerDropItemEvent(player, adsDropEntity);
        listener.onDropItem(adsDropEvent);
        assertFalse(adsDropEvent.isCancelled());
        assertFalse(adsManager.isAiming(player));
        assertNull(playerState.potionEffects.get(PotionEffectType.SLOWNESS));
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(ak.getAnimation().getIdleModelData(), getCmd(adsDropEntity.getItemStack()));

        ItemStack reequipped = equipWeaponAtSlot0(ak);
        st = animationEngine.getState(playerState.uuid);
        st.setAmmoInClip(10);
        fireShiftFReload();
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        sharedScheduler.advanceTicks(2);

        playerState.inventorySlots[0] = new FakeItemStack(Material.AIR, 0);
        Item reloadDropEntity = (Item) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{Item.class},
            new java.lang.reflect.InvocationHandler() {
                private ItemStack stack = reequipped;

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    if ("getItemStack".equals(method.getName())) {
                        return stack;
                    }
                    if ("setItemStack".equals(method.getName())) {
                        stack = (ItemStack) args[0];
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
        PlayerDropItemEvent reloadDropEvent = new PlayerDropItemEvent(player, reloadDropEntity);
        listener.onDropItem(reloadDropEvent);
        assertFalse(reloadDropEvent.isCancelled());
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(ak.getAnimation().getIdleModelData(), getCmd(reloadDropEntity.getItemStack()));

        sharedScheduler.advanceTicks(effectiveReloadDurationTicks(ak, 10) + 5);
        assertEquals(10, st.getAmmoInClip());
    }

    @Test
    void testRightClickHoldAutoFillSpray() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        equipWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int clip = st.getAmmoInClip();

        plugin.setRightClickFireForGuns(true);
        try {
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(clip - 1, st.getAmmoInClip());
            BukkitTask runner = st.getAutoFireTask();
            assertNotNull(runner, "AUTO click must start continuous auto-fire runner");
            assertFalse(runner.isCancelled());

            for (int t = 0; t < 10; t++) {
                if (t > 0 && t % 4 == 0) {
                    fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                    assertSame(runner, st.getAutoFireTask(), "Held right-click pulse must preserve existing auto-fire runner phase");
                }
                st.setLastFireTime(System.currentTimeMillis() - 150L);
                sharedScheduler.stepOneTick();
            }
            int ammoAfterHold = st.getAmmoInClip();
            assertTrue(ammoAfterHold <= clip - 5, "Held right-click must maintain continuous auto spray, ammo was " + ammoAfterHold);

            int step = Math.max(1, ak.getStats().getFireRateTicks());
            int windowTicks = Math.max(6, step + 4);
            for (int t = 0; t <= windowTicks + 2; t++) {
                st.setLastFireTime(System.currentTimeMillis() - 150L);
                sharedScheduler.stepOneTick();
            }
            assertNull(st.getAutoFireTask(), "Releasing right-click must cancel auto-fire runner after hold window");
            int ammoAfterStop = st.getAmmoInClip();
            sharedScheduler.advanceTicks(10);
            assertEquals(ammoAfterStop, st.getAmmoInClip(), "No shots may fire after auto-fire window ends");
        } finally {
            plugin.setRightClickFireForGuns(false);
        }
    }

    @Test
    void testPerWeaponClipAmmoIsolatedAcrossHotbarSwitches() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        WeaponDefinition deagle = weaponManager.getWeapon("deagle");
        WeaponDefinition awp = weaponManager.getWeapon("awp");
        assertNotNull(ak);
        assertNotNull(deagle);
        assertNotNull(awp);

        playerState.inventorySlots[0] = weaponManager.buildItemStack(ak);
        playerState.inventorySlots[1] = weaponManager.buildItemStack(deagle);
        playerState.inventorySlots[2] = weaponManager.buildItemStack(awp);
        playerState.inventorySlots[3] = new FakeItemStack(Material.AIR, 0);
        playerState.heldSlot = 3;

        switchHotbarSlot(3, 0);
        sharedScheduler.advanceTicks(Math.max(6, ak.getAnimation().getDeployTicks()) + 1);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int akClip = ak.getStats().getClipSize();
        assertEquals(akClip, st.getAmmoInClip());

        st.setLastRightClickMs(0L);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        st.setLastRightClickMs(0L);
        st.setLastFireTime(0L);
        sharedScheduler.advanceTicks(ak.getStats().getFireRateTicks() + 1);
        assertEquals(akClip - 2, st.getAmmoInClip());

        switchHotbarSlot(0, 1);
        sharedScheduler.advanceTicks(Math.max(6, deagle.getAnimation().getDeployTicks()) + 1);
        int deagleClip = deagle.getStats().getClipSize();
        assertEquals(deagleClip, st.getAmmoInClip(), "Switching from ak47 to deagle must use deagle clip size, not ak47 remaining ammo");
        st.setLastRightClickMs(0L);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(deagleClip - 1, st.getAmmoInClip());

        switchHotbarSlot(1, 2);
        sharedScheduler.advanceTicks(Math.max(6, awp.getAnimation().getDeployTicks()) + 1);
        int awpClip = awp.getStats().getClipSize();
        assertEquals(awpClip, st.getAmmoInClip(), "Switching to awp must initialize awp clip size");
        st.setLastRightClickMs(0L);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(awpClip - 1, st.getAmmoInClip());

        switchHotbarSlot(2, 0);
        sharedScheduler.advanceTicks(Math.max(6, ak.getAnimation().getDeployTicks()) + 1);
        assertEquals(akClip - 2, st.getAmmoInClip(), "Switching back to ak47 must restore ak47 remaining ammo");

        switchHotbarSlot(0, 2);
        sharedScheduler.advanceTicks(Math.max(6, awp.getAnimation().getDeployTicks()) + 1);
        st.setAmmoInClip(0);
        switchHotbarSlot(2, 0);
        switchHotbarSlot(0, 2);
        sharedScheduler.advanceTicks(Math.max(6, awp.getAnimation().getDeployTicks()) + 1);
        assertEquals(0, st.getAmmoInClip(), "Empty weapon clip must stay 0 across hotbar switch instead of auto-refilling");
    }

    @Test
    void testEntityDamageAndInteractEntitySingleTickDedup() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        equipWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int startAmmo = st.getAmmoInClip();
        LivingEntity dummyVictim = createDummyLivingEntity();

        EntityDamageByEntityEvent damageEvent = fireDamageEntity(dummyVictim);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);

        assertTrue(damageEvent.isCancelled());
        assertEquals(startAmmo - 1, st.getAmmoInClip(), "EntityDamageByEntityEvent + Left-Click interact in same tick must fire gun once, not twice");
        assertFalse(adsManager.isAiming(player));

        PlayerInteractEntityEvent interactEntityEvent = fireInteractEntity(dummyVictim, EquipmentSlot.HAND);
        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);

        assertTrue(interactEntityEvent.isCancelled());
        assertTrue(adsManager.isAiming(player), "PlayerInteractEntityEvent + Right-Click interact in same tick must enter ADS once");

        adsManager.clearAimingSilently(player);
        st.setLastInteractEntityMs(0L);
        plugin.setHoldToAds(false);
        try {
            PlayerInteractEntityEvent toggleInteractEntity = fireInteractEntity(dummyVictim, EquipmentSlot.HAND);
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(toggleInteractEntity.isCancelled());
            assertTrue(adsManager.isAiming(player), "Toggle-mode PlayerInteractEntityEvent + Right-Click interact in same tick must toggle ADS ON once, not ON->OFF");
        } finally {
            plugin.setHoldToAds(true);
        }
    }

    @Test
    void testAdsFireNonCancellationAndRapidToggleAllGuns() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            if (!weapon.getType().isGun() || !weapon.getAdsConfig().isEnabled()) {
                continue;
            }

            setUpQuietly();
            plugin.setHoldToAds(false);
            try {
                ItemStack item = equipTrackedWeaponAtSlot0(weapon);
                PlayerWeaponState st = animationEngine.getState(playerState.uuid);
                int fireRate = Math.max(1, weapon.getAnimation().getFireTickRate());
                int adsRate = Math.max(1, weapon.getAnimation().getAdsTickRate());
                List<Integer> adsIn = weapon.getAnimation().getAdsInFrames();
                List<Integer> adsFire = weapon.getAnimation().getAdsFireFrames();
                List<Integer> adsOut = weapon.getAnimation().getAdsOutFrames();
                List<Integer> fireFrames = weapon.getAnimation().getFireFrames();
                int adsModel = weapon.getAnimation().getAdsModelData();
                int idleModel = weapon.getAnimation().getIdleModelData();

                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                sharedScheduler.advanceTicks(adsIn.size() * adsRate + 2);
                assertTrue(adsManager.isAiming(player), id + ": ADS must be active");
                assertTrue(st.isAiming(), id + ": state.isAiming() must be true");
                assertFalse(playerState.moveSpeedMods.isEmpty(), id + ": Zoom modifier missing");
                assertTrue(((playerState.moveSpeedAttr.getValue() / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0 < 0.99, id + ": Zoom FOV reduction missing");
                assertEquals(WeaponState.AIMING_ADS, st.getCurrentState(), id + ": expected AIMING_ADS");
                assertEquals(adsModel, getCmd(item), id + ": expected adsModelData");

                int logStart = getCmdWriteLog(item).size();
                st.setLastFireTime(0L);
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                assertTrue(adsManager.isAiming(player), id + ": firing while aiming must NOT cancel AdsManager aiming");
                assertTrue(st.isAiming(), id + ": firing while aiming must NOT cancel state.isAiming()");
                assertFalse(playerState.moveSpeedMods.isEmpty(), id + ": firing while aiming must NOT remove zoom modifier");
                assertEquals(WeaponState.FIRING, st.getCurrentState(), id + ": expected FIRING state");
                List<Integer> observedAdsFire = new ArrayList<>();
                for (int t = 0; t <= adsFire.size() * fireRate + 2; t++) {
                    observedAdsFire.add(getCmd(item));
                    sharedScheduler.stepOneTick();
                }
                for (int expectedCmd : adsFire) {
                    assertTrue(observedAdsFire.contains(expectedCmd), id + ": missing adsFire frame " + expectedCmd);
                }
                List<Integer> adsFireWrites = getCmdWriteLog(item).subList(logStart, getCmdWriteLog(item).size());
                assertFalse(adsFireWrites.isEmpty(), id + ": cmdWriteLog must capture ADS fire frame writes");
                assertTrue(adsFireWrites.contains(adsModel), id + ": ADS fire sequence must settle back to adsModelData");
                assertFalse(adsFireWrites.contains(idleModel), id + ": ADS firing must NEVER revert to idleModelData");
                assertTrue(adsManager.isAiming(player), id + ": ADS must stay active after adsFire finishes");
                assertTrue(st.isAiming(), id + ": state.isAiming() must stay true after adsFire finishes");
                assertEquals(WeaponState.AIMING_ADS, st.getCurrentState(), id + ": failed to settle to AIMING_ADS");
                assertEquals(adsModel, getCmd(item), id + ": failed to settle to adsModelData");

                setUpQuietly();
                plugin.setHoldToAds(false);
                item = equipTrackedWeaponAtSlot0(weapon);
                st = animationEngine.getState(playerState.uuid);
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                sharedScheduler.stepOneTick();
                assertTrue(adsManager.isAiming(player), id + ": expected aiming during adsInFrames");
                st.setLastFireTime(0L);
                int midAdsInLog = getCmdWriteLog(item).size();
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                assertTrue(adsManager.isAiming(player), id + ": firing during adsInFrames cancelled ADS");
                assertTrue(st.isAiming(), id + ": firing during adsInFrames cleared state.isAiming()");
                assertFalse(playerState.moveSpeedMods.isEmpty(), id + ": firing during adsInFrames removed zoom modifier");
                assertEquals(adsFire.get(0), getCmd(item), id + ": firing during adsInFrames did not start adsFire frame 0");
                sharedScheduler.advanceTicks(Math.max(adsIn.size() * adsRate, adsFire.size() * fireRate) + 3);
                assertTrue(adsManager.isAiming(player), id + ": ADS dropped after firing during adsInFrames");
                assertEquals(WeaponState.AIMING_ADS, st.getCurrentState(), id + ": failed to settle to AIMING_ADS after firing during adsInFrames");
                assertEquals(adsModel, getCmd(item), id + ": stale adsInFrames or idle overwrote adsModelData");
                List<Integer> midInWrites = getCmdWriteLog(item).subList(midAdsInLog, getCmdWriteLog(item).size());
                assertFalse(midInWrites.isEmpty(), id + ": midInWrites must not be empty");
                assertFalse(midInWrites.contains(idleModel), id + ": firing during adsInFrames flashed idleModelData");

                setUpQuietly();
                plugin.setHoldToAds(false);
                item = equipTrackedWeaponAtSlot0(weapon);
                st = animationEngine.getState(playerState.uuid);
                st.setLastFireTime(0L);
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                assertEquals(WeaponState.FIRING, st.getCurrentState());
                sharedScheduler.stepOneTick();
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                assertTrue(adsManager.isAiming(player), id + ": right-click ADS while FIRING failed to activate ADS");
                assertTrue(st.isAiming(), id + ": right-click ADS while FIRING failed to set state.isAiming()");
                assertEquals(adsIn.get(0), getCmd(item), id + ": right-click ADS while FIRING did not start adsIn frame 0");
                sharedScheduler.advanceTicks(Math.max(fireFrames.size() * fireRate, adsIn.size() * adsRate) + 4);
                assertTrue(adsManager.isAiming(player), id + ": ADS was overwritten after firing->ADS toggle");
                assertEquals(WeaponState.AIMING_ADS, st.getCurrentState(), id + ": stale fire task overwrote AIMING_ADS state");
                assertEquals(adsModel, getCmd(item), id + ": stale fire task overwrote adsModelData");

                st.setLastFireTime(0L);
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                assertEquals(WeaponState.FIRING, st.getCurrentState());
                sharedScheduler.stepOneTick();
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
                assertFalse(adsManager.isAiming(player), id + ": right-click ADS OFF while ADS-FIRING did not cancel ADS");
                assertFalse(st.isAiming(), id + ": right-click ADS OFF while ADS-FIRING left state.isAiming() true");
                assertTrue(playerState.moveSpeedMods.isEmpty(), id + ": Zoom modifier not removed");
                assertEquals(0.2f, playerState.walkSpeed, 1e-4f, id + ": walkSpeed not restored");
                assertEquals(adsOut.get(0), getCmd(item), id + ": right-click ADS OFF did not start adsOut frame 0");
                sharedScheduler.advanceTicks(Math.max(adsFire.size() * fireRate, adsOut.size() * adsRate) + 4);
                assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": stale adsFire task overwrote IDLE state");
                assertEquals(idleModel, getCmd(item), id + ": stale adsFire task overwrote idleModelData");
            } finally {
                plugin.setHoldToAds(true);
            }
        }
    }

    @Test
    void testMeleeThreeAttackControlsAndComboAllMelee() {
        List<String> meleeIds = List.of("combat_knife", "tactical_axe", "csgo_dark_star");
        for (String id : meleeIds) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            assertNotNull(weapon, "Missing melee weapon: " + id);

            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);
            int fireRate = Math.max(1, weapon.getAnimation().getFireTickRate());
            long cooldownMs = Math.max(MeleeEngine.MIN_MELEE_COOLDOWN_MS, Math.max(1, weapon.getStats().getFireRateTicks()) * 50L);
            List<Integer> attack1 = weapon.getAnimation().getFireFrames();
            List<Integer> attack2 = weapon.getAnimation().resolveSlash2Frames();
            List<Integer> attack3 = weapon.getAnimation().resolveHeavyFrames();
            int idleModel = weapon.getAnimation().getIdleModelData();

            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(1, st.getMeleeComboStep(), id + ": Attack 1 must advance combo step to 1");
            assertEquals(attack1.get(0), getCmd(item), id + ": Left Click #1 must start Attack 1 frame 0");
            List<Integer> observedA1 = collectCmdsOverTicks(item, attack1.size() * fireRate + 2);
            for (int cmd : attack1) {
                assertTrue(observedA1.contains(cmd), id + ": Attack 1 missing frame " + cmd);
            }
            for (int cmd : attack2) {
                if (!attack1.contains(cmd)) {
                    assertFalse(observedA1.contains(cmd), id + ": Attack 1 rendered Attack 2 frame " + cmd);
                }
            }
            assertEquals(WeaponState.IDLE, st.getCurrentState());
            assertEquals(idleModel, getCmd(item));

            st.setLastMeleeAttackMs(System.currentTimeMillis() - (cooldownMs + 50L));
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(0, st.getMeleeComboStep(), id + ": Attack 2 must alternate combo step back to 0");
            assertEquals(attack2.get(0), getCmd(item), id + ": Left Click #2 must start Attack 2 frame 0");
            List<Integer> observedA2 = collectCmdsOverTicks(item, attack2.size() * fireRate + 2);
            for (int cmd : attack2) {
                assertTrue(observedA2.contains(cmd), id + ": Attack 2 missing frame " + cmd);
            }
            for (int cmd : attack3) {
                if (!attack2.contains(cmd)) {
                    assertFalse(observedA2.contains(cmd), id + ": Attack 2 rendered Attack 3 frame " + cmd);
                }
            }
            assertEquals(WeaponState.IDLE, st.getCurrentState());
            assertEquals(idleModel, getCmd(item));

            st.setLastMeleeAttackMs(System.currentTimeMillis() - (cooldownMs + 50L));
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(1, st.getMeleeComboStep(), id + ": Left Click #3 must alternate back to Attack 1");
            assertEquals(attack1.get(0), getCmd(item), id + ": Left Click #3 did not play Attack 1 frame 0");
            sharedScheduler.advanceTicks(attack1.size() * fireRate + 2);
            assertEquals(idleModel, getCmd(item));

            st.setLastMeleeAttackMs(System.currentTimeMillis() - (MeleeEngine.MELEE_COMBO_WINDOW_MS + 250L));
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(attack1.get(0), getCmd(item), id + ": Expired combo window must reset to Attack 1");
            sharedScheduler.advanceTicks(attack1.size() * fireRate + 2);

            st.setLastMeleeAttackMs(0L);
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertEquals(0, st.getMeleeComboStep(), id + ": Heavy attack must reset combo step to 0");
            assertEquals(attack3.get(0), getCmd(item), id + ": Right Click must start ONLY Attack 3 heavy frame 0");
            List<Integer> observedA3 = collectCmdsOverTicks(item, attack3.size() * fireRate + 2);
            for (int cmd : attack3) {
                assertTrue(observedA3.contains(cmd), id + ": Attack 3 missing frame " + cmd);
            }
            for (int cmd : attack2) {
                if (!attack3.contains(cmd)) {
                    assertFalse(observedA3.contains(cmd), id + ": Attack 3 concatenated Attack 2 frame " + cmd);
                }
            }
            assertEquals(WeaponState.IDLE, st.getCurrentState());
            assertEquals(idleModel, getCmd(item));

            st.setLastMeleeAttackMs(0L);
            LivingEntity victim = createDummyLivingEntity();
            EntityDamageByEntityEvent dmgEvent = fireDamageEntity(victim);
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(dmgEvent.isCancelled());
            assertEquals(1, st.getMeleeComboStep(), id + ": EntityDamage + LeftClick in same tick must NOT double-fire Attack 1 -> Attack 2");
            assertEquals(attack1.get(0), getCmd(item), id + ": EntityDamage + LeftClick must remain on Attack 1");
            sharedScheduler.advanceTicks(attack1.size() * fireRate + 2);

            st.setLastMeleeAttackMs(0L);
            PlayerInteractEntityEvent ieEvent = fireInteractEntity(victim, EquipmentSlot.HAND);
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            assertTrue(ieEvent.isCancelled());
            assertEquals(attack3.get(0), getCmd(item), id + ": InteractEntity + RightClick must trigger Attack 3 once");
            sharedScheduler.advanceTicks(attack3.size() * fireRate + 2);

            fireSwapHand();
            assertEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": F key must start inspect");
            sharedScheduler.advanceTicks(3);
            fireSwapHand();
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": Second F must cancel inspect");
            assertEquals(idleModel, getCmd(item));

            PlayerDropItemEvent dropEvent = fireDropQEvent(item);
            assertFalse(dropEvent.isCancelled(), id + ": Q on melee must allow normal drop");
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": Q on melee must NOT reload");
            assertEquals(idleModel, getCmd(item), id + ": Q on melee must NOT change CMD");
        }
    }

    @Test
    void testInspectInterruptZeroIdleFlashStutterAll17() {
        for (String id : ALL_18_WEAPONS) {
            WeaponDefinition weapon = weaponManager.getWeapon(id);

            setUpQuietly();
            ItemStack item = equipTrackedWeaponAtSlot0(weapon);
            int idleModel = weapon.getAnimation().getIdleModelData();
            startInspectAndAdvanceTwoFrames(weapon, item);
            int writeCountBeforeLeft = getCmdWriteLog(item).size();
            fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
            List<Integer> leftWrites = getCmdWriteLog(item).subList(writeCountBeforeLeft, getCmdWriteLog(item).size());
            assertEquals(1, leftWrites.size(), id + ": Left-Click inspect interrupt must write exactly frame 0 without intermediate idle flash: " + leftWrites);
            assertEquals(weapon.getAnimation().getFireFrames().get(0), leftWrites.get(0), id + ": Left-Click inspect interrupt must jump straight to frame 0");
            assertFalse(leftWrites.contains(idleModel), id + ": Left-Click inspect interrupt flashed idleModelData");

            setUpQuietly();
            item = equipTrackedWeaponAtSlot0(weapon);
            startInspectAndAdvanceTwoFrames(weapon, item);
            int writeCountBeforeRight = getCmdWriteLog(item).size();
            fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            List<Integer> rightWrites = getCmdWriteLog(item).subList(writeCountBeforeRight, getCmdWriteLog(item).size());
            int expectedRightFrame0 = weapon.getType().isGun()
                ? weapon.getAnimation().getAdsInFrames().get(0)
                : weapon.getAnimation().resolveHeavyFrames().get(0);
            assertEquals(1, rightWrites.size(), id + ": Right-Click inspect interrupt must write exactly frame 0 without intermediate idle flash: " + rightWrites);
            assertEquals(expectedRightFrame0, rightWrites.get(0), id + ": Right-Click inspect interrupt must jump straight to frame 0");
            assertFalse(rightWrites.contains(idleModel), id + ": Right-Click inspect interrupt flashed idleModelData");
        }
    }

    @Test
    void testCsgCommandTranslatesAmpersandColorCodes() {
        List<String> messages = new ArrayList<>();
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{CommandSender.class},
            (proxy, method, args) -> {
                if ("sendMessage".equals(method.getName()) && args != null && args.length >= 1 && args[0] instanceof String msg) {
                    messages.add(msg);
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );

        CSGCommand cmd = new CSGCommand(plugin);
        assertTrue(cmd.onCommand(sender, null, "csg", new String[]{"list"}));
        assertTrue(messages.size() >= 18);
        String joined = String.join("\n", messages);
        assertTrue(joined.contains("§"));
        assertFalse(joined.contains("&6"));
        assertFalse(joined.contains("&a"));
        assertFalse(joined.contains("&7"));
        assertFalse(joined.contains("&e"));
    }

    @Test
    void testResolveSoundKeyNamespaces() {
        assertEquals("", WeaponManager.resolveSoundKey(null));
        assertEquals("", WeaponManager.resolveSoundKey(""));
        assertEquals("cookie_csg:csgo.weapon.ak47.shoot", WeaponManager.resolveSoundKey("csgo.weapon.ak47.shoot"));
        assertEquals("cookie_csg:csgo.weapon.knife.backstab", WeaponManager.resolveSoundKey("csgo.weapon.knife.backstab"));
        assertEquals("csg:sim_clip_in", WeaponManager.resolveSoundKey("csg:sim_clip_in"));
        assertEquals("cookie_csg:csgo.weapon.aim.in", WeaponManager.resolveSoundKey("cookie_csg:csgo.weapon.aim.in"));
    }

    @Test
    void testSwingSuppressionHasteLifecycleAndPreservesExistingHaste() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        equipWeaponAtSlot0(ak);

        PotionEffect active = playerState.potionEffects.get(PotionEffectType.HASTE);
        assertNotNull(active, "HASTE swing suppressor missing after equipping weapon");
        assertEquals(AnimationEngine.SWING_SUPPRESS_HASTE_AMPLIFIER, active.getAmplifier());
        assertEquals(PotionEffect.INFINITE_DURATION, active.getDuration());
        assertFalse(active.hasParticles());
        assertFalse(active.hasIcon());

        switchHotbarSlot(0, 1);
        assertFalse(playerState.potionEffects.containsKey(PotionEffectType.HASTE), "HASTE swing suppressor not cleared on unequip");

        PotionEffect playerBeaconHaste = new PotionEffect(PotionEffectType.HASTE, 400, 1, false, true, true);
        playerState.potionEffects.put(PotionEffectType.HASTE, playerBeaconHaste);
        switchHotbarSlot(1, 0);

        PotionEffect suppressed = playerState.potionEffects.get(PotionEffectType.HASTE);
        assertNotNull(suppressed);
        assertEquals(AnimationEngine.SWING_SUPPRESS_HASTE_AMPLIFIER, suppressed.getAmplifier());

        switchHotbarSlot(0, 1);
        PotionEffect restored = playerState.potionEffects.get(PotionEffectType.HASTE);
        assertNotNull(restored, "Pre-existing non-suppressor HASTE was not restored");
        assertEquals(1, restored.getAmplifier());
        assertEquals(400, restored.getDuration());
    }

    @Test
    void testInspectAndReloadClearAdsSilentlyWithoutAdsOutFlash() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        FakeItemStack tracked = equipTrackedWeaponAtSlot0(ak);
        int adsSettleTicks = Math.max(2, ak.getAnimation().getAdsInFrames().size() * Math.max(1, ak.getAnimation().getAdsTickRate()));

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(adsSettleTicks + 2);
        assertTrue(adsManager.isAiming(player));

        tracked.cmdWriteLog.clear();
        playerState.playedSounds.clear();
        fireSwapHand();
        assertFalse(adsManager.isAiming(player));
        assertFalse(playerState.playedSounds.contains(WeaponManager.resolveSoundKey(ak.getAudio().getAdsOut())));
        int firstInspectCmd = ak.getAnimation().getInspectFrames().get(0);
        assertEquals(firstInspectCmd, getCmd(tracked));
        assertFalse(tracked.cmdWriteLog.isEmpty());
        assertEquals(firstInspectCmd, tracked.cmdWriteLog.get(0));

        sharedScheduler.advanceTicks(ak.getAnimation().getInspectFrames().size() * Math.max(1, ak.getAnimation().getInspectTickRate()) + 2);
        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(adsSettleTicks + 2);
        assertTrue(adsManager.isAiming(player));

        tracked.cmdWriteLog.clear();
        playerState.playedSounds.clear();
        fireShiftFReload();
        assertFalse(adsManager.isAiming(player));
        assertFalse(playerState.playedSounds.contains(WeaponManager.resolveSoundKey(ak.getAudio().getAdsOut())));
        int firstReloadCmd = effectiveFirstReloadFrame(ak, animationEngine.getState(playerState.uuid).getAmmoInClip());
        assertEquals(firstReloadCmd, getCmd(tracked));
        assertEquals(firstReloadCmd, tracked.cmdWriteLog.get(0));
    }

    @Test
    void testCleanItemsAdderPackHas17WeaponsAndNoMinecraftSoundsJson() throws Exception {
        File iaRoot = new File("CookieCSG_DONE/Pack 1/ItemsAdder/contents/cookie_csg");
        File weaponsYml = new File(iaRoot, "configs/weapons.yml");
        assertTrue(weaponsYml.exists(), "Missing trimmed ItemsAdder weapons.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(weaponsYml);
        var itemsSection = yml.getConfigurationSection("items");
        assertNotNull(itemsSection);
        assertEquals(18, itemsSection.getKeys(false).size(), "Expected exactly 18 base items in ItemsAdder weapons.yml");

        File modelsDir = new File(iaRoot, "resourcepack/assets/cookie_csg/models/item");
        for (String key : itemsSection.getKeys(false)) {
            String modelPath = itemsSection.getString(key + ".resource.model_path");
            assertNotNull(modelPath, "Missing model_path for " + key);
            assertTrue(modelPath.startsWith("cookie_csg:item/"), "Unexpected model_path " + modelPath);
            String modelFile = modelPath.substring("cookie_csg:item/".length()) + ".json";
            assertTrue(new File(modelsDir, modelFile).exists(), "Missing model file for " + key + ": " + modelFile);
        }

        assertFalse(new File(iaRoot, "resourcepack/assets/minecraft/sounds.json").exists(), "Duplicate assets/minecraft/sounds.json must not exist");
        File csgSounds = new File(iaRoot, "resourcepack/assets/cookie_csg/sounds.json");
        assertTrue(csgSounds.exists(), "Missing assets/cookie_csg/sounds.json");
    }

    @Test
    void testShiftFReloadsGunsAndFInspectsAll17() {
        for (String id : ALL_18_WEAPONS) {
            setUpQuietly();
            WeaponDefinition weapon = weaponManager.getWeapon(id);
            ItemStack item = equipWeaponAtSlot0(weapon);
            PlayerWeaponState st = animationEngine.getState(playerState.uuid);

            if (weapon.getType().isGun()) {
                fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
                sharedScheduler.advanceTicks(Math.max(2, weapon.getStats().getFireRateTicks()) + 1);
                assertTrue(st.getAmmoInClip() < weapon.getStats().getClipSize(), id + ": expected ammo < clip after shot");
            }

            playerState.sneaking = false;
            fireSwapHand();
            assertEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": plain F must trigger INSPECTING");
            fireSwapHand();
            assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": second plain F must return to IDLE");

            playerState.sneaking = true;
            fireSwapHand();
            if (weapon.getType().isGun()) {
                assertEquals(WeaponState.RELOADING, st.getCurrentState(), id + ": Shift+F must trigger RELOADING for gun");
                int ammoBefore = st.getAmmoInClip();
                int firstReloadCmd = effectiveFirstReloadFrame(weapon, ammoBefore);
                assertEquals(firstReloadCmd, getCmd(item), id + ": Shift+F must immediately render first reload frame");
                int reloadDuration = effectiveReloadDurationTicks(weapon, ammoBefore);
                sharedScheduler.advanceTicks(reloadDuration + 2);
                assertEquals(WeaponState.IDLE, st.getCurrentState(), id + ": Shift+F reload did not finish back to IDLE");
                assertEquals(weapon.getStats().getClipSize(), st.getAmmoInClip(), id + ": Shift+F reload did not refill clip");
            } else {
                assertEquals(WeaponState.INSPECTING, st.getCurrentState(), id + ": Shift+F on melee must fall back to INSPECTING");
            }
            playerState.sneaking = false;
        }

        setUpQuietly();
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        FakeItemStack tracked = equipTrackedWeaponAtSlot0(ak);
        int adsSettleTicks = Math.max(2, ak.getAnimation().getAdsInFrames().size() * Math.max(1, ak.getAnimation().getAdsTickRate()));
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(Math.max(2, ak.getStats().getFireRateTicks()) + 1);
        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(adsSettleTicks + 2);
        assertTrue(adsManager.isAiming(player));

        tracked.cmdWriteLog.clear();
        playerState.playedSounds.clear();
        playerState.sneaking = true;
        fireSwapHand();
        assertFalse(adsManager.isAiming(player));
        assertFalse(playerState.playedSounds.contains(WeaponManager.resolveSoundKey(ak.getAudio().getAdsOut())));
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        assertEquals(WeaponState.RELOADING, st.getCurrentState());
        int firstReloadCmd = effectiveFirstReloadFrame(ak, st.getAmmoInClip());
        assertEquals(firstReloadCmd, getCmd(tracked));
        assertEquals(firstReloadCmd, tracked.cmdWriteLog.get(0));
        playerState.sneaking = false;
    }

    @Test
    void testGunAndMeleeDamageNotCancelledByEntityDamageListener() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        equipWeaponAtSlot0(ak);
        List<EntityDamageByEntityEvent> events = new ArrayList<>();
        int[] iFrameClears = new int[1];
        double[] totalApplied = new double[1];
        LivingEntity victim = createDamageableVictim(events, iFrameClears, totalApplied);
        playerState.rayTraceOverride = new RayTraceResult(new Vector(0.0, 65.0, 5.0), victim);

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(1, events.size(), "Gun hit must trigger exactly 1 EntityDamageByEntityEvent");
        assertFalse(events.get(0).isCancelled(), "Weapon raycast damage event must NOT be cancelled");
        assertEquals(ak.getStats().getDamage(), totalApplied[0], 0.001);
        assertEquals(1, iFrameClears[0]);

        EntityDamageByEntityEvent directSwingEvent = fireDamageEntity(victim);
        assertTrue(directSwingEvent.isCancelled(), "Direct vanilla left-click EntityDamageByEntityEvent must still be cancelled");

        setUpQuietly();
        WeaponDefinition knife = weaponManager.getWeapon("combat_knife");
        equipWeaponAtSlot0(knife);
        List<EntityDamageByEntityEvent> meleeEvents = new ArrayList<>();
        int[] meleeClears = new int[1];
        double[] meleeApplied = new double[1];
        LivingEntity meleeVictim = createDamageableVictim(meleeEvents, meleeClears, meleeApplied);
        playerState.nearbyEntitiesOverride = List.of(meleeVictim);
        playerState.rayTraceEntitiesOverride = new RayTraceResult(new Vector(0.0, 65.0, 2.0), meleeVictim);

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(1, meleeEvents.size());
        assertFalse(meleeEvents.get(0).isCancelled(), "Melee slash damage event must NOT be cancelled");
        assertEquals(knife.getStats().getDamage(), meleeApplied[0], 0.001);

        meleeEvents.clear();
        meleeApplied[0] = 0.0;
        PlayerWeaponState meleeState = animationEngine.getState(playerState.uuid);
        meleeState.setLastMeleeAttackMs(0L);
        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(1, meleeEvents.size());
        assertFalse(meleeEvents.get(0).isCancelled(), "Melee heavy stab damage event must NOT be cancelled");
        assertTrue(meleeApplied[0] >= knife.getStats().getDamage(), "Melee heavy stab must deal positive damage");
    }

    @Test
    void testShotgunPelletDamageAggregatedAndRapidFireBypassesNoDamageTicks() {
        WeaponDefinition m870 = weaponManager.getWeapon("m870");
        assertNotNull(m870);
        equipWeaponAtSlot0(m870);
        List<EntityDamageByEntityEvent> events = new ArrayList<>();
        int[] iFrameClears = new int[1];
        double[] totalApplied = new double[1];
        LivingEntity victim = createDamageableVictim(events, iFrameClears, totalApplied);
        playerState.rayTraceOverride = new RayTraceResult(new Vector(0.0, 65.0, 5.0), victim);

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(1, events.size(), "All 8 shotgun pellets hitting same victim must aggregate into 1 damage call");
        assertFalse(events.get(0).isCancelled());
        assertEquals(m870.getStats().getDamage() * 8.0, totalApplied[0], 0.001);
        assertEquals(1, iFrameClears[0]);
    }

    @Test
    void testDoubleShiftCyclesFireModesAndRespectsSingleModeWeapons() {
        WeaponDefinition m4a1 = weaponManager.getWeapon("csgo_m4a1");
        equipWeaponAtSlot0(m4a1);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        assertEquals(FireMode.AUTO, st.getFireMode(m4a1));

        fireToggleSneak(true);
        assertEquals(FireMode.AUTO, st.getFireMode(m4a1), "Single shift must not switch fire mode");

        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(m4a1), "Double shift must switch AUTO -> SEMI-AUTO");

        st.setLastSneakPressMs(System.currentTimeMillis() - 600L);
        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(m4a1), "Slow shift (>400ms) must not switch fire mode");

        fireToggleSneak(true);
        assertEquals(FireMode.AUTO, st.getFireMode(m4a1), "Double shift must cycle SEMI-AUTO -> AUTO");

        WeaponDefinition awp = weaponManager.getWeapon("awp");
        equipWeaponAtSlot0(awp);
        assertEquals(FireMode.SINGLE, st.getFireMode(awp));
        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.SINGLE, st.getFireMode(awp), "Fixed single-mode sniper must stay SINGLE");
    }

    @Test
    void testSemiAutoFiresSingleRoundAndAutoFiresContinuously() {
        WeaponDefinition m4a1 = weaponManager.getWeapon("csgo_m4a1");
        equipWeaponAtSlot0(m4a1);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int m4Clip = m4a1.getStats().getClipSize();
        assertEquals(FireMode.AUTO, st.getFireMode(m4a1));

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(m4a1));

        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(m4Clip - 1, st.getAmmoInClip());
        sharedScheduler.advanceTicks(10);
        assertEquals(m4Clip - 1, st.getAmmoInClip(), "SEMI-AUTO mode must fire strictly 1 round per click");

        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        equipWeaponAtSlot0(ak);
        st = animationEngine.getState(playerState.uuid);
        int akClip = ak.getStats().getClipSize();
        assertEquals(FireMode.AUTO, st.getFireMode(ak));

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(ak));
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(10);
        assertEquals(akClip - 2, st.getAmmoInClip(), "SEMI-AUTO mode must never schedule auto-fill shots");

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.AUTO, st.getFireMode(ak));
        st.setAmmoInClip(ak.getId(), akClip);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        st.setLastFireTime(System.currentTimeMillis() - 150L);
        sharedScheduler.advanceTicks(3);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        st.setLastFireTime(System.currentTimeMillis() - 150L);
        sharedScheduler.advanceTicks(2);
        assertTrue(st.getAmmoInClip() < akClip - 2, "AUTO mode continuous spray must fire additional rounds");
    }

    @Test
    void testM249AliasTabCompleteAndCmdResolution() {
        WeaponDefinition m249 = weaponManager.getWeapon("csgo_m249");
        assertNotNull(m249);
        assertEquals(m249, weaponManager.getWeapon("m249"));

        ItemStack rawAliasItem = new FakeItemStack(Material.IRON_HOE, 1);
        rawAliasItem.editMeta(meta -> meta.setCustomModelData(200401));
        assertEquals("csgo_m249", weaponManager.getWeaponId(rawAliasItem));

        CSGCommand command = new CSGCommand(plugin);
        List<String> giveM = command.onTabComplete(player, null, "csg", new String[]{"give", "SimTester", "m"});
        assertTrue(giveM.contains("m249"));
        List<String> giveCsgo = command.onTabComplete(player, null, "csg", new String[]{"give", "SimTester", "csgo_"});
        assertTrue(giveCsgo.contains("csgo_m249"));
        List<String> editM = command.onTabComplete(player, null, "csg", new String[]{"edit", "m24"});
        assertTrue(editM.contains("m249"));
    }

    @Test
    void testAdsOpticalZoomLevelsAndWalkSpeedCompensation() {
        WeaponDefinition aug = weaponManager.getWeapon("aug");
        WeaponDefinition awp = weaponManager.getWeapon("awp");
        WeaponDefinition kar98 = weaponManager.getWeapon("kar98");
        WeaponDefinition deagle = weaponManager.getWeapon("deagle");
        WeaponDefinition p90 = weaponManager.getWeapon("p90");
        assertNotNull(aug);
        assertNotNull(awp);
        assertNotNull(kar98);
        assertNotNull(deagle);
        assertNotNull(p90);

        playerState.walkSpeed = 0.2f;
        playerState.moveSpeedBase = 0.1;
        playerState.moveSpeedMods.clear();

        adsManager.setAiming(player, deagle, true);
        double deagleVal = playerState.moveSpeedAttr.getValue();
        double deagleFov = ((deagleVal / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
        assertTrue(deagleVal > 0.01);
        assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
        adsManager.setAiming(player, deagle, false);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);
        assertEquals(0.1, playerState.moveSpeedBase, 1e-6);
        assertTrue(playerState.moveSpeedMods.isEmpty());

        adsManager.setAiming(player, kar98, true);
        double karVal = playerState.moveSpeedAttr.getValue();
        double karFov = ((karVal / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
        assertTrue(karVal > 0.01);
        assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
        assertEquals(deagleFov, karFov, 1e-4);
        adsManager.setAiming(player, kar98, false);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);

        adsManager.setAiming(player, p90, true);
        double p90Val = playerState.moveSpeedAttr.getValue();
        double p90Fov = ((p90Val / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
        assertTrue(p90Val > 0.01);
        assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
        assertTrue(p90Fov < deagleFov);
        adsManager.setAiming(player, p90, false);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);

        adsManager.setAiming(player, aug, true);
        double augVal = playerState.moveSpeedAttr.getValue();
        double augFov = ((augVal / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
        assertTrue(augVal > 0.01);
        assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
        assertTrue(augFov < p90Fov && augFov < 0.45);
        adsManager.setAiming(player, aug, false);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);

        adsManager.setAiming(player, awp, true);
        double awpVal = playerState.moveSpeedAttr.getValue();
        double awpFov = ((awpVal / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
        assertTrue(awpVal > 0.01);
        assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
        assertTrue(awpFov < augFov && awpFov < 0.28);
        adsManager.setAiming(player, awp, false);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);

        int prevZoom = awp.getAdsConfig().getZoomLevel();
        try {
            awp.getAdsConfig().setZoomLevel(5);
            adsManager.setAiming(player, awp, true);
            double z5Val = playerState.moveSpeedAttr.getValue();
            double z5Fov = ((z5Val / (playerState.walkSpeed / 2.0)) + 1.0) / 2.0;
            assertTrue(z5Val > 0.01);
            assertEquals(0.1, playerState.moveSpeedAttr.getBaseValue(), 1e-6);
            assertFalse(Float.isNaN(playerState.walkSpeed));
            assertTrue(playerState.walkSpeed >= -1.0f && playerState.walkSpeed <= 1.0f);
            assertTrue(z5Fov < awpFov && z5Fov > 0.05);
            adsManager.setAiming(player, awp, false);
            assertEquals(0.2f, playerState.walkSpeed, 1e-4f);
        } finally {
            awp.getAdsConfig().setZoomLevel(prevZoom);
        }
        adsManager.cleanup(player);
        assertEquals(0.2f, playerState.walkSpeed, 1e-4f);
        assertEquals(0.1, playerState.moveSpeedBase, 1e-6);
        assertTrue(playerState.moveSpeedMods.isEmpty());
    }

    @Test
    void testFireModeEnglishLabels() {
        assertEquals("SINGLE", FireMode.SINGLE.getDisplayName());
        assertEquals("SEMI-AUTO", FireMode.SEMI_AUTO.getDisplayName());
        assertEquals("AUTO", FireMode.AUTO.getDisplayName());
        assertEquals(FireMode.SINGLE, FireMode.fromString("SINGLE"));
        assertEquals(FireMode.SEMI_AUTO, FireMode.fromString("SEMI-AUTO"));
        assertEquals(FireMode.SEMI_AUTO, FireMode.fromString("SEMI"));
        assertEquals(FireMode.SEMI_AUTO, FireMode.fromString("BURST"));
        assertEquals(FireMode.AUTO, FireMode.fromString("AUTO"));
    }

    @Test
    void testMainMenuLeftClickGivesWeaponAndRightClickOpensEditor() {
        MainMenu menu = new MainMenu(plugin, player, 0);
        Inventory topInv = menu.getInventory();
        ItemStack firstIcon = topInv.getItem(0);
        assertNotNull(firstIcon);
        assertFalse(firstIcon.getItemMeta().getLore().isEmpty());
        assertTrue(firstIcon.getItemMeta().getLore().stream().anyMatch(l -> l.contains("Left-Click: Receive weapon to test")));
        assertTrue(firstIcon.getItemMeta().getLore().stream().anyMatch(l -> l.contains("Right-Click: Edit weapon")));

        InventoryView view = (InventoryView) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{InventoryView.class},
            (p, method, args) -> switch (method.getName()) {
                case "getTopInventory" -> topInv;
                case "getBottomInventory" -> playerState.inventory;
                case "getPlayer" -> player;
                case "getInventory" -> (args != null && args.length == 1 && (int) args[0] < 54) ? topInv : playerState.inventory;
                case "getSlotType" -> InventoryType.SlotType.CONTAINER;
                case "convertSlot" -> args != null && args.length == 1 ? (int) args[0] : 0;
                case "getType" -> InventoryType.CHEST;
                default -> defaultValue(method.getReturnType());
            }
        );

        playerState.inventorySlots[0] = new FakeItemStack(Material.AIR, 0);
        InventoryClickEvent leftEvt = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        menu.handleClick(leftEvt);
        assertFalse(playerState.inventorySlots[0].getType().isAir(), "Left-Click in MainMenu must give weapon directly");
        assertNotNull(weaponManager.getWeaponId(playerState.inventorySlots[0]));
        assertNull(playerState.openedInventory, "Left-Click must NOT open editor menu");

        InventoryClickEvent rightEvt = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.RIGHT, InventoryAction.PICKUP_HALF);
        menu.handleClick(rightEvt);
        assertNotNull(playerState.openedInventory);
        assertTrue(playerState.openedInventory.getHolder() instanceof WeaponEditorMenu, "Right-Click in MainMenu must open WeaponEditorMenu");
    }

    @Test
    void testRelativeRecoilNeverLocksPlayerMouseRotation() {
        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        assertNotNull(ak);
        equipWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(ak));

        player.setRotation(45.0f, 10.0f);
        playerState.teleportCalls = 0;
        playerState.setRotationCalls = 0;
        playerState.lastTeleportFlags.clear();

        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(0, playerState.teleportCalls);
        assertEquals(1, playerState.setRotationCalls);
        assertTrue(playerState.pitch < 10.0f && playerState.pitch >= -90.0f);
        assertTrue(Math.abs(playerState.yaw - 45.0f) <= ak.getRecoil().getHorizontal() + 0.01f);

        float turnedYaw = 135.0f;
        float turnedPitch = 18.0f;
        player.setRotation(turnedYaw, turnedPitch);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(0, playerState.teleportCalls);
        assertEquals(3, playerState.setRotationCalls);
        assertTrue(Math.abs(playerState.yaw - turnedYaw) <= ak.getRecoil().getHorizontal() + 0.01f, "Yaw must stay relative to concurrent mouse turn");
        assertTrue(Math.abs(playerState.yaw - 45.0f) > 50.0f, "Yaw must not snap back to pre-turn angle");
        assertTrue(playerState.pitch < turnedPitch && playerState.pitch > 15.0f, "Pitch recoil must apply relative to turned pitch");

        player.setRotation(0.0f, -89.8f);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(0, playerState.teleportCalls);
        assertEquals(-90.0f, playerState.pitch, 1e-4f);

        player.setRotation(0.0f, -90.0f);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(0, playerState.teleportCalls);
        assertTrue(playerState.pitch >= -90.0f && playerState.pitch <= 90.0f);

        player.setRotation(0.0f, 90.0f);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(0, playerState.teleportCalls);
        assertTrue(playerState.pitch >= -90.0f && playerState.pitch <= 90.0f);
    }

    @Test
    void testHoldToAdsEntersMaintainsDuringFireAndReleasesOnTimeout() {
        assertTrue(plugin.isHoldToAds());
        int holdTicks = Math.max(4, (int) Math.ceil(plugin.getHoldAdsReleaseMs() / 50.0));

        WeaponDefinition ak = weaponManager.getWeapon("ak47");
        assertNotNull(ak);
        FakeItemStack tracked = equipTrackedWeaponAtSlot0(ak);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);

        List<Integer> adsIn = ak.getAnimation().getAdsInFrames();
        List<Integer> adsFire = ak.getAnimation().getAdsFireFrames();
        List<Integer> adsOut = ak.getAnimation().getAdsOutFrames();
        int adsModel = ak.getAnimation().getAdsModelData();
        int idleModel = ak.getAnimation().getIdleModelData();
        int adsRate = Math.max(1, ak.getAnimation().getAdsTickRate());
        int fireRate = Math.max(1, ak.getAnimation().getFireTickRate());

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player));
        assertTrue(st.isAiming());
        assertEquals(WeaponState.AIMING_ADS, st.getCurrentState());
        assertEquals(adsIn.get(0), getCmd(tracked));
        BukkitTask initialHoldTask = st.getAdsHoldTask();
        assertNotNull(initialHoldTask);

        for (int t = 0; t < 8; t++) {
            if (t > 0 && t % 4 == 0) {
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            }
            sharedScheduler.stepOneTick();
        }
        assertTrue(adsManager.isAiming(player));
        assertSame(initialHoldTask, st.getAdsHoldTask());
        assertEquals(adsModel, getCmd(tracked));

        tracked.cmdWriteLog.clear();
        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player));
        assertSame(initialHoldTask, st.getAdsHoldTask());
        assertFalse(tracked.cmdWriteLog.contains(adsIn.get(0)), "RMB hold pulse must not restart adsIn animation");
        assertTrue(tracked.cmdWriteLog.isEmpty(), "RMB hold pulse while settled in ADS must not overwrite model");

        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertTrue(adsManager.isAiming(player));
        assertTrue(st.isAiming());
        assertEquals(WeaponState.FIRING, st.getCurrentState());
        List<Integer> observedAdsFire = new ArrayList<>();
        for (int t = 0; t <= adsFire.size() * fireRate + 2; t++) {
            if (t > 0 && t % 4 == 0) {
                fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
            }
            observedAdsFire.add(getCmd(tracked));
            sharedScheduler.stepOneTick();
        }
        for (int expectedCmd : adsFire) {
            assertTrue(observedAdsFire.contains(expectedCmd), "Missing adsFire frame " + expectedCmd);
        }
        assertTrue(adsManager.isAiming(player), "Firing while holding RMB must keep ADS active");
        assertEquals(WeaponState.AIMING_ADS, st.getCurrentState());
        assertEquals(adsModel, getCmd(tracked));

        fireInteract(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND);
        sharedScheduler.advanceTicks(holdTicks - 1);
        assertTrue(adsManager.isAiming(player), "Hold-ADS must remain active before holdTicks elapse");

        List<Integer> observedAdsOut = new ArrayList<>();
        for (int t = 0; t <= adsOut.size() * adsRate + 3; t++) {
            observedAdsOut.add(getCmd(tracked));
            sharedScheduler.stepOneTick();
        }
        assertFalse(adsManager.isAiming(player), "Hold-ADS must exit after holdTicks elapse without RMB pulses");
        assertFalse(st.isAiming());
        assertNull(playerState.potionEffects.get(PotionEffectType.SLOWNESS));
        for (int expectedCmd : adsOut) {
            assertTrue(observedAdsOut.contains(expectedCmd), "Missing adsOut frame " + expectedCmd);
        }
        assertEquals(WeaponState.IDLE, st.getCurrentState());
        assertEquals(idleModel, getCmd(tracked));
    }

    @Test
    void testFireModesAutoAndSemiAutoDistinctBehavior() {
        WeaponDefinition m4a1 = weaponManager.getWeapon("csgo_m4a1");
        assertNotNull(m4a1);
        equipWeaponAtSlot0(m4a1);
        PlayerWeaponState st = animationEngine.getState(playerState.uuid);
        int clip = m4a1.getStats().getClipSize();

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.SEMI_AUTO, st.getFireMode(m4a1));

        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(clip - 1, st.getAmmoInClip());
        assertNull(st.getAutoFireTask());
        sharedScheduler.advanceTicks(10);
        assertEquals(clip - 1, st.getAmmoInClip(), "SEMI-AUTO mode must fire strictly 1 shot");

        st.setLastFireTime(System.currentTimeMillis() - 100L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(clip - 1, st.getAmmoInClip(), "SEMI-AUTO mode must enforce semi-auto cadence");

        st.setLastFireTime(System.currentTimeMillis() - 250L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(clip - 2, st.getAmmoInClip());

        fireToggleSneak(true);
        fireToggleSneak(true);
        assertEquals(FireMode.AUTO, st.getFireMode(m4a1));

        st.setAmmoInClip(m4a1.getId(), clip);
        st.setLastFireTime(0L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertEquals(clip - 1, st.getAmmoInClip());
        BukkitTask autoRunner = st.getAutoFireTask();
        assertNotNull(autoRunner);
        assertFalse(autoRunner.isCancelled());

        sharedScheduler.stepOneTick();
        st.setLastFireTime(System.currentTimeMillis() - 150L);
        fireInteract(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND);
        assertSame(autoRunner, st.getAutoFireTask(), "AUTO mode must maintain uninterrupted continuous runner");
        assertEquals(clip - 1, st.getAmmoInClip(), "Off-phase AUTO click must not fire off-cadence shot");

        sharedScheduler.stepOneTick();
        st.setLastFireTime(System.currentTimeMillis() - 150L);
        sharedScheduler.stepOneTick();
        assertEquals(clip - 2, st.getAmmoInClip(), "AUTO runner must fire on original tick phase without resetting");

        st.setLastFireTime(System.currentTimeMillis() - 150L);
        sharedScheduler.stepOneTick();
        assertEquals(clip - 2, st.getAmmoInClip());
        st.setLastFireTime(System.currentTimeMillis() - 150L);
        sharedScheduler.stepOneTick();
        assertEquals(clip - 3, st.getAmmoInClip());

        int step = Math.max(1, m4a1.getStats().getFireRateTicks());
        int windowTicks = Math.max(5, step * 2 + 2);
        for (int t = 0; t <= windowTicks + 2; t++) {
            st.setLastFireTime(System.currentTimeMillis() - 150L);
            sharedScheduler.stepOneTick();
        }
        assertTrue(autoRunner.isCancelled());
        assertNull(st.getAutoFireTask());
        int settledAmmo = st.getAmmoInClip();
        sharedScheduler.advanceTicks(6);
        assertEquals(settledAmmo, st.getAmmoInClip());
    }

    private static int effectiveReloadDurationTicks(WeaponDefinition weapon, int ammoInClip) {
        WeaponAnimation anim = weapon.getAnimation();
        int rate = Math.max(1, anim.getReloadTickRate());
        if (weapon.getType() == WeaponType.SHOTGUN && anim.getReloadLoopFrames() != null && !anim.getReloadLoopFrames().isEmpty()) {
            int introTicks = anim.getReloadIntroTicks() > 0 ? anim.getReloadIntroTicks() : anim.getReloadIntroFrames().size() * rate;
            int loopTicks = anim.getReloadLoopTicks() > 0 ? anim.getReloadLoopTicks() : anim.getReloadLoopFrames().size() * rate;
            int endTicks = anim.getReloadEndTicks() > 0 ? anim.getReloadEndTicks() : anim.getReloadEndFrames().size() * rate;
            int shells = Math.max(1, weapon.getStats().getClipSize() - Math.max(0, ammoInClip));
            return Math.max(1, introTicks + (shells * loopTicks) + endTicks);
        }
        boolean tactical = ammoInClip > 0 && anim.getReloadTacticalFrames() != null && !anim.getReloadTacticalFrames().isEmpty();
        if (tactical) {
            if (weapon.getStats().getReloadTacticalTicks() > 0) {
                return weapon.getStats().getReloadTacticalTicks();
            }
            if (weapon.getStats().getReloadTicks() > 0) {
                return weapon.getStats().getReloadTicks();
            }
            return Math.max(1, anim.getReloadTacticalFrames().size() * rate);
        }
        if (weapon.getStats().getReloadTicks() > 0) {
            return weapon.getStats().getReloadTicks();
        }
        return Math.max(1, anim.getReloadFrames().size() * rate);
    }

    private static int effectiveFirstReloadFrame(WeaponDefinition weapon, int ammoInClip) {
        WeaponAnimation anim = weapon.getAnimation();
        if (weapon.getType() == WeaponType.SHOTGUN && anim.getReloadLoopFrames() != null && !anim.getReloadLoopFrames().isEmpty()) {
            if (anim.getReloadIntroFrames() != null && !anim.getReloadIntroFrames().isEmpty()) {
                return anim.getReloadIntroFrames().get(0);
            }
            return anim.getReloadLoopFrames().get(0);
        }
        if (ammoInClip > 0 && anim.getReloadTacticalFrames() != null && !anim.getReloadTacticalFrames().isEmpty()) {
            return anim.getReloadTacticalFrames().get(0);
        }
        return anim.getReloadFrames().get(0);
    }

    private static int effectiveDistinctReloadFrameCount(WeaponDefinition weapon, int ammoInClip) {
        WeaponAnimation anim = weapon.getAnimation();
        if (weapon.getType() == WeaponType.SHOTGUN && anim.getReloadLoopFrames() != null && !anim.getReloadLoopFrames().isEmpty()) {
            return anim.getReloadIntroFrames().size() + anim.getReloadLoopFrames().size() + anim.getReloadEndFrames().size();
        }
        if (ammoInClip > 0 && anim.getReloadTacticalFrames() != null && !anim.getReloadTacticalFrames().isEmpty()) {
            return anim.getReloadTacticalFrames().size();
        }
        return anim.getReloadFrames().size();
    }

    private void setUpQuietly() {
        try {
            setUp();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ItemStack equipWeaponAtSlot0(WeaponDefinition weapon) {
        ItemStack item = weaponManager.buildItemStack(weapon);
        playerState.inventorySlots[0] = item;
        playerState.inventorySlots[1] = new FakeItemStack(Material.AIR, 0);
        playerState.heldSlot = 1;
        switchHotbarSlot(1, 0);
        int deployTicks = weapon.getAnimation().getDeployTicks() > 0 ? weapon.getAnimation().getDeployTicks() : 6;
        sharedScheduler.advanceTicks(deployTicks + 1);
        return item;
    }

    private FakeItemStack equipTrackedWeaponAtSlot0(WeaponDefinition weapon) {
        ItemStack built = weaponManager.buildItemStack(weapon);
        FakeItemStack tracked = new FakeItemStack(built.getType(), built.getAmount());
        tracked.setItemMeta(built.getItemMeta());
        tracked.cmdWriteLog.clear();
        playerState.inventorySlots[0] = tracked;
        playerState.inventorySlots[1] = new FakeItemStack(Material.AIR, 0);
        playerState.heldSlot = 1;
        switchHotbarSlot(1, 0);
        int deployTicks = weapon.getAnimation().getDeployTicks() > 0 ? weapon.getAnimation().getDeployTicks() : 6;
        sharedScheduler.advanceTicks(deployTicks + 1);
        return tracked;
    }

    private void startInspectAndAdvanceTwoFrames(WeaponDefinition weapon, ItemStack item) {
        fireSwapHand();
        int rate = Math.max(1, weapon.getAnimation().getInspectTickRate());
        sharedScheduler.advanceTicks(rate + 1);
    }

    private ItemStack buildFakeItemsAdderItem(WeaponDefinition weapon) {
        ItemStack item = new FakeItemStack(weapon.getMaterial(), 1);
        item.editMeta(meta -> {
            meta.setCustomModelData(weapon.getAnimation().getIdleModelData());
            meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, weapon.getId());
        });
        return item;
    }

    private void switchHotbarSlot(int prevSlot, int newSlot) {
        playerState.heldSlot = newSlot;
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(player, prevSlot, newSlot);
        listener.onItemHeldChange(event);
    }

    private void fireInteract(Action action, EquipmentSlot hand) {
        ItemStack held = player.getInventory().getItemInMainHand();
        PlayerInteractEvent event = new PlayerInteractEvent(player, action, held, null, BlockFace.SELF, hand);
        listener.onPlayerInteract(event);
    }

    private void fireSwapHand() {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = new FakeItemStack(Material.AIR, 0);
        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(player, main, off);
        listener.onSwapHand(event);
    }

    private void fireShiftFReload() {
        boolean prev = playerState.sneaking;
        playerState.sneaking = true;
        try {
            fireSwapHand();
        } finally {
            playerState.sneaking = prev;
        }
    }

    private PlayerDropItemEvent fireDropQEvent(ItemStack item) {
        Item dropEntity = (Item) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{Item.class},
            (proxy, method, args) -> "getItemStack".equals(method.getName()) ? item : defaultValue(method.getReturnType())
        );
        PlayerDropItemEvent event = new PlayerDropItemEvent(player, dropEntity);
        listener.onDropItem(event);
        return event;
    }

    private LivingEntity createDummyLivingEntity() {
        return (LivingEntity) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{LivingEntity.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private EntityDamageByEntityEvent fireDamageEntity(LivingEntity victim) {
        DamageSource dummySource = (DamageSource) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{DamageSource.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
            player,
            victim,
            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
            dummySource,
            1.0
        );
        listener.onEntityDamage(damageEvent);
        return damageEvent;
    }

    private PlayerInteractEntityEvent fireInteractEntity(Entity victim, EquipmentSlot hand) {
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, victim, hand);
        listener.onInteractEntity(event);
        return event;
    }

    private void fireToggleSneak(boolean sneaking) {
        playerState.sneaking = sneaking;
        PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(player, sneaking);
        listener.onToggleSneak(event);
        playerState.sneaking = false;
    }

    private LivingEntity createDamageableVictim(List<EntityDamageByEntityEvent> observedEvents, int[] iFrameClears, double[] totalApplied) {
        DamageSource dummySource = (DamageSource) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{DamageSource.class},
            (proxy, method, args) -> defaultValue(method.getReturnType())
        );
        return (LivingEntity) Proxy.newProxyInstance(
            AnimationSimulatorTest.class.getClassLoader(),
            new Class<?>[]{LivingEntity.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getLocation" -> new Location(playerState.world, 0.0, 64.0, 5.0);
                case "getEyeLocation" -> new Location(playerState.world, 0.0, 65.62, 5.0);
                case "getHeight" -> 1.8;
                case "isDead" -> false;
                case "getWorld" -> playerState.world;
                case "setNoDamageTicks" -> {
                    if (args != null && args.length >= 1 && ((int) args[0]) == 0) {
                        iFrameClears[0]++;
                    }
                    yield null;
                }
                case "damage" -> {
                    double amount = args != null && args.length >= 1 ? ((Number) args[0]).doubleValue() : 0.0;
                    Entity damager = args != null && args.length >= 2 && args[1] instanceof Entity e ? e : player;
                    EntityDamageByEntityEvent ev = new EntityDamageByEntityEvent(
                        damager,
                        (Entity) proxy,
                        EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                        dummySource,
                        amount
                    );
                    listener.onEntityDamage(ev);
                    observedEvents.add(ev);
                    if (!ev.isCancelled()) {
                        totalApplied[0] += ev.getDamage();
                    }
                    yield null;
                }
                case "equals" -> args != null && args.length == 1 && args[0] == proxy;
                case "hashCode" -> System.identityHashCode(proxy);
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private List<Integer> collectCmdsOverTicks(ItemStack item, int ticks) {
        List<Integer> cmds = new ArrayList<>();
        for (int t = 0; t <= ticks; t++) {
            cmds.add(getCmd(item));
            sharedScheduler.stepOneTick();
        }
        return cmds;
    }

    private static List<Integer> getCmdWriteLog(ItemStack item) {
        if (item instanceof FakeItemStack fake) {
            return fake.cmdWriteLog;
        }
        return Collections.emptyList();
    }

    private static int getCmd(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() ? meta.getCustomModelData() : -1;
    }

    private static NamespacedKey getItemModel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasItemModel() ? meta.getItemModel() : null;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == char.class) return '\0';
        return null;
    }

    static final class FakePlayerState {
        final UUID uuid = UUID.randomUUID();
        final ItemStack[] inventorySlots = new ItemStack[9];
        int heldSlot = 0;
        boolean sneaking = false;
        boolean sprinting = false;
        float yaw = 0f;
        float pitch = 0f;
        float walkSpeed = 0.2f;
        double moveSpeedBase = 0.1;
        final Map<NamespacedKey, AttributeModifier> moveSpeedMods = new LinkedHashMap<>();
        int teleportCalls = 0;
        int setRotationCalls = 0;
        final Set<TeleportFlag> lastTeleportFlags = new HashSet<>();
        Inventory openedInventory = null;
        final List<String> playedSounds = new ArrayList<>();
        final List<Component> actionBars = new ArrayList<>();
        final Map<PotionEffectType, PotionEffect> potionEffects = new HashMap<>();
        RayTraceResult rayTraceOverride = null;
        RayTraceResult rayTraceEntitiesOverride = null;
        List<Entity> nearbyEntitiesOverride = null;
        final World world;
        final PlayerInventory inventory;
        final AttributeInstance moveSpeedAttr;
        final Player proxy;

        FakePlayerState() {
            for (int i = 0; i < inventorySlots.length; i++) {
                inventorySlots[i] = new FakeItemStack(Material.AIR, 0);
            }
            this.world = (World) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{World.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getNearbyEntities" -> nearbyEntitiesOverride != null ? nearbyEntitiesOverride : Collections.emptyList();
                    case "rayTrace" -> rayTraceOverride;
                    case "rayTraceEntities" -> rayTraceEntitiesOverride;
                    case "getName" -> "sim_world";
                    default -> defaultValue(method.getReturnType());
                }
            );
            this.inventory = (PlayerInventory) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getItemInMainHand" -> inventorySlots[heldSlot];
                    case "setItemInMainHand" -> {
                        inventorySlots[heldSlot] = (ItemStack) args[0];
                        yield null;
                    }
                    case "getItem" -> {
                        int slot = (int) args[0];
                        yield (slot >= 0 && slot < inventorySlots.length) ? inventorySlots[slot] : new FakeItemStack(Material.AIR, 0);
                    }
                    case "setItem" -> {
                        int slot = (int) args[0];
                        if (slot >= 0 && slot < inventorySlots.length) {
                            inventorySlots[slot] = (ItemStack) args[1];
                        }
                        yield null;
                    }
                    case "getHeldItemSlot" -> heldSlot;
                    case "setHeldItemSlot" -> {
                        heldSlot = (int) args[0];
                        yield null;
                    }
                    case "addItem" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof ItemStack[] items) {
                            for (ItemStack it : items) {
                                if (it == null) continue;
                                for (int s = 0; s < inventorySlots.length; s++) {
                                    if (inventorySlots[s] == null || inventorySlots[s].getType() == Material.AIR) {
                                        inventorySlots[s] = it;
                                        break;
                                    }
                                }
                            }
                        }
                        yield new HashMap<Integer, ItemStack>();
                    }
                    default -> defaultValue(method.getReturnType());
                }
            );
            this.moveSpeedAttr = (AttributeInstance) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{AttributeInstance.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> Attribute.MOVEMENT_SPEED;
                    case "getBaseValue", "getDefaultValue" -> moveSpeedBase;
                    case "setBaseValue" -> {
                        moveSpeedBase = (double) args[0];
                        yield null;
                    }
                    case "addModifier", "addTransientModifier" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof AttributeModifier mod) {
                            moveSpeedMods.put(mod.getKey(), mod);
                        }
                        yield null;
                    }
                    case "removeModifier" -> {
                        if (args != null && args.length >= 1) {
                            if (args[0] instanceof NamespacedKey nk) {
                                moveSpeedMods.remove(nk);
                            } else if (args[0] instanceof AttributeModifier mod) {
                                moveSpeedMods.remove(mod.getKey());
                            }
                        }
                        yield null;
                    }
                    case "getModifier" -> args != null && args.length >= 1 && args[0] instanceof NamespacedKey nk
                        ? moveSpeedMods.get(nk)
                        : null;
                    case "getModifiers" -> List.copyOf(moveSpeedMods.values());
                    case "getValue" -> {
                        double add = 0.0;
                        double multBase = 1.0;
                        double multTotal = 1.0;
                        for (AttributeModifier mod : moveSpeedMods.values()) {
                            switch (mod.getOperation()) {
                                case ADD_NUMBER -> add += mod.getAmount();
                                case ADD_SCALAR -> multBase += mod.getAmount();
                                case MULTIPLY_SCALAR_1 -> multTotal *= (1.0 + mod.getAmount());
                            }
                        }
                        yield Math.max(0.0, (moveSpeedBase + add) * multBase * multTotal);
                    }
                    default -> defaultValue(method.getReturnType());
                }
            );
            this.proxy = (Player) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getName" -> "SimTester";
                    case "isOnline" -> true;
                    case "isSneaking" -> sneaking;
                    case "isSprinting" -> sprinting;
                    case "setSprinting" -> {
                        sprinting = (boolean) args[0];
                        yield null;
                    }
                    case "isOnGround" -> true;
                    case "getAttribute" -> args != null && args.length >= 1 && Attribute.MOVEMENT_SPEED.equals(args[0])
                        ? moveSpeedAttr
                        : null;
                    case "getInventory" -> inventory;
                    case "getWorld" -> world;
                    case "getLocation" -> new Location(world, 0, 64, 0, yaw, pitch);
                    case "getEyeLocation" -> new Location(world, 0, 65.62, 0, yaw, pitch);
                    case "getYaw" -> yaw;
                    case "getPitch" -> pitch;
                    case "setRotation" -> {
                        setRotationCalls++;
                        yaw = (float) args[0];
                        pitch = (float) args[1];
                        yield null;
                    }
                    case "teleport" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof Location loc) {
                            teleportCalls++;
                            lastTeleportFlags.clear();
                            for (int i = 1; i < args.length; i++) {
                                if (args[i] instanceof TeleportFlag flag) {
                                    lastTeleportFlags.add(flag);
                                } else if (args[i] instanceof TeleportFlag[] flags) {
                                    Collections.addAll(lastTeleportFlags, flags);
                                }
                            }
                            if (lastTeleportFlags.contains(TeleportFlag.Relative.YAW)) {
                                yaw += loc.getYaw();
                            } else {
                                yaw = loc.getYaw();
                            }
                            float nextPitch = lastTeleportFlags.contains(TeleportFlag.Relative.PITCH)
                                ? pitch + loc.getPitch()
                                : loc.getPitch();
                            pitch = Math.max(-90.0f, Math.min(90.0f, nextPitch));
                            yield true;
                        }
                        yield false;
                    }
                    case "getWalkSpeed" -> walkSpeed;
                    case "setWalkSpeed" -> {
                        walkSpeed = (float) args[0];
                        moveSpeedBase = Math.max(0.0, walkSpeed / 2.0);
                        yield null;
                    }
                    case "openInventory" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof Inventory inv) {
                            openedInventory = inv;
                        }
                        yield null;
                    }
                    case "playSound" -> {
                        if (args != null && args.length >= 2) {
                            playedSounds.add(String.valueOf(args[1]));
                        }
                        yield null;
                    }
                    case "sendActionBar" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof Component c) {
                            actionBars.add(c);
                        }
                        yield null;
                    }
                    case "getPotionEffect" -> potionEffects.get(args[0]);
                    case "addPotionEffect" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof PotionEffect pe) {
                            potionEffects.put(pe.getType(), pe);
                        }
                        yield true;
                    }
                    case "removePotionEffect" -> {
                        if (args != null && args.length >= 1 && args[0] instanceof PotionEffectType pet) {
                            potionEffects.remove(pet);
                        }
                        yield null;
                    }
                    case "equals" -> args[0] == p;
                    case "hashCode" -> uuid.hashCode();
                    default -> defaultValue(method.getReturnType());
                }
            );
        }
    }

    static final class FakeScheduler {
        private long currentTick = 0;
        private int nextTaskId = 1;
        private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>(
            Comparator.comparingLong((ScheduledTask t) -> t.runAtTick).thenComparingInt(t -> t.id)
        );
        private final Map<Integer, ScheduledTask> byId = new HashMap<>();

        void reset() {
            currentTick = 0;
            nextTaskId = 1;
            queue.clear();
            byId.clear();
        }

        void stepOneTick() {
            while (!queue.isEmpty() && queue.peek().runAtTick <= currentTick) {
                ScheduledTask task = queue.poll();
                if (task.cancelled) {
                    continue;
                }
                task.runnable.run();
                if (!task.cancelled && task.period > 0) {
                    task.runAtTick = currentTick + task.period;
                    queue.add(task);
                }
            }
            currentTick++;
        }

        void advanceTicks(int ticks) {
            for (int i = 0; i < ticks; i++) {
                stepOneTick();
            }
        }

        private BukkitTask schedule(Plugin owner, Runnable runnable, long delay, long period) {
            int id = nextTaskId++;
            long targetTick = currentTick + Math.max(0L, delay);
            ScheduledTask task = new ScheduledTask(id, owner, runnable, targetTick, period);
            byId.put(id, task);
            queue.add(task);
            return task;
        }

        BukkitScheduler asBukkitScheduler() {
            return (BukkitScheduler) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{BukkitScheduler.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "runTask" -> schedule((Plugin) args[0], (Runnable) args[1], 0L, 0L);
                    case "runTaskLater" -> {
                        if (args[1] instanceof Runnable r) {
                            yield schedule((Plugin) args[0], r, (long) args[2], 0L);
                        }
                        yield null;
                    }
                    case "runTaskTimer" -> {
                        if (args[1] instanceof Runnable r) {
                            yield schedule((Plugin) args[0], r, (long) args[2], (long) args[3]);
                        }
                        yield null;
                    }
                    case "cancelTask" -> {
                        ScheduledTask t = byId.get((int) args[0]);
                        if (t != null) t.cancel();
                        yield null;
                    }
                    case "cancelTasks" -> {
                        Plugin p = (Plugin) args[0];
                        for (ScheduledTask t : byId.values()) {
                            if (t.owner == p) t.cancel();
                        }
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                }
            );
        }

        static final class ScheduledTask implements BukkitTask {
            final int id;
            final Plugin owner;
            final Runnable runnable;
            long runAtTick;
            final long period;
            boolean cancelled = false;

            ScheduledTask(int id, Plugin owner, Runnable runnable, long runAtTick, long period) {
                this.id = id;
                this.owner = owner;
                this.runnable = runnable;
                this.runAtTick = runAtTick;
                this.period = period;
            }

            @Override public int getTaskId() { return id; }
            @Override public Plugin getOwner() { return owner; }
            @Override public boolean isSync() { return true; }
            @Override public boolean isCancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }
    }

    static final class FakeItemStack extends ItemStack {
        private Material type;
        private int amount;
        private FakeItemMetaState metaState;
        final List<Integer> cmdWriteLog = new ArrayList<>();

        FakeItemStack(Material type, int amount) {
            super();
            this.type = type != null ? type : Material.AIR;
            this.amount = amount;
        }

        @Override public Material getType() { return type; }
        @Override public void setType(Material type) { this.type = type; }
        @Override public int getAmount() { return amount; }
        @Override public void setAmount(int amount) { this.amount = amount; }
        @Override public boolean isEmpty() { return type == Material.AIR || amount <= 0; }
        @Override public boolean hasItemMeta() { return type != Material.AIR && metaState != null; }

        @Override
        public ItemMeta getItemMeta() {
            if (type == Material.AIR) {
                return null;
            }
            return metaState != null ? metaState.copy().asProxy() : FakeItemMetaState.newProxy();
        }

        @Override
        public boolean setItemMeta(ItemMeta itemMeta) {
            if (itemMeta == null) {
                this.metaState = null;
                return true;
            }
            this.metaState = FakeItemMetaState.fromMeta(itemMeta).copy();
            if (this.metaState.customModelData != null) {
                this.cmdWriteLog.add(this.metaState.customModelData);
            }
            return true;
        }

        @Override
        public boolean editMeta(Consumer<? super ItemMeta> consumer) {
            ItemMeta m = getItemMeta();
            if (m == null) {
                return false;
            }
            consumer.accept(m);
            return setItemMeta(m);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <M extends ItemMeta> boolean editMeta(Class<M> metaClass, Consumer<? super M> consumer) {
            ItemMeta m = getItemMeta();
            if (!metaClass.isInstance(m)) {
                return false;
            }
            consumer.accept((M) m);
            return setItemMeta(m);
        }

        @Override
        public PersistentDataContainerView getPersistentDataContainer() {
            return metaState != null ? metaState.pdc : new FakePdc();
        }

        @Override
        public boolean editPersistentDataContainer(Consumer<PersistentDataContainer> consumer) {
            return editMeta(m -> consumer.accept(m.getPersistentDataContainer()));
        }

        @Override
        public ItemStack clone() {
            FakeItemStack copy = new FakeItemStack(type, amount);
            copy.metaState = metaState != null ? metaState.copy() : null;
            return copy;
        }
    }

    static final class FakeItemMetaState {
        String displayName;
        List<String> lore;
        Integer customModelData;
        NamespacedKey itemModel;
        boolean unbreakable;
        final Set<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        final Map<Attribute, List<AttributeModifier>> attributeModifiers = new LinkedHashMap<>();
        final FakePdc pdc = new FakePdc();

        static ItemMeta newProxy() {
            return new FakeItemMetaState().asProxy();
        }

        static FakeItemMetaState fromMeta(ItemMeta meta) {
            FakeItemMetaState st = new FakeItemMetaState();
            if (meta.hasDisplayName()) st.displayName = meta.getDisplayName();
            if (meta.hasLore()) st.lore = meta.getLore();
            if (meta.hasCustomModelData()) st.customModelData = meta.getCustomModelData();
            if (meta.hasItemModel()) st.itemModel = meta.getItemModel();
            st.unbreakable = meta.isUnbreakable();
            st.flags.addAll(meta.getItemFlags());
            if (meta.hasAttributeModifiers() && meta.getAttributeModifiers() != null) {
                for (Map.Entry<Attribute, AttributeModifier> entry : meta.getAttributeModifiers().entries()) {
                    st.attributeModifiers.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
                }
            }
            if (meta.getPersistentDataContainer() instanceof FakePdc fp) {
                st.pdc.data.putAll(fp.data);
            }
            return st;
        }

        FakeItemMetaState copy() {
            FakeItemMetaState c = new FakeItemMetaState();
            c.displayName = this.displayName;
            c.lore = this.lore != null ? new ArrayList<>(this.lore) : null;
            c.customModelData = this.customModelData;
            c.itemModel = this.itemModel;
            c.unbreakable = this.unbreakable;
            c.flags.addAll(this.flags);
            for (Map.Entry<Attribute, List<AttributeModifier>> entry : this.attributeModifiers.entrySet()) {
                c.attributeModifiers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            c.pdc.data.putAll(this.pdc.data);
            return c;
        }

        @SuppressWarnings("unchecked")
        ItemMeta asProxy() {
            return (ItemMeta) Proxy.newProxyInstance(
                AnimationSimulatorTest.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasDisplayName", "hasCustomName" -> displayName != null;
                    case "getDisplayName" -> displayName != null ? displayName : "";
                    case "setDisplayName" -> { displayName = (String) args[0]; yield null; }
                    case "hasLore" -> lore != null && !lore.isEmpty();
                    case "getLore" -> lore != null ? new ArrayList<>(lore) : null;
                    case "setLore" -> { lore = args[0] != null ? new ArrayList<>((List<String>) args[0]) : null; yield null; }
                    case "hasCustomModelData" -> customModelData != null;
                    case "getCustomModelData" -> customModelData != null ? customModelData : 0;
                    case "setCustomModelData" -> { customModelData = (Integer) args[0]; yield null; }
                    case "hasItemModel" -> itemModel != null;
                    case "getItemModel" -> itemModel;
                    case "setItemModel" -> { itemModel = (NamespacedKey) args[0]; yield null; }
                    case "isUnbreakable" -> unbreakable;
                    case "setUnbreakable" -> { unbreakable = (boolean) args[0]; yield null; }
                    case "addItemFlags" -> { Collections.addAll(flags, (ItemFlag[]) args[0]); yield null; }
                    case "removeItemFlags" -> { for (ItemFlag f : (ItemFlag[]) args[0]) flags.remove(f); yield null; }
                    case "getItemFlags" -> EnumSet.copyOf(flags);
                    case "hasItemFlag" -> flags.contains((ItemFlag) args[0]);
                    case "hasAttributeModifiers" -> !attributeModifiers.isEmpty();
                    case "getAttributeModifiers" -> {
                        if (args != null && args.length == 1) {
                            if (args[0] instanceof Attribute attr) {
                                List<AttributeModifier> list = attributeModifiers.get(attr);
                                yield list != null ? new ArrayList<>(list) : null;
                            }
                            Multimap<Attribute, AttributeModifier> slotMap = ArrayListMultimap.create();
                            if (args[0] instanceof EquipmentSlot slot) {
                                for (Map.Entry<Attribute, List<AttributeModifier>> e : attributeModifiers.entrySet()) {
                                    for (AttributeModifier mod : e.getValue()) {
                                        if (mod.getSlotGroup().test(slot)) {
                                            slotMap.put(e.getKey(), mod);
                                        }
                                    }
                                }
                            }
                            yield slotMap;
                        }
                        Multimap<Attribute, AttributeModifier> all = ArrayListMultimap.create();
                        for (Map.Entry<Attribute, List<AttributeModifier>> e : attributeModifiers.entrySet()) {
                            all.putAll(e.getKey(), e.getValue());
                        }
                        yield all;
                    }
                    case "addAttributeModifier" -> {
                        Attribute attr = (Attribute) args[0];
                        AttributeModifier mod = (AttributeModifier) args[1];
                        attributeModifiers.computeIfAbsent(attr, k -> new ArrayList<>()).add(mod);
                        yield true;
                    }
                    case "removeAttributeModifier" -> {
                        if (args != null && args.length == 1 && args[0] instanceof Attribute attr) {
                            yield attributeModifiers.remove(attr) != null;
                        }
                        yield false;
                    }
                    case "getPersistentDataContainer" -> pdc;
                    case "clone" -> copy().asProxy();
                    case "serialize" -> Map.of();
                    default -> defaultValue(method.getReturnType());
                }
            );
        }
    }

    static final class FakePdc implements PersistentDataContainer {
        final Map<NamespacedKey, Object> data = new HashMap<>();

        @Override public <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value) { data.put(key, value); }
        @Override public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) { return data.containsKey(key); }
        @Override public boolean has(NamespacedKey key) { return data.containsKey(key); }
        @Override @SuppressWarnings("unchecked") public <P, C> C get(NamespacedKey key, PersistentDataType<P, C> type) { return (C) data.get(key); }
        @Override public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
            return has(key, type) ? get(key, type) : defaultValue;
        }
        @Override public Set<NamespacedKey> getKeys() { return data.keySet(); }
        @Override public void remove(NamespacedKey key) { data.remove(key); }
        @Override public boolean isEmpty() { return data.isEmpty(); }
        @Override public PersistentDataAdapterContext getAdapterContext() { return null; }
        @Override public byte[] serializeToBytes() { return new byte[0]; }
        @Override public void readFromBytes(byte[] bytes, boolean clear) {}
        @Override public void copyTo(PersistentDataContainer other, boolean replace) {}
    }
}
