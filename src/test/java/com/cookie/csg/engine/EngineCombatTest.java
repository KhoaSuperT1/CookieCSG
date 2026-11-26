package com.cookie.csg.engine;

import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EngineCombatTest {

    @Test
    public void testWeaponStates() {
        assertEquals(6, WeaponState.values().length);
        assertNotNull(WeaponState.valueOf("IDLE"));
        assertNotNull(WeaponState.valueOf("DRAWING"));
        assertNotNull(WeaponState.valueOf("AIMING_ADS"));
        assertNotNull(WeaponState.valueOf("FIRING"));
        assertNotNull(WeaponState.valueOf("INSPECTING"));
        assertNotNull(WeaponState.valueOf("RELOADING"));
    }

    @Test
    public void testPlayerWeaponStateLifecycle() {
        UUID uuid = UUID.randomUUID();
        PlayerWeaponState state = new PlayerWeaponState(uuid);

        assertEquals(uuid, state.getPlayerUuid());
        assertEquals(WeaponState.IDLE, state.getCurrentState());
        assertFalse(state.isAiming());
        assertEquals(0, state.getAmmoInClip());
        assertEquals(0, state.getCurrentFrameIndex());

        state.setCurrentState(WeaponState.INSPECTING);
        state.setCurrentFrameIndex(5);
        state.interrupt();

        assertEquals(WeaponState.IDLE, state.getCurrentState());
        assertEquals(0, state.getCurrentFrameIndex());

        state.setAiming(true);
        state.setCurrentState(WeaponState.INSPECTING);
        state.interrupt();

        assertEquals(WeaponState.AIMING_ADS, state.getCurrentState());

        assertEquals(0, state.getMeleeComboStep());
        assertEquals(0L, state.getLastMeleeAttackMs());
        state.setMeleeComboStep(1);
        state.setLastMeleeAttackMs(12345L);
        assertEquals(1, state.getMeleeComboStep());
        assertEquals(12345L, state.getLastMeleeAttackMs());
    }

    @Test
    public void testBackstabDotProductMath() {
        Vector attackerDirSame = new Vector(0, 0, 1).normalize();
        Vector victimDirSame = new Vector(0, 0, 1).normalize();
        double dotSame = attackerDirSame.dot(victimDirSame);
        assertTrue(dotSame >= 0.5);

        Vector victimDir45 = new Vector(Math.sin(Math.toRadians(45)), 0, Math.cos(Math.toRadians(45))).normalize();
        double dot45 = attackerDirSame.dot(victimDir45);
        assertTrue(dot45 >= 0.5);

        Vector victimDir60 = new Vector(Math.sin(Math.toRadians(60)), 0, Math.cos(Math.toRadians(60))).normalize();
        double dot60 = attackerDirSame.dot(victimDir60);
        assertTrue(dot60 >= 0.4999);

        Vector victimDirOpposite = new Vector(0, 0, -1).normalize();
        double dotOpposite = attackerDirSame.dot(victimDirOpposite);
        assertFalse(dotOpposite >= 0.5);

        Vector victimDir90 = new Vector(1, 0, 0).normalize();
        double dot90 = attackerDirSame.dot(victimDir90);
        assertFalse(dot90 >= 0.5);
    }

    @Test
    public void testHeadshotCalculation() {
        double victimY = 64.0;
        double headshotHitY = 65.5;
        double bodyshotHitY = 65.0;

        assertTrue((headshotHitY - victimY) >= 1.45);
        assertFalse((bodyshotHitY - victimY) >= 1.45);
    }

    @Test
    public void testSpreadFormula() {
        double baseSpread = 0.035;
        double adsMultiplier = 0.15;
        double sprintMultiplier = 2.0;
        double airMultiplier = 3.5;

        double adsSpread = baseSpread * adsMultiplier * 1.0 * 1.0;
        assertEquals(0.00525, adsSpread, 0.00001);

        double jumpingSpread = baseSpread * 1.0 * sprintMultiplier * airMultiplier;
        assertEquals(0.245, jumpingSpread, 0.00001);
    }

    @Test
    public void testZoomLevelsProduceMonotonicFovReduction() {
        double moveAttr = 0.06;
        double prevFov = 1.0;
        for (int level = 1; level <= 5; level++) {
            float ws = AdsManager.computeZoomWalkSpeed(level, moveAttr);
            assertTrue(ws >= -1.0f && ws <= 1.0f);
            assertNotEquals(0.0f, ws);
            double abilitiesWalk = ws / 2.0;
            double clientFovMod = ((moveAttr / abilitiesWalk) + 1.0) / 2.0;
            assertTrue(clientFovMod > 0.05 && clientFovMod < prevFov);
            double sprintFovMod = (((moveAttr * 1.3) / abilitiesWalk) + 1.0) / 2.0;
            assertTrue(sprintFovMod > 0.01);
            prevFov = clientFovMod;
        }
    }

    @Test
    public void testRelativeRecoilTeleportIdAndPitchClamping() {
        assertEquals(Integer.MAX_VALUE, RaycastRecoilEngine.RECOIL_TELEPORT_ID);
        assertEquals(-0.2f, RaycastRecoilEngine.clampDeltaPitch(-89.8f, -1.2f), 1e-5f);
        assertEquals(0.0f, RaycastRecoilEngine.clampDeltaPitch(-90.0f, -1.5f), 1e-5f);
        assertEquals(0.0f, RaycastRecoilEngine.clampDeltaPitch(90.0f, 1.5f), 1e-5f);
        assertEquals(-1.25f, RaycastRecoilEngine.clampDeltaPitch(10.0f, -1.25f), 1e-5f);
    }

    @Test
    public void testAllThreePacksFireModesCappedAtTwoAndNoBurst() {
        for (String packName : List.of("Pack 1", "Pack 2", "Pack 3")) {
            File folder = new File("CookieCSG_DONE/" + packName + "/CookieCSG/weapons");
            assertTrue(folder.exists(), "Missing pack weapons folder: " + folder);
            WeaponManager mgr = new WeaponManager(folder);
            mgr.loadWeapons();
            for (WeaponDefinition def : mgr.getWeapons().values()) {
                if (!def.getType().isGun()) {
                    continue;
                }
                List<FireMode> supported = def.getStats().resolveSupportedFireModes(def.getType());
                assertTrue(supported.size() >= 1 && supported.size() <= 2, def.getId() + " modes=" + supported);
                FireMode defMode = def.getStats().resolveDefaultFireMode(def.getType());
                assertNotNull(defMode, def.getId() + " missing default fire mode");
                assertTrue(supported.contains(defMode), def.getId() + " default not in supported");
            }
        }
    }
}
