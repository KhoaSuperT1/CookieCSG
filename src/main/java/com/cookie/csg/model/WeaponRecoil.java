package com.cookie.csg.model;

public class WeaponRecoil {

    private double vertical;
    private double horizontal;
    private double spread;
    private double sprintSpreadMultiplier;

    public WeaponRecoil() {
        this.vertical = 1.0;
        this.horizontal = 0.3;
        this.spread = 0.035;
        this.sprintSpreadMultiplier = 2.0;
    }

    public WeaponRecoil(double vertical, double horizontal, double spread, double sprintSpreadMultiplier) {
        this.vertical = vertical;
        this.horizontal = horizontal;
        this.spread = spread;
        this.sprintSpreadMultiplier = sprintSpreadMultiplier;
    }

    public double getVertical() {
        return vertical;
    }

    public void setVertical(double vertical) {
        this.vertical = vertical;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(double horizontal) {
        this.horizontal = horizontal;
    }

    public double getSpread() {
        return spread;
    }

    public void setSpread(double spread) {
        this.spread = spread;
    }

    public double getSprintSpreadMultiplier() {
        return sprintSpreadMultiplier;
    }

    public void setSprintSpreadMultiplier(double sprintSpreadMultiplier) {
        this.sprintSpreadMultiplier = sprintSpreadMultiplier;
    }
}
