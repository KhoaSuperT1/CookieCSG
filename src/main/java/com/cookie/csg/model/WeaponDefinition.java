package com.cookie.csg.model;

import org.bukkit.Material;

public class WeaponDefinition {

    private String id;
    private String displayName;
    private WeaponMode mode;
    private WeaponType type;
    private Material material;
    private WeaponAnimation animation;
    private WeaponStats stats;
    private WeaponAds ads;
    private WeaponRecoil recoil;
    private WeaponAudio audio;

    public WeaponDefinition() {
        this.mode = WeaponMode.MODEL;
        this.animation = new WeaponAnimation();
        this.stats = new WeaponStats();
        this.ads = new WeaponAds();
        this.recoil = new WeaponRecoil();
        this.audio = new WeaponAudio();
    }

    public WeaponDefinition(String id, String displayName, WeaponType type, Material material, WeaponAnimation animation, WeaponStats stats, WeaponAds ads, WeaponRecoil recoil, WeaponAudio audio) {
        this.id = id;
        this.displayName = displayName;
        this.mode = WeaponMode.MODEL;
        this.type = type;
        this.material = material;
        this.animation = animation != null ? animation : new WeaponAnimation();
        this.stats = stats != null ? stats : new WeaponStats();
        this.ads = ads != null ? ads : new WeaponAds();
        this.recoil = recoil != null ? recoil : new WeaponRecoil();
        this.audio = audio != null ? audio : new WeaponAudio();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public WeaponMode getMode() {
        return mode != null ? mode : WeaponMode.MODEL;
    }

    public void setMode(WeaponMode mode) {
        this.mode = mode != null ? mode : WeaponMode.MODEL;
    }

    public boolean isDefaultMode() {
        return getMode() == WeaponMode.DEFAULT;
    }

    public boolean isModelMode() {
        return getMode() == WeaponMode.MODEL;
    }

    public WeaponType getType() {
        return type;
    }

    public void setType(WeaponType type) {
        this.type = type;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public WeaponAnimation getAnimation() {
        return animation;
    }

    public void setAnimation(WeaponAnimation animation) {
        this.animation = animation;
    }

    public WeaponStats getStats() {
        return stats;
    }

    public void setStats(WeaponStats stats) {
        this.stats = stats;
    }

    public WeaponAds getAds() {
        return ads;
    }

    public void setAds(WeaponAds ads) {
        this.ads = ads;
    }

    public WeaponAds getAdsConfig() {
        return ads;
    }

    public void setAdsConfig(WeaponAds ads) {
        this.ads = ads;
    }

    public WeaponRecoil getRecoil() {
        return recoil;
    }

    public void setRecoil(WeaponRecoil recoil) {
        this.recoil = recoil;
    }

    public WeaponAudio getAudio() {
        return audio;
    }

    public void setAudio(WeaponAudio audio) {
        this.audio = audio;
    }
}
