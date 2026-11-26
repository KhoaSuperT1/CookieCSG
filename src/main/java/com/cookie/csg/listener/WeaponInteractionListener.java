package com.cookie.csg.listener;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.engine.PlayerWeaponState;
import com.cookie.csg.engine.WeaponState;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class WeaponInteractionListener implements Listener {

    public static final long DOUBLE_SHIFT_WINDOW_MS = 400L;

    private final CookieCSGPlugin plugin;

    public WeaponInteractionListener(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWeaponManager().isWeapon(item)) {
            return;
        }

        String weaponId = plugin.getWeaponManager().getWeaponId(item);
        WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(weaponId);
        if (weapon == null) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            return;
        }

        Action action = event.getAction();
        boolean isLeft = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean isRight = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!isLeft && !isRight) {
            return;
        }

        event.setCancelled(true);
        plugin.getAnimationEngine().applySwingSuppression(player);

        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (isRight && System.currentTimeMillis() - state.getLastInteractEntityMs() < 45L) {
            return;
        }
        if (isLeft && System.currentTimeMillis() - state.getLastDamageEntityMs() < 45L) {
            return;
        }

        if (weapon.getType().isMelee()) {
            if (isLeft) {
                handleMeleeSlash(player, weapon);
            } else {
                handleMeleeHeavy(player, weapon);
            }
            return;
        }

        if (plugin.isRightClickFireForGuns()) {
            if (isRight) {
                handleGunFire(player, weapon, true);
            } else {
                handleGunAds(player, weapon);
            }
        } else {
            if (isLeft) {
                handleGunFire(player, weapon, false);
            } else {
                handleGunAds(player, weapon);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWeaponManager().isWeapon(item)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(item);
        if (weapon == null) {
            return;
        }

        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        state.setLastInteractEntityMs(System.currentTimeMillis());

        if (weapon.getType().isMelee()) {
            handleMeleeHeavy(player, weapon);
        } else if (plugin.isRightClickFireForGuns()) {
            handleGunFire(player, weapon, true);
        } else {
            handleGunAds(player, weapon);
        }
    }

    private void handleMeleeSlash(Player player, WeaponDefinition weapon) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING || state.getCurrentState() == WeaponState.RELOADING) {
            return;
        }
        boolean wasInspecting = state.getCurrentState() == WeaponState.INSPECTING;
        if (wasInspecting) {
            state.interrupt();
        }
        boolean executed = plugin.getMeleeEngine().executeSlash(player, weapon);
        if (wasInspecting && !executed) {
            plugin.getAnimationEngine().restoreCurrentBaseModel(player, weapon);
        }
    }

    private void handleMeleeHeavy(Player player, WeaponDefinition weapon) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING || state.getCurrentState() == WeaponState.RELOADING) {
            return;
        }
        boolean wasInspecting = state.getCurrentState() == WeaponState.INSPECTING;
        if (wasInspecting) {
            state.interrupt();
        }
        boolean executed = false;
        if (weapon.getType() == WeaponType.KNIFE) {
            executed = plugin.getMeleeEngine().executeStab(player, weapon);
        } else if (weapon.getType() == WeaponType.AXE) {
            executed = plugin.getMeleeEngine().executeAxeCleave(player, weapon);
        }
        if (wasInspecting && !executed) {
            plugin.getAnimationEngine().restoreCurrentBaseModel(player, weapon);
        }
    }

    private void handleGunFire(Player player, WeaponDefinition weapon, boolean allowAutoFill) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING) {
            return;
        }
        if (state.getCurrentState() == WeaponState.RELOADING && !plugin.getAnimationEngine().canInterruptReloadForFire(state, weapon)) {
            return;
        }

        FireMode mode = state.getFireMode(weapon);
        boolean isAds = plugin.getAdsManager().isAiming(player);

        if (mode == FireMode.SINGLE || mode == FireMode.SEMI_AUTO) {
            state.cancelAutoFire();
            state.setLastRightClickMs(0L);
            state.setLastFireClickMs(0L);
            plugin.getRecoilEngine().executeFire(player, weapon, isAds, mode, false);
            return;
        }

        long now = System.currentTimeMillis();
        int step = Math.max(1, weapon.getStats() != null ? weapon.getStats().getFireRateTicks() : 2);
        int windowTicks = (allowAutoFill && plugin.isRightClickFireForGuns()) ? Math.max(6, step + 4) : Math.max(5, step * 2 + 2);
        state.setAutoFireRemainingTicks(windowTicks);
        BukkitTask existingAuto = state.getAutoFireTask();
        if (existingAuto != null && !existingAuto.isCancelled() && weapon.getId().equalsIgnoreCase(state.getActiveWeaponId())) {
            state.setAutoFireRemainingTicks(windowTicks);
            if (allowAutoFill && plugin.isRightClickFireForGuns()) {
                state.setLastRightClickMs(now);
            } else {
                state.setLastFireClickMs(now);
            }
            return;
        }
        boolean fired = plugin.getRecoilEngine().executeFire(player, weapon, isAds, FireMode.AUTO, false);
        if (fired) {
            if (allowAutoFill && plugin.isRightClickFireForGuns()) {
                state.setLastRightClickMs(now);
            } else {
                state.setLastFireClickMs(now);
            }
            startContinuousAutoFire(player, weapon, state, step);
        }
    }

    private void startContinuousAutoFire(Player player, WeaponDefinition weapon, PlayerWeaponState state, int stepTicks) {
        int step = Math.max(1, stepTicks);
        String expectedWeaponId = weapon.getId();
        int clipSize = weapon.getStats() != null ? weapon.getStats().getClipSize() : 0;
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                state.cancelAutoFire();
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            String curId = plugin.getWeaponManager().getWeaponId(held);
            if (curId == null || !curId.equalsIgnoreCase(expectedWeaponId)) {
                state.cancelAutoFire();
                return;
            }
            WeaponState cur = state.getCurrentState();
            if (cur == WeaponState.DRAWING || cur == WeaponState.RELOADING || cur == WeaponState.INSPECTING) {
                state.cancelAutoFire();
                return;
            }
            if (state.getAmmoInClip(weapon.getId(), clipSize) <= 0) {
                state.cancelAutoFire();
                return;
            }
            int rem = state.getAutoFireRemainingTicks() - step;
            state.setAutoFireRemainingTicks(rem);
            boolean curAds = plugin.getAdsManager().isAiming(player);
            plugin.getRecoilEngine().executeFire(player, weapon, curAds, FireMode.AUTO, true);
            if (state.getAutoFireRemainingTicks() <= 0) {
                state.cancelAutoFire();
            }
        }, step, step);
        state.setAutoFireTask(task);
    }

    private void handleGunAds(Player player, WeaponDefinition weapon) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING || state.getCurrentState() == WeaponState.RELOADING) {
            return;
        }
        boolean wasInspecting = state.getCurrentState() == WeaponState.INSPECTING;
        if (wasInspecting) {
            state.interrupt();
        }
        boolean useHold = plugin.isHoldToAds() && !plugin.isRightClickFireForGuns();
        if (!useHold) {
            state.cancelAutoFire();
            state.setLastRightClickMs(0L);
            state.setLastFireClickMs(0L);
        }
        if (weapon.getAdsConfig() != null && !weapon.getAdsConfig().isEnabled()) {
            if (wasInspecting) {
                plugin.getAnimationEngine().restoreCurrentBaseModel(player, weapon);
            }
            return;
        }
        if (useHold) {
            plugin.getAdsManager().handleHoldAdsPulse(player, weapon);
        } else {
            plugin.getAdsManager().toggleAds(player, weapon);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getWeaponManager().isWeapon(item)) {
            return;
        }
        WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(item);
        if (weapon == null || !weapon.getType().isGun()) {
            return;
        }
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        long prev = state.getLastSneakPressMs();
        if (prev > 0L && (now - prev) <= DOUBLE_SHIFT_WINDOW_MS) {
            state.setLastSneakPressMs(0L);
            state.cancelAutoFire();
            state.resetSprayState();
            List<FireMode> supported = weapon.getStats() != null
                ? weapon.getStats().resolveSupportedFireModes(weapon.getType())
                : List.of();
            if (supported.size() <= 1) {
                FireMode only = supported.isEmpty() ? FireMode.SINGLE : supported.get(0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.7f);
                player.sendActionBar(Component.text("⚙ Fire Mode: " + only.getDisplayName() + " (fixed)", NamedTextColor.GRAY));
                return;
            }
            FireMode next = state.cycleFireMode(weapon);
            if (next != null) {
                float pitch = switch (next) {
                    case AUTO -> 1.6f;
                    case SEMI_AUTO -> 1.2f;
                    case SINGLE -> 1.0f;
                };
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey("csgo.weapon.fireselect"), 0.9f, pitch);
                Component msg = switch (next) {
                    case AUTO -> Component.text("⚙ Fire Mode: AUTO", NamedTextColor.GOLD);
                    case SEMI_AUTO -> Component.text("⚙ Fire Mode: SEMI-AUTO", NamedTextColor.AQUA);
                    case SINGLE -> Component.text("⚙ Fire Mode: SINGLE", NamedTextColor.YELLOW);
                };
                player.sendActionBar(msg);
            }
        } else {
            state.setLastSneakPressMs(now);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!plugin.getWeaponManager().isWeapon(item)) {
            return;
        }

        event.setCancelled(true);
        plugin.getAnimationEngine().applySwingSuppression(player);
        String weaponId = plugin.getWeaponManager().getWeaponId(item);
        WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(weaponId);
        if (weapon != null) {
            if (player.isSneaking() && weapon.getType().isGun()) {
                plugin.getAnimationEngine().playReloadAnimation(player, weapon, item);
            } else {
                plugin.getAnimationEngine().startInspect(player, weapon);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();

        if (!plugin.getWeaponManager().isWeapon(dropped)) {
            return;
        }

        WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(dropped);
        if (weapon != null) {
            plugin.getAnimationEngine().resetIdleModel(dropped, weapon);
            event.getItemDrop().setItemStack(dropped);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        String heldId = plugin.getWeaponManager().getWeaponId(held);
        PlayerWeaponState state = plugin.getAnimationEngine().getState(player.getUniqueId());
        boolean droppedActiveHeld = !plugin.getWeaponManager().isWeapon(held)
            || (weapon != null && state != null
                && weapon.getId().equalsIgnoreCase(state.getActiveWeaponId())
                && !weapon.getId().equalsIgnoreCase(heldId));
        if (droppedActiveHeld) {
            plugin.getAdsManager().cleanup(player);
            plugin.getAnimationEngine().cancelAnimation(player);
            if (state != null) {
                state.setLastSneakPressMs(0L);
                state.setLastFireClickMs(0L);
                state.resetSprayState();
            }
            if (!plugin.getWeaponManager().isWeapon(held)) {
                plugin.getAnimationEngine().clearSwingSuppression(player);
            }
        }
        scheduleHeldWeaponSync(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        plugin.getAdsManager().cleanup(player);
        plugin.getAnimationEngine().cancelAnimation(player);
        PlayerWeaponState heldState = plugin.getAnimationEngine().getState(player.getUniqueId());
        if (heldState != null) {
            heldState.setLastSneakPressMs(0L);
            heldState.setLastFireClickMs(0L);
            heldState.resetSprayState();
        }

        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        if (plugin.getWeaponManager().isWeapon(previousItem)) {
            WeaponDefinition previousWeapon = plugin.getWeaponManager().getWeapon(previousItem);
            if (previousWeapon != null) {
                plugin.getAnimationEngine().resetIdleModel(previousItem, previousWeapon);
                player.getInventory().setItem(event.getPreviousSlot(), previousItem);
            }
        }

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (plugin.getWeaponManager().isWeapon(newItem)) {
            plugin.getAnimationEngine().applySwingSuppression(player);
            String weaponId = plugin.getWeaponManager().getWeaponId(newItem);
            WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(weaponId);
            if (weapon != null) {
                plugin.getAnimationEngine().startDeploy(player, weapon, newItem);
                player.getInventory().setItem(event.getNewSlot(), newItem);
            }
        } else {
            plugin.getAnimationEngine().clearSwingSuppression(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getAdsManager().sanitizeStaleZoomState(player);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (plugin.getWeaponManager().isWeapon(held)) {
            plugin.getAnimationEngine().applySwingSuppression(player);
            WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(held);
            if (weapon != null && weapon.getAnimation() != null) {
                plugin.getWeaponManager().applyWeaponVisual(held, weapon, weapon.getAnimation().getIdleModelData());
                PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
                state.setActiveWeaponId(weapon.getId());
                if (weapon.getStats() != null) {
                    state.getAmmoInClip(weapon.getId(), weapon.getStats().getClipSize());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAdsManager().cleanup(player);
        plugin.getAnimationEngine().clearSwingSuppression(player);
        plugin.getAnimationEngine().removeState(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && plugin.getWeaponManager().isWeapon(event.getItem().getItemStack())) {
            scheduleHeldWeaponSync(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleHeldWeaponSync(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        scheduleHeldWeaponSync(event.getPlayer());
    }

    private void scheduleHeldWeaponSync(Player player) {
        if (player == null || plugin == null || plugin.getAnimationEngine() == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getAnimationEngine().syncHeldWeaponState(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (plugin.isApplyingWeaponDamage(player.getUniqueId())) {
                return;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (plugin.getWeaponManager().isWeapon(item)) {
                event.setCancelled(true);
                plugin.getAnimationEngine().applySwingSuppression(player);
                String weaponId = plugin.getWeaponManager().getWeaponId(item);
                WeaponDefinition weapon = plugin.getWeaponManager().getWeapon(weaponId);
                if (weapon != null) {
                    PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
                    state.setLastDamageEntityMs(System.currentTimeMillis());
                    if (weapon.getType().isMelee()) {
                        handleMeleeSlash(player, weapon);
                    } else if (plugin.isRightClickFireForGuns()) {
                        handleGunAds(player, weapon);
                    } else {
                        handleGunFire(player, weapon, false);
                    }
                }
            }
        }
    }
}
