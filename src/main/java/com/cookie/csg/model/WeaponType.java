package com.cookie.csg.model;

public enum WeaponType {
    RIFLE(7.5),
    PISTOL(4.5),
    SNIPER(20.0),
    SHOTGUN(3.0),
    SMG(4.0),
    KNIFE(6.0),
    AXE(8.0);

    private final double defaultDamage;

    WeaponType(double defaultDamage) {
        this.defaultDamage = defaultDamage;
    }

    public boolean isMelee() {
        return this == KNIFE || this == AXE;
    }

    public boolean isGun() {
        return !isMelee();
    }

    public double getDefaultDamage() {
        return defaultDamage;
    }
}
