package com.cookie.csg.engine;

import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PlayerWeaponState {

    private final UUID playerUuid;
    private WeaponState currentState;
    private BukkitTask activeTask;
    private final List<BukkitTask> pendingTasks;
    private long actionToken;
    private int currentFrameIndex;
    private int ammoInClip;
    private String activeWeaponId;
    private final Map<String, Integer> ammoByWeapon;
    private long lastFireTime;
    private boolean isAiming;
    private long lastRightClickMs;
    private long lastInteractEntityMs;
    private long lastDamageEntityMs;
    private BukkitTask autoFireTask;
    private int meleeComboStep;
    private long lastMeleeAttackMs;
    private boolean swingSuppressed;
    private PotionEffect savedHasteEffect;
    private final Map<String, FireMode> fireModeByWeapon;
    private long lastSneakPressMs;
    private long lastFireClickMs;
    private long burstLockUntilMs;
    private BukkitTask adsHoldTask;
    private int adsHoldRemainingTicks;
    private int autoFireRemainingTicks;
    private int consecutiveShots;
    private long lastShotForSpreadMs;

    public PlayerWeaponState(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.currentState = WeaponState.IDLE;
        this.activeTask = null;
        this.pendingTasks = new ArrayList<>();
        this.actionToken = 0L;
        this.currentFrameIndex = 0;
        this.ammoInClip = 0;
        this.activeWeaponId = null;
        this.ammoByWeapon = new HashMap<>();
        this.lastFireTime = 0L;
        this.isAiming = false;
        this.lastRightClickMs = 0L;
        this.lastInteractEntityMs = 0L;
        this.lastDamageEntityMs = 0L;
        this.autoFireTask = null;
        this.meleeComboStep = 0;
        this.lastMeleeAttackMs = 0L;
        this.fireModeByWeapon = new HashMap<>();
        this.lastSneakPressMs = 0L;
        this.lastFireClickMs = 0L;
        this.burstLockUntilMs = 0L;
        this.adsHoldTask = null;
        this.adsHoldRemainingTicks = 0;
        this.autoFireRemainingTicks = 0;
        this.consecutiveShots = 0;
        this.lastShotForSpreadMs = 0L;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public WeaponState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(WeaponState currentState) {
        this.currentState = currentState;
    }

    public BukkitTask getActiveTask() {
        return activeTask;
    }

    public void setActiveTask(BukkitTask activeTask) {
        if (this.activeTask != null && !this.activeTask.isCancelled()) {
            this.activeTask.cancel();
        }
        this.activeTask = activeTask;
        if (activeTask != null && !this.pendingTasks.contains(activeTask)) {
            this.pendingTasks.add(activeTask);
        }
    }

    public void addPendingTask(BukkitTask task) {
        if (task != null) {
            this.pendingTasks.add(task);
        }
    }

    public long getActionToken() {
        return actionToken;
    }

    public long nextActionToken() {
        return ++this.actionToken;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public void setCurrentFrameIndex(int currentFrameIndex) {
        this.currentFrameIndex = currentFrameIndex;
    }

    public String getActiveWeaponId() {
        return activeWeaponId;
    }

    public void setActiveWeaponId(String weaponId) {
        this.activeWeaponId = (weaponId == null || weaponId.isBlank()) ? null : weaponId.toLowerCase(Locale.ROOT);
        if (this.activeWeaponId != null && ammoByWeapon.containsKey(this.activeWeaponId)) {
            this.ammoInClip = ammoByWeapon.get(this.activeWeaponId);
        }
    }

    public int getAmmoInClip(String weaponId, int defaultClipSize) {
        if (weaponId == null || weaponId.isBlank()) {
            return getAmmoInClip();
        }
        String key = weaponId.toLowerCase(Locale.ROOT);
        int maxClip = Math.max(0, defaultClipSize);
        Integer stored = ammoByWeapon.get(key);
        if (stored == null) {
            stored = maxClip;
            ammoByWeapon.put(key, stored);
        } else if (maxClip > 0 && stored > maxClip) {
            stored = maxClip;
            ammoByWeapon.put(key, stored);
        }
        if (key.equals(this.activeWeaponId)) {
            this.ammoInClip = stored;
        }
        return stored;
    }

    public void setAmmoInClip(String weaponId, int ammo) {
        int clamped = Math.max(0, ammo);
        if (weaponId == null || weaponId.isBlank()) {
            this.ammoInClip = clamped;
            return;
        }
        String key = weaponId.toLowerCase(Locale.ROOT);
        ammoByWeapon.put(key, clamped);
        if (key.equals(this.activeWeaponId) || this.activeWeaponId == null) {
            this.ammoInClip = clamped;
        }
    }

    public int getAmmoInClip() {
        if (activeWeaponId != null && ammoByWeapon.containsKey(activeWeaponId)) {
            return ammoByWeapon.get(activeWeaponId);
        }
        return ammoInClip;
    }

    public void setAmmoInClip(int ammoInClip) {
        int clamped = Math.max(0, ammoInClip);
        this.ammoInClip = clamped;
        if (activeWeaponId != null) {
            ammoByWeapon.put(activeWeaponId, clamped);
        }
    }

    public long getLastFireTime() {
        return lastFireTime;
    }

    public void setLastFireTime(long lastFireTime) {
        this.lastFireTime = lastFireTime;
    }

    public boolean isAiming() {
        return isAiming;
    }

    public void setAiming(boolean aiming) {
        isAiming = aiming;
    }

    public long getLastRightClickMs() {
        return lastRightClickMs;
    }

    public void setLastRightClickMs(long lastRightClickMs) {
        this.lastRightClickMs = lastRightClickMs;
    }

    public long getLastInteractEntityMs() {
        return lastInteractEntityMs;
    }

    public void setLastInteractEntityMs(long lastInteractEntityMs) {
        this.lastInteractEntityMs = lastInteractEntityMs;
    }

    public long getLastDamageEntityMs() {
        return lastDamageEntityMs;
    }

    public void setLastDamageEntityMs(long lastDamageEntityMs) {
        this.lastDamageEntityMs = lastDamageEntityMs;
    }

    public BukkitTask getAutoFireTask() {
        return autoFireTask;
    }

    public void setAutoFireTask(BukkitTask autoFireTask) {
        if (this.autoFireTask != null && !this.autoFireTask.isCancelled() && this.autoFireTask != autoFireTask) {
            this.autoFireTask.cancel();
        }
        this.autoFireTask = autoFireTask;
    }

    public void cancelAutoFireTaskOnly() {
        if (this.autoFireTask != null && !this.autoFireTask.isCancelled()) {
            this.autoFireTask.cancel();
        }
        this.autoFireTask = null;
        this.autoFireRemainingTicks = 0;
    }

    public void cancelAutoFire() {
        cancelAutoFireTaskOnly();
        this.burstLockUntilMs = 0L;
    }

    public BukkitTask getAdsHoldTask() {
        return adsHoldTask;
    }

    public void setAdsHoldTask(BukkitTask adsHoldTask) {
        if (this.adsHoldTask != null && !this.adsHoldTask.isCancelled() && this.adsHoldTask != adsHoldTask) {
            this.adsHoldTask.cancel();
        }
        this.adsHoldTask = adsHoldTask;
    }

    public int getAdsHoldRemainingTicks() {
        return adsHoldRemainingTicks;
    }

    public void setAdsHoldRemainingTicks(int adsHoldRemainingTicks) {
        this.adsHoldRemainingTicks = Math.max(0, adsHoldRemainingTicks);
    }

    public void cancelAdsHold() {
        if (this.adsHoldTask != null && !this.adsHoldTask.isCancelled()) {
            this.adsHoldTask.cancel();
        }
        this.adsHoldTask = null;
        this.adsHoldRemainingTicks = 0;
    }

    public int getAutoFireRemainingTicks() {
        return autoFireRemainingTicks;
    }

    public void setAutoFireRemainingTicks(int autoFireRemainingTicks) {
        this.autoFireRemainingTicks = Math.max(0, autoFireRemainingTicks);
    }

    public int getConsecutiveShots() {
        return consecutiveShots;
    }

    public void setConsecutiveShots(int consecutiveShots) {
        this.consecutiveShots = Math.max(0, consecutiveShots);
    }

    public long getLastShotForSpreadMs() {
        return lastShotForSpreadMs;
    }

    public void setLastShotForSpreadMs(long lastShotForSpreadMs) {
        this.lastShotForSpreadMs = lastShotForSpreadMs;
    }

    public void resetSprayState() {
        this.consecutiveShots = 0;
        this.lastShotForSpreadMs = 0L;
    }

    public void clearBurstLock() {
        this.burstLockUntilMs = 0L;
    }

    public FireMode getFireMode(WeaponDefinition weapon) {
        if (weapon == null || weapon.getStats() == null) {
            return null;
        }
        List<FireMode> supported = weapon.getStats().resolveSupportedFireModes(weapon.getType());
        if (supported.isEmpty()) {
            return null;
        }
        String key = weapon.getId() != null ? weapon.getId().toLowerCase(Locale.ROOT) : "";
        FireMode stored = fireModeByWeapon.get(key);
        if (stored != null && supported.contains(stored)) {
            return stored;
        }
        FireMode def = weapon.getStats().resolveDefaultFireMode(weapon.getType());
        if (def != null && !key.isEmpty()) {
            fireModeByWeapon.put(key, def);
        }
        return def;
    }

    public void setFireMode(String weaponId, FireMode mode) {
        if (weaponId != null && !weaponId.isBlank() && mode != null) {
            fireModeByWeapon.put(weaponId.toLowerCase(Locale.ROOT), mode);
        }
    }

    public FireMode cycleFireMode(WeaponDefinition weapon) {
        if (weapon == null || weapon.getStats() == null) {
            return null;
        }
        List<FireMode> supported = weapon.getStats().resolveSupportedFireModes(weapon.getType());
        if (supported.isEmpty()) {
            return null;
        }
        FireMode current = getFireMode(weapon);
        if (supported.size() == 1) {
            return supported.get(0);
        }
        int idx = supported.indexOf(current);
        FireMode next = supported.get((idx + 1) % supported.size());
        if (weapon.getId() != null && !weapon.getId().isBlank()) {
            fireModeByWeapon.put(weapon.getId().toLowerCase(Locale.ROOT), next);
        }
        return next;
    }

    public long getLastSneakPressMs() {
        return lastSneakPressMs;
    }

    public void setLastSneakPressMs(long lastSneakPressMs) {
        this.lastSneakPressMs = lastSneakPressMs;
    }

    public long getLastFireClickMs() {
        return lastFireClickMs;
    }

    public void setLastFireClickMs(long lastFireClickMs) {
        this.lastFireClickMs = lastFireClickMs;
    }

    public long getBurstLockUntilMs() {
        return burstLockUntilMs;
    }

    public void setBurstLockUntilMs(long burstLockUntilMs) {
        this.burstLockUntilMs = burstLockUntilMs;
    }

    public int getMeleeComboStep() {
        return meleeComboStep;
    }

    public void setMeleeComboStep(int meleeComboStep) {
        this.meleeComboStep = meleeComboStep;
    }

    public long getLastMeleeAttackMs() {
        return lastMeleeAttackMs;
    }

    public void setLastMeleeAttackMs(long lastMeleeAttackMs) {
        this.lastMeleeAttackMs = lastMeleeAttackMs;
    }

    public boolean isSwingSuppressed() {
        return swingSuppressed;
    }

    public void setSwingSuppressed(boolean swingSuppressed) {
        this.swingSuppressed = swingSuppressed;
    }

    public PotionEffect getSavedHasteEffect() {
        return savedHasteEffect;
    }

    public void setSavedHasteEffect(PotionEffect savedHasteEffect) {
        this.savedHasteEffect = savedHasteEffect;
    }

    public void interrupt() {
        this.actionToken++;
        if (activeTask != null && !activeTask.isCancelled()) {
            activeTask.cancel();
        }
        activeTask = null;
        for (BukkitTask task : pendingTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        pendingTasks.clear();
        currentFrameIndex = 0;
        if (currentState == WeaponState.INSPECTING
                || currentState == WeaponState.DRAWING
                || currentState == WeaponState.RELOADING
                || currentState == WeaponState.FIRING) {
            currentState = isAiming ? WeaponState.AIMING_ADS : WeaponState.IDLE;
        }
    }
}
