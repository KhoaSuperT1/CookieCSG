package com.cookie.csg.model;

public class WeaponAds {

    private boolean enabled;
    private int zoomLevel;
    private double spreadMultiplier;
    private double recoilMultiplier;
    private boolean scopeOverlay;

    public WeaponAds() {
        this.enabled = true;
        this.zoomLevel = 1;
        this.spreadMultiplier = 0.15;
        this.recoilMultiplier = 0.60;
        this.scopeOverlay = false;
    }

    public WeaponAds(boolean enabled, int zoomLevel, double spreadMultiplier, double recoilMultiplier, boolean scopeOverlay) {
        this.enabled = enabled;
        this.zoomLevel = zoomLevel;
        this.spreadMultiplier = spreadMultiplier;
        this.recoilMultiplier = recoilMultiplier;
        this.scopeOverlay = scopeOverlay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public double getSpreadMultiplier() {
        return spreadMultiplier;
    }

    public void setSpreadMultiplier(double spreadMultiplier) {
        this.spreadMultiplier = spreadMultiplier;
    }

    public double getRecoilMultiplier() {
        return recoilMultiplier;
    }

    public void setRecoilMultiplier(double recoilMultiplier) {
        this.recoilMultiplier = recoilMultiplier;
    }

    public boolean isScopeOverlay() {
        return scopeOverlay;
    }

    public void setScopeOverlay(boolean scopeOverlay) {
        this.scopeOverlay = scopeOverlay;
    }
}
