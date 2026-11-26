package com.cookie.csg.engine;

import com.cookie.csg.CookieCSGPlugin;
import com.cookie.csg.manager.WeaponManager;
import com.cookie.csg.model.WeaponDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdsManager {

    private static final NamespacedKey ADS_MOVE_SPEED_KEY = new NamespacedKey("cookiecsg", "ads_move_speed");

    private final CookieCSGPlugin plugin;
    private final Set<UUID> aimingPlayers;
    private final Map<UUID, Float> savedWalkSpeeds;
    private final Map<UUID, Double> savedBaseMoveSpeeds;

    public AdsManager(CookieCSGPlugin plugin) {
        this.plugin = plugin;
        this.aimingPlayers = ConcurrentHashMap.newKeySet();
        this.savedWalkSpeeds = new ConcurrentHashMap<>();
        this.savedBaseMoveSpeeds = new ConcurrentHashMap<>();
    }

    public boolean isAiming(Player player) {
        if (player == null) {
            return false;
        }
        return aimingPlayers.contains(player.getUniqueId());
    }

    public boolean isAiming(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return aimingPlayers.contains(uuid);
    }

    public void toggleAds(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return;
        }
        if (isAiming(player)) {
            setAiming(player, weapon, false);
        } else {
            setAiming(player, weapon, true);
        }
    }

    public void handleHoldAdsPulse(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return;
        }
        if (weapon.getAdsConfig() != null && !weapon.getAdsConfig().isEnabled()) {
            return;
        }
        int holdTicks = plugin != null ? Math.max(4, (int) Math.ceil(plugin.getHoldAdsReleaseMs() / 50.0)) : 7;
        PlayerWeaponState state = (plugin != null && plugin.getAnimationEngine() != null)
            ? plugin.getAnimationEngine().getOrCreateState(player.getUniqueId())
            : null;
        if (state != null) {
            state.setAdsHoldRemainingTicks(holdTicks);
        }
        if (!isAiming(player)) {
            setAiming(player, weapon, true);
            if (state != null) {
                state.setAdsHoldRemainingTicks(holdTicks);
            }
            startAdsHoldWatchdog(player, weapon, state);
        } else if (state != null && (state.getAdsHoldTask() == null || state.getAdsHoldTask().isCancelled())) {
            startAdsHoldWatchdog(player, weapon, state);
        }
    }

    private void startAdsHoldWatchdog(Player player, WeaponDefinition weapon, PlayerWeaponState state) {
        if (state == null || plugin == null) {
            return;
        }
        String expectedWeaponId = weapon.getId();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isAiming(player)) {
                state.cancelAdsHold();
                return;
            }
            if (plugin.getWeaponManager() != null) {
                ItemStack held = player.getInventory().getItemInMainHand();
                String curId = plugin.getWeaponManager().getWeaponId(held);
                if (curId == null || !curId.equalsIgnoreCase(expectedWeaponId)) {
                    state.cancelAdsHold();
                    return;
                }
            }
            WeaponState cur = state.getCurrentState();
            if (cur == WeaponState.DRAWING || cur == WeaponState.RELOADING || cur == WeaponState.INSPECTING) {
                state.cancelAdsHold();
                return;
            }
            int rem = state.getAdsHoldRemainingTicks() - 1;
            state.setAdsHoldRemainingTicks(rem);
            if (rem <= 0) {
                state.cancelAdsHold();
                setAiming(player, weapon, false);
            }
        }, 1L, 1L);
        state.setAdsHoldTask(task);
    }

    public void setAiming(Player player, WeaponDefinition weapon, boolean aiming) {
        if (player == null || weapon == null) {
            return;
        }
        if (aiming) {
            if (weapon.getAdsConfig() != null && !weapon.getAdsConfig().isEnabled()) {
                return;
            }
            aimingPlayers.add(player.getUniqueId());
            applyZoomEffects(player, weapon);

            if (plugin != null && plugin.getAnimationEngine() != null) {
                plugin.getAnimationEngine().playAdsTransition(player, weapon, true);
            } else {
                int adsModel = weapon.getAnimation().getAdsModelData();
                if (adsModel > 0) {
                    setItemCustomModelData(player, weapon, adsModel);
                }
            }

            if (weapon.getAudio() != null && weapon.getAudio().getAdsIn() != null && !weapon.getAudio().getAdsIn().isEmpty()) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getAdsIn()), 1.0f, 1.0f);
            }
        } else {
            aimingPlayers.remove(player.getUniqueId());
            if (plugin != null && plugin.getAnimationEngine() != null) {
                PlayerWeaponState state = plugin.getAnimationEngine().getState(player.getUniqueId());
                if (state != null) {
                    state.cancelAdsHold();
                }
            }
            removeZoomEffects(player);

            if (plugin != null && plugin.getAnimationEngine() != null) {
                plugin.getAnimationEngine().playAdsTransition(player, weapon, false);
            } else {
                int idleModel = weapon.getAnimation().getIdleModelData();
                if (idleModel > 0) {
                    setItemCustomModelData(player, weapon, idleModel);
                }
            }

            if (weapon.getAudio() != null && weapon.getAudio().getAdsOut() != null && !weapon.getAudio().getAdsOut().isEmpty()) {
                player.playSound(player.getLocation(), WeaponManager.resolveSoundKey(weapon.getAudio().getAdsOut()), 1.0f, 1.0f);
            }
        }
    }

    public void clearAimingSilently(Player player) {
        if (player == null) {
            return;
        }
        aimingPlayers.remove(player.getUniqueId());
        removeZoomEffects(player);
        if (plugin != null && plugin.getAnimationEngine() != null) {
            PlayerWeaponState state = plugin.getAnimationEngine().getState(player.getUniqueId());
            if (state != null) {
                state.cancelAdsHold();
                state.setAiming(false);
                if (state.getCurrentState() == WeaponState.AIMING_ADS) {
                    state.setCurrentState(WeaponState.IDLE);
                }
            }
        }
    }

    public static double targetFovModifierForZoomLevel(int zoomLevel) {
        if (zoomLevel <= 1) {
            return 0.85;
        } else if (zoomLevel == 2) {
            return 0.65;
        } else if (zoomLevel == 3) {
            return 0.38;
        } else if (zoomLevel == 4) {
            return 0.22;
        } else {
            return 0.16;
        }
    }

    public static float computeZoomWalkSpeed(int zoomLevel, double effectiveMoveSpeed) {
        double safeAttr = Math.max(0.01, effectiveMoveSpeed);
        double targetFov = targetFovModifierForZoomLevel(zoomLevel);
        double ratio = 2.0 * targetFov - 1.0;
        if (Math.abs(ratio) < 1e-4) {
            ratio = ratio < 0.0 ? -1e-4 : 1e-4;
        }
        double rawWalkSpeed = (safeAttr / ratio) * 2.0;
        if (rawWalkSpeed < 0.0) {
            return (float) Math.max(-1.0, Math.min(-0.02, rawWalkSpeed));
        }
        return (float) Math.max(0.02, Math.min(1.0, rawWalkSpeed));
    }

    public void applyZoomEffects(Player player, WeaponDefinition weapon) {
        if (player == null || weapon == null) {
            return;
        }
        int zoomLevel = 1;
        if (weapon.getAdsConfig() != null) {
            zoomLevel = Math.max(1, weapon.getAdsConfig().getZoomLevel());
        }
        double defaultMoveMult = switch (zoomLevel) {
            case 1 -> 0.72;
            case 2 -> 0.68;
            case 3 -> 0.58;
            case 4 -> 0.45;
            default -> 0.40;
        };
        double adsMoveMult = weapon.getStats() != null && weapon.getStats().getAdsMovementSpeedMultiplier() > 0.0
            ? Math.max(0.25, Math.min(0.95, weapon.getStats().getAdsMovementSpeedMultiplier()))
            : defaultMoveMult;

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr == null) {
            int amplifier = switch (zoomLevel) {
                case 1 -> 0;
                case 2 -> 2;
                case 3 -> 4;
                case 4 -> 5;
                default -> 6;
            };
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, amplifier, false, false, false));
            return;
        }

        UUID uuid = player.getUniqueId();
        float curWalk = player.getWalkSpeed();
        float fallbackWalk = curWalk > 0.02f ? curWalk : 0.2f;
        savedWalkSpeeds.computeIfAbsent(uuid, id -> fallbackWalk);
        double curBaseAttr = speedAttr.getBaseValue();
        double fallbackBaseAttr = curBaseAttr > 0.01 ? curBaseAttr : 0.1;
        double savedBaseAttr = savedBaseMoveSpeeds.computeIfAbsent(uuid, id -> fallbackBaseAttr);

        speedAttr.removeModifier(ADS_MOVE_SPEED_KEY);
        speedAttr.setBaseValue(savedBaseAttr);
        speedAttr.addModifier(new AttributeModifier(ADS_MOVE_SPEED_KEY, adsMoveMult - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

        double effectiveAttr = Math.max(0.01, speedAttr.getValue());
        float zoomWalkSpeed = computeZoomWalkSpeed(zoomLevel, effectiveAttr);

        if (zoomLevel >= 3 && player.isSprinting()) {
            player.setSprinting(false);
        }

        player.setWalkSpeed(zoomWalkSpeed);
        speedAttr.setBaseValue(savedBaseAttr);
    }

    public void removeZoomEffects(Player player) {
        if (player == null) {
            return;
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        restoreWalkSpeed(player);
    }

    public void sanitizeStaleZoomState(Player player) {
        if (player == null || isAiming(player)) {
            return;
        }
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        boolean hadModifier = false;
        if (speedAttr != null && speedAttr.getModifier(ADS_MOVE_SPEED_KEY) != null) {
            speedAttr.removeModifier(ADS_MOVE_SPEED_KEY);
            hadModifier = true;
        }
        if (player.getWalkSpeed() < 0.0f || hadModifier) {
            player.setWalkSpeed(0.2f);
            if (speedAttr != null && speedAttr.getBaseValue() <= 0.01) {
                speedAttr.setBaseValue(0.1);
            }
        }
    }

    private void restoreWalkSpeed(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Float savedWalk = savedWalkSpeeds.remove(uuid);
        Double savedBaseAttr = savedBaseMoveSpeeds.remove(uuid);
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(ADS_MOVE_SPEED_KEY);
        }
        if (savedWalk != null && savedWalk > 0.0f) {
            player.setWalkSpeed(Math.max(0.05f, Math.min(1.0f, savedWalk)));
        } else if (player.getWalkSpeed() < 0.0f) {
            player.setWalkSpeed(0.2f);
        }
        if (speedAttr != null && savedBaseAttr != null && savedBaseAttr > 0.0) {
            speedAttr.setBaseValue(savedBaseAttr);
        }
    }

    public void cleanup(Player player) {
        if (player == null) {
            return;
        }
        cleanup(player.getUniqueId());
    }

    public void cleanup(UUID uuid) {
        if (uuid == null) {
            return;
        }
        aimingPlayers.remove(uuid);
        if (plugin != null && plugin.getAnimationEngine() != null) {
            PlayerWeaponState state = plugin.getAnimationEngine().getState(uuid);
            if (state != null) {
                state.cancelAdsHold();
                state.setAiming(false);
                if (state.getCurrentState() == WeaponState.AIMING_ADS) {
                    state.setCurrentState(WeaponState.IDLE);
                }
            }
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            removeZoomEffects(player);
        } else {
            savedWalkSpeeds.remove(uuid);
            savedBaseMoveSpeeds.remove(uuid);
        }
    }

    public void cleanupAll() {
        for (UUID uuid : aimingPlayers) {
            if (plugin != null && plugin.getAnimationEngine() != null) {
                PlayerWeaponState state = plugin.getAnimationEngine().getState(uuid);
                if (state != null) {
                    state.cancelAdsHold();
                    state.setAiming(false);
                }
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeZoomEffects(player);
            }
        }
        for (UUID uuid : Set.copyOf(savedWalkSpeeds.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreWalkSpeed(player);
            }
        }
        savedWalkSpeeds.clear();
        savedBaseMoveSpeeds.clear();
        aimingPlayers.clear();
    }

    private void setItemCustomModelData(Player player, WeaponDefinition weapon, int modelData) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.AIR && item.hasItemMeta()) {
            if (plugin != null && plugin.getWeaponManager() != null && weapon != null) {
                plugin.getWeaponManager().applyWeaponVisual(item, weapon, modelData);
                return;
            }
            item.editMeta(meta -> meta.setCustomModelData(modelData));
        }
    }
}
