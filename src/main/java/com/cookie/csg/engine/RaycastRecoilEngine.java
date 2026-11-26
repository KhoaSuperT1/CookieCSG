package com.cookie.csg.engine;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.FireMode;
import com.cookie.csg.model.WeaponDefinition;
import com.cookie.csg.model.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RaycastRecoilEngine {

    static final int RECOIL_TELEPORT_ID = Integer.MAX_VALUE;

    private static volatile boolean nmsChecked = false;
    private static volatile boolean nmsReady = false;
    private static Object nmsVec3Zero;
    private static Constructor<?> nmsPmrCtor;
    private static Set<?> nmsRelativeFlags;
    private static Method nmsPosPacketOfMethod;
    private static Field nmsConnectionField;
    private static Method nmsSendPacketMethod;
    private static volatile Class<?> cachedPlayerClass;
    private static volatile Method cachedGetHandleMethod;

    private final CookieCSGPlugin plugin;

    public RaycastRecoilEngine(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    public void executeFire(Player player, WeaponDefinition weapon, boolean isAds) {
        executeFire(player, weapon, isAds, null, false);
    }

    public boolean executeFire(Player player, WeaponDefinition weapon, boolean isAds, FireMode fireMode, boolean scheduledShot) {
        if (player == null || weapon == null) {
            return false;
        }

        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        if (state.getCurrentState() == WeaponState.DRAWING) {
            return false;
        }
        boolean interruptedPrevAnim = false;
        if (state.getCurrentState() == WeaponState.RELOADING) {
            if (!plugin.getAnimationEngine().canInterruptReloadForFire(state, weapon)) {
                return false;
            }
            state.interrupt();
            interruptedPrevAnim = true;
        }

        if (state.getCurrentState() == WeaponState.INSPECTING) {
            state.interrupt();
            interruptedPrevAnim = true;
        }

        FireMode effectiveMode = fireMode != null ? fireMode : state.getFireMode(weapon);
        int baseRate = Math.max(1, weapon.getStats().getFireRateTicks());
        long fireDelayMs = switch (effectiveMode != null ? effectiveMode : FireMode.AUTO) {
            case SINGLE -> Math.max(baseRate, 5) * 50L;
            case SEMI_AUTO -> (baseRate <= 3 ? Math.max(4, baseRate + 2) : baseRate) * 50L;
            case AUTO -> baseRate * 50L;
        };
        long now = System.currentTimeMillis();
        if (scheduledShot) {
            long minAllowed = Math.max(0L, fireDelayMs - 15L);
            if (now - state.getLastFireTime() < minAllowed) {
                if (interruptedPrevAnim) {
                    plugin.getAnimationEngine().restoreCurrentBaseModel(player, weapon);
                }
                return false;
            }
        } else if (now - state.getLastFireTime() < fireDelayMs) {
            if (interruptedPrevAnim) {
                plugin.getAnimationEngine().restoreCurrentBaseModel(player, weapon);
            }
            return false;
        }
        state.setLastFireTime(now);

        state.setActiveWeaponId(weapon.getId());
        int clipSize = weapon.getStats().getClipSize();
        int currentAmmo = state.getAmmoInClip(weapon.getId(), clipSize);
        if (currentAmmo <= 0) {
            if (weapon.getAudio() != null && weapon.getAudio().getEmpty() != null && !weapon.getAudio().getEmpty().isEmpty()) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getEmpty()), 1.0f, 1.0f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.8f);
            }
            plugin.getAnimationEngine().playReloadAnimation(player, weapon);
            return false;
        }

        int remainingAmmo = currentAmmo - 1;
        state.setAmmoInClip(weapon.getId(), remainingAmmo);
        String modeSuffix = effectiveMode != null ? "  |  [" + effectiveMode.getDisplayName() + "]" : "";
        player.sendActionBar(Component.text("✦ " + remainingAmmo + " / " + weapon.getStats().getMaxAmmo() + modeSuffix, NamedTextColor.GOLD));

        double modeSpreadMult;
        double modeVertRecoilMult;
        double modeHorizRecoilMult;
        if (effectiveMode == FireMode.SINGLE || effectiveMode == FireMode.SEMI_AUTO) {
            state.setConsecutiveShots(0);
            state.setLastShotForSpreadMs(now);
            modeSpreadMult = 0.60;
            modeVertRecoilMult = 0.68;
            modeHorizRecoilMult = 0.60;
        } else {
            int shots = (now - state.getLastShotForSpreadMs() <= 350L) ? state.getConsecutiveShots() + 1 : 0;
            state.setConsecutiveShots(shots);
            state.setLastShotForSpreadMs(now);
            modeSpreadMult = 1.0 + Math.min(0.65, shots * 0.06);
            modeVertRecoilMult = 1.08 + Math.min(0.25, shots * 0.02);
            modeHorizRecoilMult = 1.10;
        }

        int pelletCount = (weapon.getType() == WeaponType.SHOTGUN) ? 8 : 1;

        double baseSpread = weapon.getRecoil().getSpread();
        if (weapon.getType() == WeaponType.SNIPER && !isAds) {
            baseSpread = 0.15;
        }

        double adsSpreadMultiplier = isAds ? (weapon.getAdsConfig() != null ? weapon.getAdsConfig().getSpreadMultiplier() : 0.15) : 1.0;
        double sprintMultiplier = player.isSprinting() ? weapon.getRecoil().getSprintSpreadMultiplier() : 1.0;
        double airMultiplier = (!player.isOnGround()) ? 3.5 : 1.0;
        double totalSpread = baseSpread * adsSpreadMultiplier * sprintMultiplier * airMultiplier * modeSpreadMult;

        double range = weapon.getStats().getRange() > 0 ? weapon.getStats().getRange() : 64.0;
        Location eyeLoc = player.getEyeLocation();

        Map<LivingEntity, Double> damageByVictim = new LinkedHashMap<>();
        Map<LivingEntity, Boolean> headshotByVictim = new LinkedHashMap<>();
        Map<LivingEntity, Location> hitLocationByVictim = new LinkedHashMap<>();

        for (int i = 0; i < pelletCount; i++) {
            double rx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * totalSpread;
            double ry = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * totalSpread;
            double rz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * totalSpread;

            Vector bulletDir = eyeLoc.getDirection().clone().add(new Vector(rx, ry, rz)).normalize();

            RayTraceResult result = player.getWorld().rayTrace(eyeLoc, bulletDir, range, FluidCollisionMode.NEVER, true, 0.15, entity -> !entity.equals(player) && entity instanceof LivingEntity living && !living.isDead());

            double rawTracerDist = result != null ? result.getHitPosition().distance(eyeLoc.toVector()) : Math.min(range, 48.0);
            double maxTracerDist = Math.min(rawTracerDist, 48.0);
            double tracerStart = isAds ? 2.4 : 1.6;
            for (double d = tracerStart; d < maxTracerDist; d += 1.6) {
                Location tracerPoint = eyeLoc.clone().add(bulletDir.clone().multiply(d));
                player.getWorld().spawnParticle(Particle.CRIT, tracerPoint, 1, 0, 0, 0, 0);
            }

            if (result != null) {
                Vector hitPos = result.getHitPosition();
                if (result.getHitEntity() instanceof LivingEntity victim) {
                    Location victimLoc = victim.getLocation();
                    double victimHeight = victim.getHeight() > 0.0 ? victim.getHeight() : 1.8;
                    double headThreshold = victimHeight >= 1.75 ? 1.45 : Math.max(0.35, victimHeight * 0.78);
                    boolean isHeadshot = (hitPos.getY() - victimLoc.getY() >= headThreshold);

                    double baseDamage = weapon.getStats().getDamage();
                    double finalDamage = isHeadshot ? (baseDamage * weapon.getStats().getHeadshotMultiplier()) : baseDamage;
                    damageByVictim.merge(victim, finalDamage, Double::sum);
                    if (isHeadshot) {
                        headshotByVictim.put(victim, true);
                    } else {
                        headshotByVictim.putIfAbsent(victim, false);
                    }
                    hitLocationByVictim.put(victim, new Location(victim.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ()));
                } else if (result.getHitBlock() != null) {
                    Block hitBlock = result.getHitBlock();
                    Location blockHitLoc = new Location(hitBlock.getWorld(), hitPos.getX(), hitPos.getY(), hitPos.getZ());
                    player.getWorld().spawnParticle(Particle.BLOCK, blockHitLoc, 8, 0.1, 0.1, 0.1, hitBlock.getBlockData());
                }
            }
        }

        for (Map.Entry<LivingEntity, Double> entry : damageByVictim.entrySet()) {
            LivingEntity victim = entry.getKey();
            double totalDamage = entry.getValue();
            plugin.applyWeaponDamage(player, victim, totalDamage, true);
            boolean isHeadshot = Boolean.TRUE.equals(headshotByVictim.get(victim));
            Location hitLocation = hitLocationByVictim.get(victim);
            if (hitLocation != null) {
                if (isHeadshot) {
                    victim.getWorld().spawnParticle(Particle.CRIT, hitLocation, 20, 0.2, 0.2, 0.2, 0.1);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
                } else {
                    victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, hitLocation, 5, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        Location muzzleLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(isAds ? 0.9 : 0.85)).add(0.0, -0.08, 0.0);
        if (isAds) {
            player.getWorld().spawnParticle(Particle.FLAME, muzzleLoc, 1, 0.02, 0.02, 0.02, 0.0);
        } else {
            player.getWorld().spawnParticle(Particle.FLAME, muzzleLoc, 2, 0.02, 0.02, 0.02, 0.0);
            player.getWorld().spawnParticle(Particle.SMOKE, muzzleLoc, 1, 0.02, 0.02, 0.02, 0.0);
        }

        if (weapon.getAudio() != null && weapon.getAudio().getShoot() != null && !weapon.getAudio().getShoot().isEmpty()) {
            player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getShoot()), 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.8f);
        }

        double recoilMultiplier = isAds ? (weapon.getAdsConfig() != null ? weapon.getAdsConfig().getRecoilMultiplier() : 0.6) : 1.0;
        double vRecoil = weapon.getRecoil().getVertical() * recoilMultiplier * modeVertRecoilMult;
        double hRecoil = weapon.getRecoil().getHorizontal() * recoilMultiplier * modeHorizRecoilMult;
        double randomYawOffset = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * hRecoil;

        float deltaYaw = (float) randomYawOffset;
        float deltaPitch = -(float) vRecoil;
        applyRelativeRecoil(player, deltaYaw, deltaPitch);

        plugin.getAnimationEngine().playFireAnimation(player, weapon, isAds, scheduledShot);
        return true;
    }

    static float clampDeltaPitch(float currentPitch, float deltaPitch) {
        return Math.max(-90.0f - currentPitch, Math.min(90.0f - currentPitch, deltaPitch));
    }

    private void applyRelativeRecoil(Player player, float deltaYaw, float deltaPitch) {
        float curPitch = player.getPitch();
        float clampedDeltaPitch = clampDeltaPitch(curPitch, deltaPitch);
        if (Math.abs(deltaYaw) < 1e-5f && Math.abs(clampedDeltaPitch) < 1e-5f) {
            return;
        }
        if (sendRelativeRotationPacket(player, deltaYaw, clampedDeltaPitch)) {
            return;
        }
        float curYaw = player.getYaw();
        float targetPitch = Math.max(-90.0f, Math.min(90.0f, curPitch + clampedDeltaPitch));
        player.setRotation(curYaw + deltaYaw, targetPitch);
    }

    private static boolean sendRelativeRotationPacket(Player player, float deltaYaw, float deltaPitch) {
        if (!ensureNmsReady()) {
            return false;
        }
        try {
            Method getHandle = resolveGetHandleMethod(player.getClass());
            if (getHandle == null) {
                return false;
            }
            Object handle = getHandle.invoke(player);
            if (handle == null) {
                return false;
            }
            Object conn = nmsConnectionField.get(handle);
            if (conn == null) {
                return false;
            }
            Object change = nmsPmrCtor.newInstance(nmsVec3Zero, nmsVec3Zero, deltaYaw, deltaPitch);
            Object packet = nmsPosPacketOfMethod.invoke(null, RECOIL_TELEPORT_ID, change, nmsRelativeFlags);
            nmsSendPacketMethod.invoke(conn, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method resolveGetHandleMethod(Class<?> playerClass) {
        if (playerClass == cachedPlayerClass && cachedGetHandleMethod != null) {
            return cachedGetHandleMethod;
        }
        try {
            Method m = playerClass.getMethod("getHandle");
            cachedPlayerClass = playerClass;
            cachedGetHandleMethod = m;
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean ensureNmsReady() {
        if (nmsChecked) {
            return nmsReady;
        }
        synchronized (RaycastRecoilEngine.class) {
            if (nmsChecked) {
                return nmsReady;
            }
            try {
                Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
                nmsVec3Zero = vec3Class.getField("ZERO").get(null);
                Class<?> pmrClass = Class.forName("net.minecraft.world.entity.PositionMoveRotation");
                nmsPmrCtor = pmrClass.getConstructor(vec3Class, vec3Class, float.class, float.class);
                Class<?> relativeClass = Class.forName("net.minecraft.world.entity.Relative");
                EnumSet flags = EnumSet.noneOf((Class) relativeClass);
                for (Object constant : relativeClass.getEnumConstants()) {
                    Enum<?> rel = (Enum<?>) constant;
                    String name = rel.name();
                    if ("X".equals(name) || "Y".equals(name) || "Z".equals(name)
                        || "Y_ROT".equals(name) || "X_ROT".equals(name)
                        || "DELTA_X".equals(name) || "DELTA_Y".equals(name) || "DELTA_Z".equals(name)) {
                        flags.add(rel);
                    }
                }
                nmsRelativeFlags = Collections.unmodifiableSet(flags);
                Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
                Class<?> posPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket");
                nmsPosPacketOfMethod = posPacketClass.getMethod("of", int.class, pmrClass, Set.class);
                Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
                nmsConnectionField = serverPlayerClass.getField("connection");
                nmsSendPacketMethod = nmsConnectionField.getType().getMethod("send", packetClass);
                nmsReady = true;
            } catch (Throwable ignored) {
                nmsReady = false;
            } finally {
                nmsChecked = true;
            }
            return nmsReady;
        }
    }
}
