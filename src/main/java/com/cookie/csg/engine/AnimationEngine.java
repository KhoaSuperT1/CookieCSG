package com.cookie.csg.engine;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.WeaponAnimation;
import com.cookie.csg.model.WeaponDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationEngine {

    public static final int SWING_SUPPRESS_HASTE_AMPLIFIER = 6;

    public enum MeleeAttackType {
        SLASH_1,
        SLASH_2,
        HEAVY
    }

    private final CookieCSGPlugin plugin;
    private final AdsManager adsManager;
    private final Map<UUID, PlayerWeaponState> playerStates;

    public AnimationEngine(CookieCSGPlugin plugin, AdsManager adsManager) {
        this.plugin = plugin;
        this.adsManager = adsManager;
        this.playerStates = new ConcurrentHashMap<>();
    }

    public PlayerWeaponState getOrCreateState(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, PlayerWeaponState::new);
    }

    public PlayerWeaponState getState(UUID uuid) {
        return playerStates.get(uuid);
    }

    public void removeState(UUID uuid) {
        clearSwingSuppression(Bukkit.getPlayer(uuid));
        PlayerWeaponState state = playerStates.remove(uuid);
        if (state != null) {
            state.cancelAdsHold();
            state.cancelAutoFire();
            state.resetSprayState();
            state.interrupt();
        }
    }

    public void cleanupAll() {
        for (UUID uuid : playerStates.keySet()) {
            clearSwingSuppression(Bukkit.getPlayer(uuid));
        }
        for (PlayerWeaponState state : playerStates.values()) {
            state.cancelAdsHold();
            state.cancelAutoFire();
            state.resetSprayState();
            state.interrupt();
        }
        playerStates.clear();
    }

    public void applySwingSuppression(Player player) {
        if (player == null) {
            return;
        }
        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        PotionEffect existing = player.getPotionEffect(PotionEffectType.HASTE);
        if (!state.isSwingSuppressed()) {
            if (existing != null && !isSuppressorEffect(existing)) {
                state.setSavedHasteEffect(existing);
            }
        }
        if (existing == null || !isSuppressorEffect(existing)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, SWING_SUPPRESS_HASTE_AMPLIFIER, true, false, false));
        }
        state.setSwingSuppressed(true);
    }

    public void clearSwingSuppression(Player player) {
        if (player == null) {
            return;
        }
        PlayerWeaponState state = getState(player.getUniqueId());
        if (state == null || !state.isSwingSuppressed()) {
            return;
        }
        state.setSwingSuppressed(false);
        PotionEffect existing = player.getPotionEffect(PotionEffectType.HASTE);
        if (existing != null && isSuppressorEffect(existing)) {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
        PotionEffect saved = state.getSavedHasteEffect();
        state.setSavedHasteEffect(null);
        if (saved != null) {
            player.addPotionEffect(saved);
        }
    }

    public void syncHeldWeaponState(Player player) {
        if (player == null) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (plugin != null && plugin.getWeaponManager() != null && plugin.getWeaponManager().isWeapon(held)) {
            WeaponDefinition w = plugin.getWeaponManager().getWeapon(held);
            PlayerWeaponState state = getOrCreateState(player.getUniqueId());
            if (w != null && w.getAnimation() != null && state.getCurrentState() == WeaponState.IDLE && !state.isAiming()) {
                plugin.getWeaponManager().applyWeaponVisual(held, w, w.getAnimation().getIdleModelData());
            }
            applySwingSuppression(player);
        } else {
            clearSwingSuppression(player);
        }
    }

    private static boolean isSuppressorEffect(PotionEffect effect) {
        return effect != null
            && PotionEffectType.HASTE.equals(effect.getType())
            && effect.getAmplifier() == SWING_SUPPRESS_HASTE_AMPLIFIER
            && effect.getDuration() == PotionEffect.INFINITE_DURATION;
    }

    public void startDeploy(Player player, WeaponDefinition weapon) {
        startDeploy(player, weapon, player != null ? player.getInventory().getItemInMainHand() : null);
    }

    public void startDeploy(Player player, WeaponDefinition weapon, ItemStack targetItem) {
        if (player == null || weapon == null) {
            return;
        }

        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        state.setActiveWeaponId(weapon.getId());
        if (weapon.getStats() != null) {
            int clipAmmo = state.getAmmoInClip(weapon.getId(), weapon.getStats().getClipSize());
            state.setAmmoInClip(weapon.getId(), clipAmmo);
        }

        state.cancelAutoFire();
        state.setLastRightClickMs(0L);
        state.setMeleeComboStep(0);
        state.interrupt();
        if (weapon.isDefaultMode()) {
            state.setCurrentState(WeaponState.IDLE);
            if (weapon.getAudio() != null && weapon.getAudio().getDeploy() != null && !weapon.getAudio().getDeploy().isEmpty()) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getDeploy()), 1.0f, 1.0f);
            }
            if (targetItem != null && plugin != null && plugin.getWeaponManager() != null) {
                plugin.getWeaponManager().applyWeaponVisual(targetItem, weapon, 0);
            }
            return;
        }
        final long token = state.getActionToken();
        state.setCurrentState(WeaponState.DRAWING);

        List<Integer> deployFrames = weapon.getAnimation().getDeployFrames();
        int rate = Math.max(1, weapon.getAnimation().getDeployTickRate());
        int deployTicks = weapon.getAnimation().getDeployTicks() > 0
            ? weapon.getAnimation().getDeployTicks()
            : (deployFrames != null && !deployFrames.isEmpty() ? deployFrames.size() * rate : 6);

        if (weapon.getAudio() != null && weapon.getAudio().getDeploy() != null && !weapon.getAudio().getDeploy().isEmpty()) {
            player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getDeploy()), 1.0f, 1.0f);
        }

        if (deployFrames != null && !deployFrames.isEmpty()) {
            int first = deployFrames.get(0);
            if (first > 0) {
                setItemCustomModelData(targetItem, weapon, first);
            } else {
                resetIdleModel(targetItem, weapon);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private int tick = 1;
                private int lastFrameIdx = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.DRAWING) {
                        return;
                    }
                    if (tick >= deployTicks) {
                        state.interrupt();
                        restoreCurrentBaseModel(player, weapon);
                        return;
                    }
                    int frameIdx = Math.min(deployFrames.size() - 1, (int) (((long) tick * deployFrames.size()) / deployTicks));
                    if (frameIdx != lastFrameIdx) {
                        lastFrameIdx = frameIdx;
                        int model = deployFrames.get(frameIdx);
                        if (model > 0) {
                            setItemCustomModelData(player, weapon, model);
                        }
                    }
                    tick++;
                }
            }, 1L, 1L);
            state.setActiveTask(task);
        } else {
            int deployModel = weapon.getAnimation().getDeployModelData();
            if (deployModel > 0) {
                setItemCustomModelData(targetItem, weapon, deployModel);
            } else {
                resetIdleModel(targetItem, weapon);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.DRAWING) {
                    return;
                }
                state.setCurrentState(state.isAiming() ? WeaponState.AIMING_ADS : WeaponState.IDLE);
                restoreCurrentBaseModel(player, weapon);
            }, deployTicks);

            state.setActiveTask(task);
        }
    }

    public void startInspect(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null || weapon.isDefaultMode()) {
            return;
        }

        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING || state.getCurrentState() == WeaponState.RELOADING) {
            return;
        }

        if (state.getCurrentState() == WeaponState.INSPECTING) {
            state.interrupt();
            restoreCurrentBaseModel(player, weapon);
            return;
        }

        if (state.isAiming() || (adsManager != null && adsManager.isAiming(player))) {
            if (adsManager != null) {
                adsManager.clearAimingSilently(player);
            } else {
                state.setAiming(false);
            }
        }

        state.cancelAutoFire();
        state.setLastRightClickMs(0L);
        state.interrupt();
        final long token = state.getActionToken();
        state.setCurrentState(WeaponState.INSPECTING);

        List<Integer> frames = weapon.getAnimation().getInspectFrames();
        if (frames == null || frames.isEmpty()) {
            state.setCurrentState(WeaponState.IDLE);
            restoreCurrentBaseModel(player, weapon);
            if (weapon.getAudio() != null && weapon.getAudio().getInspect() != null && !weapon.getAudio().getInspect().isEmpty()) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getInspect()), 1.0f, 1.0f);
            }
            return;
        }

        int rate = weapon.getAnimation().getInspectTickRate() > 0 ? weapon.getAnimation().getInspectTickRate() : 1;
        if (weapon.getAudio() != null && weapon.getAudio().getInspect() != null && !weapon.getAudio().getInspect().isEmpty()) {
            player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getInspect()), 1.0f, 1.0f);
        }

        int firstModel = frames.get(0);
        if (firstModel > 0) {
            setItemCustomModelData(player, weapon, firstModel);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int frameIndex = 1;

            @Override
            public void run() {
                if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.INSPECTING) {
                    return;
                }

                if (frameIndex < frames.size()) {
                    int model = frames.get(frameIndex);
                    if (model > 0) {
                        setItemCustomModelData(player, weapon, model);
                    }
                    frameIndex++;
                } else {
                    state.interrupt();
                    restoreCurrentBaseModel(player, weapon);
                }
            }
        }, rate, rate);

        state.setActiveTask(task);
    }

    public void cancelAnimation(Player player) {
        if (player == null) {
            return;
        }
        PlayerWeaponState state = getState(player.getUniqueId());
        if (state != null) {
            state.cancelAutoFire();
            state.setLastRightClickMs(0L);
            state.interrupt();
        }
    }

    public void playAdsTransition(Player player, WeaponDefinition weapon, boolean aiming) {
        if (player == null || weapon == null) {
            return;
        }
        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        state.interrupt();
        state.setAiming(aiming);
        WeaponState targetState = aiming ? WeaponState.AIMING_ADS : WeaponState.IDLE;
        state.setCurrentState(targetState);
        if (weapon.isDefaultMode()) {
            return;
        }
        final long token = state.getActionToken();

        List<Integer> frames = aiming ? weapon.getAnimation().getAdsInFrames() : weapon.getAnimation().getAdsOutFrames();
        int settleModel = aiming ? weapon.getAnimation().getAdsModelData() : weapon.getAnimation().getIdleModelData();
        if (frames == null || frames.isEmpty()) {
            if (settleModel > 0) {
                setItemCustomModelData(player, weapon, settleModel);
            } else {
                restoreCurrentBaseModel(player, weapon);
            }
            return;
        }

        long stepTicks = Math.max(1L, weapon.getAnimation().getAdsTickRate());
        int firstFrame = frames.get(0);
        if (firstFrame > 0) {
            setItemCustomModelData(player, weapon, firstFrame);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = 1;

            @Override
            public void run() {
                if (!player.isOnline() || state.getActionToken() != token || state.isAiming() != aiming || state.getCurrentState() != targetState) {
                    return;
                }
                if (index < frames.size()) {
                    int model = frames.get(index);
                    if (model > 0) {
                        setItemCustomModelData(player, weapon, model);
                    }
                    index++;
                } else {
                    state.interrupt();
                    restoreCurrentBaseModel(player, weapon);
                }
            }
        }, stepTicks, stepTicks);
        state.setActiveTask(task);
    }

    public void playFireAnimation(Player player, WeaponDefinition weapon, boolean isAds) {
        playFireAnimation(player, weapon, isAds, false);
    }

    public void playFireAnimation(Player player, WeaponDefinition weapon, boolean isAds, boolean continuousShot) {
        if (player == null || weapon == null || weapon.isDefaultMode()) {
            return;
        }

        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        boolean effectiveAds = isAds || (adsManager != null && adsManager.isAiming(player));
        if (adsManager != null && adsManager.isAiming(player) && !state.isAiming()) {
            state.setAiming(true);
        }
        state.interrupt();
        final long token = state.getActionToken();
        state.setCurrentState(WeaponState.FIRING);
        long stepTicks = Math.max(1L, weapon.getAnimation().getFireTickRate());

        if (effectiveAds) {
            List<Integer> adsFireFrames = weapon.getAnimation().getAdsFireFrames();
            if (adsFireFrames != null && !adsFireFrames.isEmpty()) {
                int startIndex = (continuousShot && adsFireFrames.size() >= 3) ? 1 : 0;
                BukkitTask task = playFrameSequence(player, weapon, state, token, adsFireFrames, stepTicks, startIndex);
                state.setActiveTask(task);
                return;
            }
            int fireModel = weapon.getAnimation().getAdsFireModelData();
            if (fireModel <= 0) {
                fireModel = weapon.getAnimation().getAdsModelData();
            }
            if (fireModel > 0) {
                setItemCustomModelData(player, weapon, fireModel);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.FIRING) {
                    return;
                }
                state.interrupt();
                restoreCurrentBaseModel(player, weapon);
            }, 2L);
            state.setActiveTask(task);
        } else {
            List<Integer> fireFrames = weapon.getAnimation().getFireFrames();
            if (fireFrames != null && !fireFrames.isEmpty()) {
                int startIndex = (continuousShot && fireFrames.size() >= 3) ? 1 : 0;
                BukkitTask task = playFrameSequence(player, weapon, state, token, fireFrames, stepTicks, startIndex);
                state.setActiveTask(task);
            } else {
                state.interrupt();
                restoreCurrentBaseModel(player, weapon);
            }
        }
    }

    public boolean canInterruptReloadForFire(PlayerWeaponState state, WeaponDefinition weapon) {
        if (state == null || weapon == null || weapon.getAnimation() == null) {
            return false;
        }
        List<Integer> loopFrames = weapon.getAnimation().getReloadLoopFrames();
        int clipSize = weapon.getStats() != null ? weapon.getStats().getClipSize() : 0;
        return state.getCurrentState() == WeaponState.RELOADING
            && loopFrames != null
            && !loopFrames.isEmpty()
            && state.getAmmoInClip(weapon.getId(), clipSize) > 0;
    }

    public void playReloadAnimation(Player player, WeaponDefinition weapon) {
        playReloadAnimation(player, weapon, player != null ? player.getInventory().getItemInMainHand() : null);
    }

    public void playReloadAnimation(Player player, WeaponDefinition weapon, ItemStack targetItem) {
        if (player == null || weapon == null) {
            return;
        }

        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.RELOADING) {
            return;
        }

        state.setActiveWeaponId(weapon.getId());
        if (weapon.getStats() != null) {
            state.getAmmoInClip(weapon.getId(), weapon.getStats().getClipSize());
        }
        if (state.isAiming() || (adsManager != null && adsManager.isAiming(player))) {
            if (adsManager != null) {
                adsManager.clearAimingSilently(player);
            } else {
                state.setAiming(false);
            }
        }
        state.cancelAutoFire();
        state.setLastRightClickMs(0L);
        state.interrupt();

        final long token = state.getActionToken();
        state.setCurrentState(WeaponState.RELOADING);

        if (weapon.getAudio() != null && weapon.getAudio().getReloadClipOut() != null && !weapon.getAudio().getReloadClipOut().isEmpty()) {
            player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getReloadClipOut()), 1.0f, 1.0f);
        }

        WeaponAnimation anim = weapon.getAnimation();
        if (!weapon.isDefaultMode() && anim != null && anim.getReloadLoopFrames() != null && !anim.getReloadLoopFrames().isEmpty()) {
            playShotgunLoopReload(player, weapon, state, token, anim, targetItem);
        } else {
            playLinearReload(player, weapon, state, token, anim, targetItem);
        }
    }

    private void playLinearReload(Player player, WeaponDefinition weapon, PlayerWeaponState state, long token, WeaponAnimation anim, ItemStack targetItem) {
        List<Integer> tacticalFrames = (!weapon.isDefaultMode() && anim != null) ? anim.getReloadTacticalFrames() : List.of();
        int currentAmmo = weapon.getStats() != null
            ? state.getAmmoInClip(weapon.getId(), weapon.getStats().getClipSize())
            : state.getAmmoInClip();
        boolean useTactical = !weapon.isDefaultMode() && currentAmmo > 0 && tacticalFrames != null && !tacticalFrames.isEmpty();
        List<Integer> reloadFrames = weapon.isDefaultMode()
            ? List.of()
            : (useTactical ? tacticalFrames : (anim != null ? anim.getReloadFrames() : List.of()));
        int tickRate = Math.max(1, anim != null ? anim.getReloadTickRate() : 1);

        int configuredTicks = useTactical ? weapon.getStats().getReloadTacticalTicks() : weapon.getStats().getReloadTicks();
        if (configuredTicks <= 0 && useTactical) {
            configuredTicks = !reloadFrames.isEmpty() ? reloadFrames.size() * tickRate : weapon.getStats().getReloadTicks();
        }
        int reloadTicks = configuredTicks > 0
            ? configuredTicks
            : (reloadFrames != null && !reloadFrames.isEmpty() ? reloadFrames.size() * tickRate : 40);

        int rawClipIn = weapon.getAudio() != null ? weapon.getAudio().getReloadClipInTick() : -1;
        long clipInDelay = rawClipIn >= 0
            ? Math.min(Math.max(1L, reloadTicks - 1L), rawClipIn)
            : Math.max(1L, reloadTicks / 2L);
        scheduleReloadSound(player, state, token, weapon.getAudio() != null ? weapon.getAudio().getReloadClipIn() : "", clipInDelay);

        int rawBolt = weapon.getAudio() != null ? weapon.getAudio().getReloadBoltTick() : -1;
        if (!useTactical || (rawBolt >= 0 && rawBolt < reloadTicks)) {
            long boltDelay = rawBolt >= 0
                ? Math.min(Math.max(1L, reloadTicks - 1L), rawBolt)
                : Math.max(2L, (long) (reloadTicks * 0.75));
            scheduleReloadSound(player, state, token, weapon.getAudio() != null ? weapon.getAudio().getReloadBolt() : "", boltDelay);
        }

        if (reloadFrames != null && !reloadFrames.isEmpty()) {
            int firstReload = reloadFrames.get(0);
            if (firstReload > 0) {
                ItemStack initItem = (targetItem != null && targetItem.getType() != Material.AIR)
                    ? targetItem
                    : player.getInventory().getItemInMainHand();
                setItemCustomModelData(initItem, weapon, firstReload);
            }
            BukkitTask frameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private int tick = 1;
                private int lastIdx = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.RELOADING) {
                        return;
                    }
                    if (tick >= reloadTicks) {
                        return;
                    }
                    int sampleTick = tickRate > 1 ? (tick / tickRate) * tickRate : tick;
                    int idx = Math.min(reloadFrames.size() - 1, (int) (((long) sampleTick * reloadFrames.size()) / reloadTicks));
                    if (idx != lastIdx) {
                        lastIdx = idx;
                        int model = reloadFrames.get(idx);
                        if (model > 0) {
                            setItemCustomModelData(player, weapon, model);
                        }
                    }
                    tick++;
                }
            }, 1L, 1L);
            state.setActiveTask(frameTask);
        } else {
            restoreCurrentBaseModel(player, weapon);
        }

        scheduleReloadFinish(player, weapon, state, token, reloadTicks);
    }

    private void playShotgunLoopReload(Player player, WeaponDefinition weapon, PlayerWeaponState state, long token, WeaponAnimation anim, ItemStack targetItem) {
        int tickRate = Math.max(1, anim.getReloadTickRate());
        int clipSize = Math.max(1, weapon.getStats().getClipSize());
        int currentAmmo = state.getAmmoInClip(weapon.getId(), clipSize);
        int shellsToLoad = Math.max(1, clipSize - currentAmmo);

        List<Integer> intro = anim.getReloadIntroFrames();
        List<Integer> loop = anim.getReloadLoopFrames();
        List<Integer> end = anim.getReloadEndFrames();

        int introTicks = (intro != null && !intro.isEmpty())
            ? (anim.getReloadIntroTicks() > 0 ? anim.getReloadIntroTicks() : intro.size() * tickRate)
            : 0;
        int loopTicks = Math.max(1, anim.getReloadLoopTicks() > 0 ? anim.getReloadLoopTicks() : loop.size() * tickRate);
        int endTicks = (end != null && !end.isEmpty())
            ? (anim.getReloadEndTicks() > 0 ? anim.getReloadEndTicks() : end.size() * tickRate)
            : 0;
        int totalTicks = Math.max(1, introTicks + shellsToLoad * loopTicks + endTicks);

        int rawClipIn = weapon.getAudio() != null ? weapon.getAudio().getReloadClipInTick() : -1;
        long shellSoundOffset = rawClipIn >= 0
            ? Math.min(Math.max(1L, loopTicks - 1L), rawClipIn)
            : Math.max(1L, loopTicks / 2L);
        String clipInSound = weapon.getAudio() != null ? weapon.getAudio().getReloadClipIn() : "";

        for (int s = 0; s < shellsToLoad; s++) {
            long shellStart = introTicks + (long) s * loopTicks;
            scheduleReloadSound(player, state, token, clipInSound, Math.max(1L, shellStart + shellSoundOffset));
            long shellDoneTick = shellStart + loopTicks;
            if (shellDoneTick < totalTicks) {
                BukkitTask shellAmmoTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && state.getActionToken() == token && state.getCurrentState() == WeaponState.RELOADING) {
                        int updated = Math.min(clipSize, state.getAmmoInClip(weapon.getId(), clipSize) + 1);
                        state.setAmmoInClip(weapon.getId(), updated);
                        player.sendActionBar(Component.text("✦ " + updated + " / " + weapon.getStats().getMaxAmmo(), NamedTextColor.YELLOW));
                    }
                }, shellDoneTick);
                state.addPendingTask(shellAmmoTask);
            }
        }

        if (endTicks > 0) {
            int rawBolt = weapon.getAudio() != null ? weapon.getAudio().getReloadBoltTick() : -1;
            long endStart = introTicks + (long) shellsToLoad * loopTicks;
            long boltOffset = rawBolt >= 0
                ? Math.min(Math.max(1L, endTicks - 1L), rawBolt)
                : Math.max(1L, endTicks / 2L);
            scheduleReloadSound(player, state, token, weapon.getAudio() != null ? weapon.getAudio().getReloadBolt() : "", Math.max(1L, endStart + boltOffset));
        }

        int firstModel = resolveShotgunLoopModel(0, intro, introTicks, loop, loopTicks, shellsToLoad, end, endTicks);
        if (firstModel > 0) {
            ItemStack initItem = (targetItem != null && targetItem.getType() != Material.AIR)
                ? targetItem
                : player.getInventory().getItemInMainHand();
            setItemCustomModelData(initItem, weapon, firstModel);
        }

        BukkitTask frameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int tick = 1;
            private int lastModel = firstModel;

            @Override
            public void run() {
                if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.RELOADING) {
                    return;
                }
                if (tick >= totalTicks) {
                    return;
                }
                int sampleTick = tickRate > 1 ? (tick / tickRate) * tickRate : tick;
                int model = resolveShotgunLoopModel(sampleTick, intro, introTicks, loop, loopTicks, shellsToLoad, end, endTicks);
                if (model > 0 && model != lastModel) {
                    lastModel = model;
                    setItemCustomModelData(player, weapon, model);
                }
                tick++;
            }
        }, 1L, 1L);
        state.setActiveTask(frameTask);

        scheduleReloadFinish(player, weapon, state, token, totalTicks);
    }

    private int resolveShotgunLoopModel(int tick, List<Integer> intro, int introTicks, List<Integer> loop, int loopTicks, int shellsToLoad, List<Integer> end, int endTicks) {
        if (introTicks > 0 && tick < introTicks && intro != null && !intro.isEmpty()) {
            int idx = Math.min(intro.size() - 1, (int) (((long) tick * intro.size()) / introTicks));
            return intro.get(idx);
        }
        int loopTotal = shellsToLoad * loopTicks;
        if (tick < introTicks + loopTotal) {
            int inShell = (tick - introTicks) % loopTicks;
            int idx = Math.min(loop.size() - 1, (int) (((long) inShell * loop.size()) / loopTicks));
            return loop.get(idx);
        }
        if (endTicks > 0 && end != null && !end.isEmpty()) {
            int endRel = tick - (introTicks + loopTotal);
            int idx = Math.min(end.size() - 1, (int) (((long) endRel * end.size()) / endTicks));
            return end.get(idx);
        }
        return loop.get(loop.size() - 1);
    }

    private void scheduleReloadSound(Player player, PlayerWeaponState state, long token, String soundKey, long delayTicks) {
        if (soundKey == null || soundKey.isEmpty()) {
            return;
        }
        BukkitTask soundTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && state.getActionToken() == token && state.getCurrentState() == WeaponState.RELOADING) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(soundKey), 1.0f, 1.0f);
            }
        }, Math.max(1L, delayTicks));
        state.addPendingTask(soundTask);
    }

    private void scheduleReloadFinish(Player player, WeaponDefinition weapon, PlayerWeaponState state, long token, long delayTicks) {
        BukkitTask finishTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.RELOADING) {
                return;
            }
            int fullClip = weapon.getStats().getClipSize();
            state.setAmmoInClip(weapon.getId(), fullClip);
            state.interrupt();
            restoreCurrentBaseModel(player, weapon);
            player.sendActionBar(Component.text("✦ " + fullClip + " / " + weapon.getStats().getMaxAmmo(), NamedTextColor.GREEN));
        }, Math.max(1L, delayTicks));
        state.addPendingTask(finishTask);
    }

    public void playMeleeAnimation(Player player, WeaponDefinition weapon, boolean isHeavy) {
        playMeleeAnimation(player, weapon, isHeavy ? MeleeAttackType.HEAVY : MeleeAttackType.SLASH_1);
    }

    public void playMeleeAnimation(Player player, WeaponDefinition weapon, MeleeAttackType attackType) {
        if (player == null || weapon == null || weapon.isDefaultMode() || weapon.getAnimation() == null) {
            return;
        }

        PlayerWeaponState state = getOrCreateState(player.getUniqueId());
        state.interrupt();
        final long token = state.getActionToken();
        state.setCurrentState(WeaponState.FIRING);

        WeaponAnimation anim = weapon.getAnimation();
        List<Integer> frames = new ArrayList<>();
        MeleeAttackType resolvedType = attackType != null ? attackType : MeleeAttackType.SLASH_1;

        if (resolvedType == MeleeAttackType.HEAVY) {
            List<Integer> heavy = anim.resolveHeavyFrames();
            if (heavy != null && !heavy.isEmpty()) {
                frames.addAll(heavy);
            } else if (anim.getAdsFireModelData() > 0) {
                frames.add(anim.getAdsFireModelData());
            } else if (anim.getFireFrames() != null && !anim.getFireFrames().isEmpty()) {
                frames.addAll(anim.getFireFrames());
            }
        } else if (resolvedType == MeleeAttackType.SLASH_2) {
            List<Integer> slash2 = anim.resolveSlash2Frames();
            if (slash2 != null && !slash2.isEmpty()) {
                frames.addAll(slash2);
            } else if (anim.getFireFrames() != null && !anim.getFireFrames().isEmpty()) {
                frames.addAll(anim.getFireFrames());
            }
        } else {
            if (anim.getFireFrames() != null && !anim.getFireFrames().isEmpty()) {
                frames.addAll(anim.getFireFrames());
            }
        }

        if (!frames.isEmpty()) {
            long stepTicks = Math.max(1L, anim.getFireTickRate());
            BukkitTask task = playFrameSequence(player, weapon, state, token, frames, stepTicks);
            state.setActiveTask(task);
        } else {
            state.interrupt();
            restoreCurrentBaseModel(player, weapon);
        }
    }

    private BukkitTask playFrameSequence(Player player, WeaponDefinition weapon, PlayerWeaponState state, long token, List<Integer> frames, long stepTicks) {
        return playFrameSequence(player, weapon, state, token, frames, stepTicks, 0);
    }

    private BukkitTask playFrameSequence(Player player, WeaponDefinition weapon, PlayerWeaponState state, long token, List<Integer> frames, long stepTicks, int startIndex) {
        long clampedStep = Math.max(1L, stepTicks);
        int clampedStart = Math.max(0, Math.min(startIndex, frames.size() - 1));
        int firstModel = frames.get(clampedStart);
        if (firstModel > 0) {
            setItemCustomModelData(player, weapon, firstModel);
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = clampedStart + 1;

            @Override
            public void run() {
                if (!player.isOnline() || state.getActionToken() != token || state.getCurrentState() != WeaponState.FIRING) {
                    return;
                }
                if (index < frames.size()) {
                    int model = frames.get(index);
                    if (model > 0) {
                        setItemCustomModelData(player, weapon, model);
                    }
                    index++;
                } else {
                    state.interrupt();
                    restoreCurrentBaseModel(player, weapon);
                }
            }
        }, clampedStep, clampedStep);
    }

    public void restoreCurrentBaseModel(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null || weapon.isDefaultMode() || weapon.getAnimation() == null) {
            return;
        }
        PlayerWeaponState state = getState(player.getUniqueId());
        boolean aiming = (state != null && state.isAiming()) || (adsManager != null && adsManager.isAiming(player));
        int targetModel = (aiming && weapon.getAnimation().getAdsModelData() > 0)
            ? weapon.getAnimation().getAdsModelData()
            : weapon.getAnimation().getIdleModelData();
        if (targetModel > 0) {
            setItemCustomModelData(player, weapon, targetModel);
        }
    }

    public void resetIdleModel(ItemStack item, WeaponDefinition weapon) {
        if (weapon == null || weapon.isDefaultMode() || weapon.getAnimation() == null) {
            return;
        }
        int idleModel = weapon.getAnimation().getIdleModelData();
        if (idleModel > 0) {
            setItemCustomModelData(item, weapon, idleModel);
        }
    }

    public void applyWeaponModel(Player player, WeaponDefinition weapon, int modelData) {
        if (player == null) {
            return;
        }
        setItemCustomModelData(player, weapon, modelData);
    }

    private void setItemCustomModelData(Player player, WeaponDefinition weapon, int modelData) {
        ItemStack item = player.getInventory().getItemInMainHand();
        setItemCustomModelData(item, weapon, modelData);
    }

    private void setItemCustomModelData(ItemStack item, WeaponDefinition weapon, int modelData) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        if (plugin != null && plugin.getWeaponManager() != null && weapon != null) {
            plugin.getWeaponManager().applyWeaponVisual(item, weapon, modelData);
            return;
        }
        item.editMeta(meta -> meta.setCustomModelData(modelData));
    }
}
