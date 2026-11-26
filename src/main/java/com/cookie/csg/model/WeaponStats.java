package com.cookie.csg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeaponStats {

    private double damage;
    private double headshotMultiplier;
    private int fireRateTicks;
    private int clipSize;
    private int maxAmmo;
    private int reloadTicks;
    private int reloadTacticalTicks;
    private double range;
    private double movementSpeedMultiplier;
    private double adsMovementMultiplier;
    private double backstabMultiplier;
    private double shieldBreakChance;
    private List<FireMode> fireModes;
    private FireMode defaultFireMode;
    private int burstCount;

    public WeaponStats() {
        this.damage = 5.0;
        this.headshotMultiplier = 2.0;
        this.fireRateTicks = 2;
        this.clipSize = 30;
        this.maxAmmo = 90;
        this.reloadTicks = 40;
        this.range = 64.0;
        this.movementSpeedMultiplier = 1.0;
        this.adsMovementMultiplier = 0.75;
        this.backstabMultiplier = 1.0;
        this.shieldBreakChance = 0.0;
        this.fireModes = new ArrayList<>();
        this.defaultFireMode = null;
        this.burstCount = 3;
    }

    public WeaponStats(double damage, double headshotMultiplier, int fireRateTicks, int clipSize, int maxAmmo, int reloadTicks, double range, double movementSpeedMultiplier, double adsMovementMultiplier, double backstabMultiplier, double shieldBreakChance) {
        this.damage = damage;
        this.headshotMultiplier = headshotMultiplier;
        this.fireRateTicks = fireRateTicks;
        this.clipSize = clipSize;
        this.maxAmmo = maxAmmo;
        this.reloadTicks = reloadTicks;
        this.range = range;
        this.movementSpeedMultiplier = movementSpeedMultiplier;
        this.adsMovementMultiplier = adsMovementMultiplier;
        this.backstabMultiplier = backstabMultiplier;
        this.shieldBreakChance = shieldBreakChance;
        this.fireModes = new ArrayList<>();
        this.defaultFireMode = null;
        this.burstCount = 3;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getHeadshotMultiplier() {
        return headshotMultiplier;
    }

    public void setHeadshotMultiplier(double headshotMultiplier) {
        this.headshotMultiplier = headshotMultiplier;
    }

    public int getFireRateTicks() {
        return fireRateTicks;
    }

    public void setFireRateTicks(int fireRateTicks) {
        this.fireRateTicks = fireRateTicks;
    }

    public int getClipSize() {
        return clipSize;
    }

    public void setClipSize(int clipSize) {
        this.clipSize = clipSize;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    public void setMaxAmmo(int maxAmmo) {
        this.maxAmmo = maxAmmo;
    }

    public int getReloadTicks() {
        return reloadTicks;
    }

    public void setReloadTicks(int reloadTicks) {
        this.reloadTicks = reloadTicks;
    }

    public int getReloadTacticalTicks() {
        return reloadTacticalTicks;
    }

    public void setReloadTacticalTicks(int reloadTacticalTicks) {
        this.reloadTacticalTicks = Math.max(0, reloadTacticalTicks);
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getMovementSpeedMultiplier() {
        return movementSpeedMultiplier;
    }

    public void setMovementSpeedMultiplier(double movementSpeedMultiplier) {
        this.movementSpeedMultiplier = movementSpeedMultiplier;
    }

    public double getAdsMovementMultiplier() {
        return adsMovementMultiplier;
    }

    public void setAdsMovementMultiplier(double adsMovementMultiplier) {
        this.adsMovementMultiplier = adsMovementMultiplier;
    }

    public double getAdsMovementSpeedMultiplier() {
        return adsMovementMultiplier;
    }

    public void setAdsMovementSpeedMultiplier(double adsMovementSpeedMultiplier) {
        this.adsMovementMultiplier = adsMovementSpeedMultiplier;
    }

    public double getBackstabMultiplier() {
        return backstabMultiplier;
    }

    public void setBackstabMultiplier(double backstabMultiplier) {
        this.backstabMultiplier = backstabMultiplier;
    }

    public double getShieldBreakChance() {
        return shieldBreakChance;
    }

    public void setShieldBreakChance(double shieldBreakChance) {
        this.shieldBreakChance = shieldBreakChance;
    }

    public List<FireMode> getFireModes() {
        return fireModes;
    }

    public void setFireModes(List<FireMode> fireModes) {
        this.fireModes = fireModes != null ? new ArrayList<>(fireModes) : new ArrayList<>();
    }

    public FireMode getDefaultFireMode() {
        return defaultFireMode;
    }

    public void setDefaultFireMode(FireMode defaultFireMode) {
        this.defaultFireMode = defaultFireMode;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public void setBurstCount(int burstCount) {
        this.burstCount = Math.max(2, burstCount);
    }

    public List<FireMode> resolveSupportedFireModes(WeaponType type) {
        if (type == null || type.isMelee()) {
            return List.of();
        }
        if (fireModes != null && !fireModes.isEmpty()) {
            List<FireMode> resolved = new ArrayList<>(2);
            for (FireMode mode : fireModes) {
                if (mode != null && !resolved.contains(mode)) {
                    resolved.add(mode);
                    if (resolved.size() >= 2) {
                        break;
                    }
                }
            }
            return Collections.unmodifiableList(resolved);
        }
        return switch (type) {
            case RIFLE, SMG -> List.of(FireMode.AUTO, FireMode.SEMI_AUTO);
            case PISTOL -> List.of(FireMode.SEMI_AUTO);
            case SNIPER, SHOTGUN -> List.of(FireMode.SINGLE);
            default -> List.of();
        };
    }

    public FireMode resolveDefaultFireMode(WeaponType type) {
        List<FireMode> supported = resolveSupportedFireModes(type);
        if (supported.isEmpty()) {
            return null;
        }
        if (defaultFireMode != null && supported.contains(defaultFireMode)) {
            return defaultFireMode;
        }
        return supported.get(0);
    }
}
