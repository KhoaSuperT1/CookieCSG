package com.cookie.csg.model;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeaponModelTest {

    @Test
    public void testWeaponAnimation() {
        WeaponAnimation anim = new WeaponAnimation(100, 101, 5, 105, 106, List.of(102, 103), List.of(110), List.of(120), 2);
        assertEquals(100, anim.getIdleModelData());
        assertEquals(101, anim.getDeployModelData());
        assertEquals(5, anim.getDeployTicks());
        assertEquals(105, anim.getAdsModelData());
        assertEquals(106, anim.getAdsFireModelData());
        assertEquals(2, anim.getFireFrames().size());
        assertEquals(1, anim.getInspectFrames().size());
        assertEquals(1, anim.getReloadFrames().size());
        assertEquals(2, anim.getInspectTickRate());

        anim.setIdleModelData(200);
        assertEquals(200, anim.getIdleModelData());

        anim.setDeployFrames(List.of(301, 302));
        anim.setAdsInFrames(List.of(303, 304));
        anim.setAdsFireFrames(List.of(305, 306));
        anim.setAdsOutFrames(List.of(307, 308));
        assertEquals("ak47_deploy_2", anim.resolveModelKey("ak47", WeaponType.RIFLE, 302));
        assertEquals("ak47_ads_in_1", anim.resolveModelKey("ak47", WeaponType.RIFLE, 303));
        assertEquals("ak47_ads_fire_2", anim.resolveModelKey("ak47", WeaponType.RIFLE, 306));
        assertEquals("ak47_ads_out_1", anim.resolveModelKey("ak47", WeaponType.RIFLE, 307));
        assertEquals("combat_knife_stance_2", anim.resolveModelKey("combat_knife", WeaponType.KNIFE, 304));
        assertEquals("combat_knife_heavy_1", anim.resolveModelKey("combat_knife", WeaponType.KNIFE, 305));

        assertEquals(List.of(303, 304), anim.resolveSlash2Frames());
        assertEquals(List.of(305, 306), anim.resolveHeavyFrames());

        anim.setSlash2Frames(List.of(401, 402));
        anim.setHeavyFrames(List.of(501, 502));
        assertEquals(List.of(401, 402), anim.resolveSlash2Frames());
        assertEquals(List.of(501, 502), anim.resolveHeavyFrames());
        assertEquals("combat_knife_slash2_1", anim.resolveModelKey("combat_knife", WeaponType.KNIFE, 401));
        assertEquals("combat_knife_heavy_2", anim.resolveModelKey("combat_knife", WeaponType.KNIFE, 502));
    }

    @Test
    public void testWeaponStats() {
        WeaponStats stats = new WeaponStats(10.0, 3.0, 3, 25, 75, 50, 70.0, 0.9, 0.7, 2.5, 0.5);
        assertEquals(10.0, stats.getDamage());
        assertEquals(3.0, stats.getHeadshotMultiplier());
        assertEquals(3, stats.getFireRateTicks());
        assertEquals(25, stats.getClipSize());
        assertEquals(75, stats.getMaxAmmo());
        assertEquals(50, stats.getReloadTicks());
        assertEquals(70.0, stats.getRange());
        assertEquals(0.9, stats.getMovementSpeedMultiplier());
        assertEquals(0.7, stats.getAdsMovementMultiplier());
        assertEquals(0.7, stats.getAdsMovementSpeedMultiplier());
        assertEquals(2.5, stats.getBackstabMultiplier());
        assertEquals(0.5, stats.getShieldBreakChance());
    }

    @Test
    public void testWeaponAds() {
        WeaponAds ads = new WeaponAds(true, 2, 0.2, 0.5, true);
        assertTrue(ads.isEnabled());
        assertEquals(2, ads.getZoomLevel());
        assertEquals(0.2, ads.getSpreadMultiplier());
        assertEquals(0.5, ads.getRecoilMultiplier());
        assertTrue(ads.isScopeOverlay());

        ads.setEnabled(false);
        assertFalse(ads.isEnabled());
    }

    @Test
    public void testWeaponRecoil() {
        WeaponRecoil recoil = new WeaponRecoil(1.5, 0.5, 0.04, 2.5);
        assertEquals(1.5, recoil.getVertical());
        assertEquals(0.5, recoil.getHorizontal());
        assertEquals(0.04, recoil.getSpread());
        assertEquals(2.5, recoil.getSprintSpreadMultiplier());
    }

    @Test
    public void testWeaponAudio() {
        WeaponAudio audio = new WeaponAudio("shoot", "in", "out", "dep", "out_c", "in_c", "bolt", "ins", "emp", "stab", "brk");
        assertEquals("shoot", audio.getShoot());
        assertEquals("in", audio.getAdsIn());
        assertEquals("out", audio.getAdsOut());
        assertEquals("dep", audio.getDeploy());
        assertEquals("out_c", audio.getReloadClipOut());
        assertEquals("in_c", audio.getReloadClipIn());
        assertEquals("bolt", audio.getReloadBolt());
        assertEquals("ins", audio.getInspect());
        assertEquals("emp", audio.getEmpty());
        assertEquals("stab", audio.getBackstab());
        assertEquals("brk", audio.getShieldBreak());
    }

    @Test
    public void testWeaponDefinition() {
        WeaponDefinition def = new WeaponDefinition();
        def.setId("test_gun");
        def.setDisplayName("Test Gun");
        def.setType(WeaponType.RIFLE);
        def.setMaterial(Material.IRON_HOE);

        assertEquals("test_gun", def.getId());
        assertEquals("Test Gun", def.getDisplayName());
        assertEquals(WeaponType.RIFLE, def.getType());
        assertEquals(Material.IRON_HOE, def.getMaterial());
        assertNotNull(def.getAnimation());
        assertNotNull(def.getStats());
        assertNotNull(def.getAds());
        assertNotNull(def.getAdsConfig());
        assertNotNull(def.getRecoil());
        assertNotNull(def.getAudio());
    }
}
