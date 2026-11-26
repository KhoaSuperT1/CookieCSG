package com.cookie.csg.model;

public class WeaponAudio {

    private String shoot;
    private String adsIn;
    private String adsOut;
    private String deploy;
    private String reloadClipOut;
    private String reloadClipIn;
    private String reloadBolt;
    private String inspect;
    private String empty;
    private String backstab;
    private String shieldBreak;
    private int reloadClipInTick;
    private int reloadBoltTick;

    public WeaponAudio() {
        this.shoot = "";
        this.adsIn = "";
        this.adsOut = "";
        this.deploy = "";
        this.reloadClipOut = "";
        this.reloadClipIn = "";
        this.reloadBolt = "";
        this.inspect = "";
        this.empty = "";
        this.backstab = "";
        this.shieldBreak = "";
        this.reloadClipInTick = -1;
        this.reloadBoltTick = -1;
    }

    public WeaponAudio(String shoot, String adsIn, String adsOut, String deploy, String reloadClipOut, String reloadClipIn, String reloadBolt, String inspect, String empty, String backstab, String shieldBreak) {
        this.shoot = shoot != null ? shoot : "";
        this.adsIn = adsIn != null ? adsIn : "";
        this.adsOut = adsOut != null ? adsOut : "";
        this.deploy = deploy != null ? deploy : "";
        this.reloadClipOut = reloadClipOut != null ? reloadClipOut : "";
        this.reloadClipIn = reloadClipIn != null ? reloadClipIn : "";
        this.reloadBolt = reloadBolt != null ? reloadBolt : "";
        this.inspect = inspect != null ? inspect : "";
        this.empty = empty != null ? empty : "";
        this.backstab = backstab != null ? backstab : "";
        this.shieldBreak = shieldBreak != null ? shieldBreak : "";
        this.reloadClipInTick = -1;
        this.reloadBoltTick = -1;
    }

    public String getShoot() {
        return shoot;
    }

    public void setShoot(String shoot) {
        this.shoot = shoot != null ? shoot : "";
    }

    public String getAdsIn() {
        return adsIn;
    }

    public void setAdsIn(String adsIn) {
        this.adsIn = adsIn != null ? adsIn : "";
    }

    public String getAdsOut() {
        return adsOut;
    }

    public void setAdsOut(String adsOut) {
        this.adsOut = adsOut != null ? adsOut : "";
    }

    public String getDeploy() {
        return deploy;
    }

    public void setDeploy(String deploy) {
        this.deploy = deploy != null ? deploy : "";
    }

    public String getReloadClipOut() {
        return reloadClipOut;
    }

    public void setReloadClipOut(String reloadClipOut) {
        this.reloadClipOut = reloadClipOut != null ? reloadClipOut : "";
    }

    public String getReloadClipIn() {
        return reloadClipIn;
    }

    public void setReloadClipIn(String reloadClipIn) {
        this.reloadClipIn = reloadClipIn != null ? reloadClipIn : "";
    }

    public String getReloadBolt() {
        return reloadBolt;
    }

    public void setReloadBolt(String reloadBolt) {
        this.reloadBolt = reloadBolt != null ? reloadBolt : "";
    }

    public String getInspect() {
        return inspect;
    }

    public void setInspect(String inspect) {
        this.inspect = inspect != null ? inspect : "";
    }

    public String getEmpty() {
        return empty;
    }

    public void setEmpty(String empty) {
        this.empty = empty != null ? empty : "";
    }

    public String getBackstab() {
        return backstab;
    }

    public void setBackstab(String backstab) {
        this.backstab = backstab != null ? backstab : "";
    }

    public String getShieldBreak() {
        return shieldBreak;
    }

    public void setShieldBreak(String shieldBreak) {
        this.shieldBreak = shieldBreak != null ? shieldBreak : "";
    }

    public int getReloadClipInTick() {
        return reloadClipInTick;
    }

    public void setReloadClipInTick(int reloadClipInTick) {
        this.reloadClipInTick = reloadClipInTick;
    }

    public int getReloadBoltTick() {
        return reloadBoltTick;
    }

    public void setReloadBoltTick(int reloadBoltTick) {
        this.reloadBoltTick = reloadBoltTick;
    }
}
