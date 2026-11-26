package com.cookie.csg.engine;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MeleeEngine {

    public static final long MIN_MELEE_COOLDOWN_MS = 150L;
    public static final long MELEE_COMBO_WINDOW_MS = 1800L;

    private final CookieCSGPlugin plugin;

    public MeleeEngine(CookieCSGPlugin plugin) {
        this.plugin = plugin;
    }

    private long resolveMeleeCooldownMs(WeaponDefinition weapon) {
        int fireRateTicks = weapon.getStats() != null ? weapon.getStats().getFireRateTicks() : 8;
        return Math.max(MIN_MELEE_COOLDOWN_MS, Math.max(1, fireRateTicks) * 50L);
    }

    private AnimationEngine.MeleeAttackType tryBeginSlash(Player player, WeaponDefinition weapon) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        long elapsed = now - state.getLastMeleeAttackMs();
        if (elapsed < resolveMeleeCooldownMs(weapon)) {
            return null;
        }
        boolean withinCombo = state.getLastMeleeAttackMs() > 0L && elapsed <= MELEE_COMBO_WINDOW_MS;
        int step = withinCombo ? (state.getMeleeComboStep() % 2) : 0;
        state.setMeleeComboStep((step + 1) % 2);
        state.setLastMeleeAttackMs(now);
        return step == 1 ? AnimationEngine.MeleeAttackType.SLASH_2 : AnimationEngine.MeleeAttackType.SLASH_1;
    }

    private boolean tryBeginHeavy(Player player, WeaponDefinition weapon) {
        PlayerWeaponState state = plugin.getAnimationEngine().getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (now - state.getLastMeleeAttackMs() < resolveMeleeCooldownMs(weapon)) {
            return false;
        }
        state.setMeleeComboStep(0);
        state.setLastMeleeAttackMs(now);
        return true;
    }

    public boolean executeSlash(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return false;
        }

        AnimationEngine.MeleeAttackType attackType = tryBeginSlash(player, weapon);
        if (attackType == null) {
            return false;
        }

        double range = 3.0;
        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection();

        if (weapon.getAudio() != null && weapon.getAudio().getShoot() != null && !weapon.getAudio().getShoot().isEmpty()) {
            player.getWorld().playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getShoot()), 1.0f, 1.0f);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
        }

        double damage = weapon.getStats().getDamage();
        for (Entity entity : player.getWorld().getNearbyEntities(eyeLoc, range, range, range)) {
            if (entity instanceof LivingEntity victim && !victim.equals(player) && !victim.isDead()) {
                Vector diff = victim.getLocation().add(0.0, victim.getHeight() * 0.5, 0.0).toVector().subtract(eyeLoc.toVector());
                if (diff.lengthSquared() < 1e-6 || lookDir.dot(diff.normalize()) >= 0.2) {
                    plugin.applyWeaponDamage(player, victim, damage, true);
                }
            }
        }

        plugin.getAnimationEngine().playMeleeAnimation(player, weapon, attackType);
        return true;
    }

    public boolean executeStab(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return false;
        }

        if (!tryBeginHeavy(player, weapon)) {
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(eyeLoc, lookDir, 2.8, 0.5, entity -> !entity.equals(player) && entity instanceof LivingEntity);

        if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
            Vector attackerDir = player.getLocation().getDirection().setY(0).normalize();
            Vector victimDir = victim.getLocation().getDirection().setY(0).normalize();
            double dot = attackerDir.dot(victimDir);

            if (dot >= 0.5) {
                double multiplier = weapon.getStats().getBackstabMultiplier() > 0 ? weapon.getStats().getBackstabMultiplier() : 3.0;
                double damage = weapon.getStats().getDamage() * multiplier;
                plugin.applyWeaponDamage(player, victim, damage, true);

                if (weapon.getAudio() != null && weapon.getAudio().getBackstab() != null && !weapon.getAudio().getBackstab().isEmpty()) {
                    String backstabSound = WeaponManager.resolveSoundKey(weapon.getAudio().getBackstab());
                    player.playSound(player.getLocation(), backstabSound, 1.0f, 1.0f);
                    victim.getWorld().playSound(victim.getLocation(), backstabSound, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                }

                victim.getWorld().spawnParticle(Particle.CRIT, victim.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                victim.getWorld().spawnParticle(Particle.BLOCK, victim.getEyeLocation(), 15, 0.2, 0.2, 0.2, Material.REDSTONE_BLOCK.createBlockData());
            } else {
                double damage = weapon.getStats().getDamage() * 1.2;
                plugin.applyWeaponDamage(player, victim, damage, true);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getEyeLocation(), 8, 0.2, 0.2, 0.2, 0.05);
            }
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.2f);
        }

        plugin.getAnimationEngine().playMeleeAnimation(player, weapon, AnimationEngine.MeleeAttackType.HEAVY);
        return true;
    }

    public boolean executeAxeCleave(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return false;
        }

        if (!tryBeginHeavy(player, weapon)) {
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(eyeLoc, lookDir, 3.2, 0.6, entity -> !entity.equals(player) && entity instanceof LivingEntity);

        if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
            if (victim instanceof Player victimPlayer && victimPlayer.isBlocking()) {
                victimPlayer.setCooldown(Material.SHIELD, 100);
                if (weapon.getAudio() != null && weapon.getAudio().getShieldBreak() != null && !weapon.getAudio().getShieldBreak().isEmpty()) {
                    String shieldBreakSound = WeaponManager.resolveSoundKey(weapon.getAudio().getShieldBreak());
                    player.playSound(player.getLocation(), shieldBreakSound, 1.0f, 1.0f);
                    victimPlayer.playSound(victimPlayer.getLocation(), shieldBreakSound, 1.0f, 1.0f);
                } else {
                    victimPlayer.playSound(victimPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                }
                victimPlayer.getWorld().spawnParticle(Particle.BLOCK, victimPlayer.getEyeLocation(), 25, 0.3, 0.3, 0.3, Material.SHIELD.createBlockData());
            }

            double damage = weapon.getStats().getDamage() * 1.5;
            plugin.applyWeaponDamage(player, victim, damage, true);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, true));

            Vector knockback = player.getLocation().getDirection().setY(0.3).normalize().multiply(1.2);
            victim.setVelocity(knockback);

            if (weapon.getAudio() != null && weapon.getAudio().getAdsIn() != null && !weapon.getAudio().getAdsIn().isEmpty()) {
                player.getWorld().playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getAdsIn()), 1.0f, 1.0f);
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.9f);
            }
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.15);
        } else {
            if (weapon.getAudio() != null && weapon.getAudio().getAdsIn() != null && !weapon.getAudio().getAdsIn().isEmpty()) {
                player.getWorld().playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getAdsIn()), 1.0f, 1.2f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 0.9f);
            }
        }

        plugin.getAnimationEngine().playMeleeAnimation(player, weapon, AnimationEngine.MeleeAttackType.HEAVY);
        return true;
    }
}
