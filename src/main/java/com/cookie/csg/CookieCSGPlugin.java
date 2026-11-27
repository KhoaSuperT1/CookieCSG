package com.cookie.csg;

import com.cookie.csg.command.CSGCommand;
import com.cookie.csg.engine.AdsManager;
import com.cookie.csg.engine.AnimationEngine;
import com.cookie.csg.engine.MeleeEngine;
import com.cookie.csg.engine.RaycastRecoilEngine;
import com.cookie.csg.gui.GuiListener;
import com.cookie.csg.gui.GuiManager;
import com.cookie.csg.listener.WeaponInteractionListener;
import com.cookie.csg.manager.WeaponManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CookieCSGPlugin extends JavaPlugin {

    private static final Set<UUID> APPLYING_WEAPON_DAMAGE = ConcurrentHashMap.newKeySet();

    private static CookieCSGPlugin instance;
    private WeaponManager weaponManager;
    private AdsManager adsManager;
    private RaycastRecoilEngine recoilEngine;
    private MeleeEngine meleeEngine;
    private AnimationEngine animationEngine;
    private GuiManager guiManager;
    private boolean rightClickFireForGuns;
    private boolean holdToAds = true;
    private long holdAdsReleaseMs = 350L;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.rightClickFireForGuns = getConfig().getBoolean("gun-controls.right-click-fire", false);
        this.holdToAds = getConfig().getBoolean("gun-controls.hold-to-ads", true);
        this.holdAdsReleaseMs = Math.max(100L, getConfig().getLong("gun-controls.hold-ads-release-ms", 350L));

        this.weaponManager = new WeaponManager(this);
        this.weaponManager.loadWeapons();
        this.weaponManager.extractBundledDefaultModels(getDataFolder().getParentFile());

        this.adsManager = new AdsManager(this);
        this.recoilEngine = new RaycastRecoilEngine(this);
        this.meleeEngine = new MeleeEngine(this);
        this.animationEngine = new AnimationEngine(this, this.adsManager);
        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new WeaponInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        CSGCommand csgCmd = new CSGCommand(this);
        if (getCommand("csg") != null) {
            getCommand("csg").setExecutor(csgCmd);
            getCommand("csg").setTabCompleter(csgCmd);
        }

        getLogger().info("CookieCSG v" + getDescription().getVersion() + " initialized successfully.");
    }

    @Override
    public void onDisable() {
        if (this.adsManager != null) {
            this.adsManager.cleanupAll();
        }
        if (this.animationEngine != null) {
            this.animationEngine.cleanupAll();
        }
        instance = null;
        getLogger().info("CookieCSG disabled.");
    }

    public static CookieCSGPlugin getInstance() {
        return instance;
    }

    public WeaponManager getWeaponManager() {
        return weaponManager;
    }

    public AdsManager getAdsManager() {
        return adsManager;
    }

    public RaycastRecoilEngine getRecoilEngine() {
        return recoilEngine;
    }

    public MeleeEngine getMeleeEngine() {
        return meleeEngine;
    }

    public AnimationEngine getAnimationEngine() {
        return animationEngine;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public boolean isRightClickFireForGuns() {
        return rightClickFireForGuns;
    }

    public void setRightClickFireForGuns(boolean rightClickFire) {
        this.rightClickFireForGuns = rightClickFire;
    }

    public boolean isHoldToAds() {
        return holdToAds;
    }

    public void setHoldToAds(boolean holdToAds) {
        this.holdToAds = holdToAds;
    }

    public long getHoldAdsReleaseMs() {
        return holdAdsReleaseMs;
    }

    public void setHoldAdsReleaseMs(long holdAdsReleaseMs) {
        this.holdAdsReleaseMs = Math.max(100L, holdAdsReleaseMs);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        this.rightClickFireForGuns = getConfig().getBoolean("gun-controls.right-click-fire", false);
        this.holdToAds = getConfig().getBoolean("gun-controls.hold-to-ads", true);
        this.holdAdsReleaseMs = Math.max(100L, getConfig().getLong("gun-controls.hold-ads-release-ms", 350L));
    }

    public boolean isApplyingWeaponDamage(UUID playerId) {
        return playerId != null && APPLYING_WEAPON_DAMAGE.contains(playerId);
    }

    public void applyWeaponDamage(Player attacker, LivingEntity victim, double damage, boolean bypassInvulnerability) {
        if (attacker == null || victim == null || victim.isDead() || damage <= 0.0) {
            return;
        }
        UUID attackerId = attacker.getUniqueId();
        APPLYING_WEAPON_DAMAGE.add(attackerId);
        try {
            if (bypassInvulnerability) {
                victim.setNoDamageTicks(0);
            }
            victim.damage(damage, attacker);
        } finally {
            APPLYING_WEAPON_DAMAGE.remove(attackerId);
        }
    }
}
