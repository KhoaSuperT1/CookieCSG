package com.cookie.csg.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeaponTypeTest {

    @Test
    public void testMeleeTypes() {
        assertTrue(WeaponType.KNIFE.isMelee());
        assertTrue(WeaponType.AXE.isMelee());
        assertFalse(WeaponType.KNIFE.isGun());
        assertFalse(WeaponType.AXE.isGun());
    }

    @Test
    public void testGunTypes() {
        assertTrue(WeaponType.RIFLE.isGun());
        assertTrue(WeaponType.PISTOL.isGun());
        assertTrue(WeaponType.SNIPER.isGun());
        assertTrue(WeaponType.SHOTGUN.isGun());
        assertTrue(WeaponType.SMG.isGun());

        assertFalse(WeaponType.RIFLE.isMelee());
        assertFalse(WeaponType.PISTOL.isMelee());
        assertFalse(WeaponType.SNIPER.isMelee());
        assertFalse(WeaponType.SHOTGUN.isMelee());
        assertFalse(WeaponType.SMG.isMelee());
    }

    @Test
    public void testDefaultDamages() {
        assertEquals(7.5, WeaponType.RIFLE.getDefaultDamage());
        assertEquals(4.5, WeaponType.PISTOL.getDefaultDamage());
        assertEquals(20.0, WeaponType.SNIPER.getDefaultDamage());
        assertEquals(3.0, WeaponType.SHOTGUN.getDefaultDamage());
        assertEquals(4.0, WeaponType.SMG.getDefaultDamage());
        assertEquals(6.0, WeaponType.KNIFE.getDefaultDamage());
        assertEquals(8.0, WeaponType.AXE.getDefaultDamage());
    }
}
