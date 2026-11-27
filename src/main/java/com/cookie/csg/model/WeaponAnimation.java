package com.cookie.csg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponAnimation {

    private int idleModelData;
    private int deployModelData;
    private int deployTicks;
    private int adsModelData;
    private int adsFireModelData;
    private List<Integer> fireFrames;
    private List<Integer> slash2Frames;
    private List<Integer> heavyFrames;
    private List<Integer> inspectFrames;
    private List<Integer> reloadFrames;
    private int inspectTickRate;
    private List<Integer> deployFrames;
    private int deployTickRate;
    private List<Integer> adsInFrames;
    private List<Integer> adsFireFrames;
    private List<Integer> adsOutFrames;
    private int fireTickRate;
    private int adsTickRate;
    private int reloadTickRate;
    private List<Integer> reloadTacticalFrames;
    private List<Integer> reloadIntroFrames;
    private List<Integer> reloadLoopFrames;
    private List<Integer> reloadEndFrames;
    private int reloadIntroTicks;
    private int reloadLoopTicks;
    private int reloadEndTicks;
    private String itemModelNamespace = "cookie_csg";
    private Map<Integer, String> frameModels;
    private transient final Map<Integer, String> resolvedKeyCache = new ConcurrentHashMap<>();
    private transient volatile String cachedKeyIdentity = "";

    public WeaponAnimation() {
        this.deployFrames = new ArrayList<>();
        this.deployTickRate = 1;
        this.adsInFrames = new ArrayList<>();
        this.adsFireFrames = new ArrayList<>();
        this.adsOutFrames = new ArrayList<>();
        this.fireFrames = new ArrayList<>();
        this.slash2Frames = new ArrayList<>();
        this.heavyFrames = new ArrayList<>();
        this.inspectFrames = new ArrayList<>();
        this.reloadFrames = new ArrayList<>();
        this.inspectTickRate = 1;
        this.fireTickRate = 1;
        this.adsTickRate = 1;
        this.reloadTickRate = 1;
        this.reloadTacticalFrames = new ArrayList<>();
        this.reloadIntroFrames = new ArrayList<>();
        this.reloadLoopFrames = new ArrayList<>();
        this.reloadEndFrames = new ArrayList<>();
        this.reloadIntroTicks = 0;
        this.reloadLoopTicks = 0;
        this.reloadEndTicks = 0;
        this.frameModels = new HashMap<>();
    }

    public WeaponAnimation(int idleModelData, int deployModelData, int deployTicks, int adsModelData, int adsFireModelData, List<Integer> fireFrames, List<Integer> inspectFrames, List<Integer> reloadFrames, int inspectTickRate) {
        this.idleModelData = idleModelData;
        this.deployModelData = deployModelData;
        this.deployTicks = deployTicks;
        this.adsModelData = adsModelData;
        this.adsFireModelData = adsFireModelData;
        this.deployFrames = new ArrayList<>();
        this.deployTickRate = 1;
        this.adsInFrames = new ArrayList<>();
        this.adsFireFrames = new ArrayList<>();
        this.adsOutFrames = new ArrayList<>();
        this.fireFrames = fireFrames != null ? new ArrayList<>(fireFrames) : new ArrayList<>();
        this.slash2Frames = new ArrayList<>();
        this.heavyFrames = new ArrayList<>();
        this.inspectFrames = inspectFrames != null ? new ArrayList<>(inspectFrames) : new ArrayList<>();
        this.reloadFrames = reloadFrames != null ? new ArrayList<>(reloadFrames) : new ArrayList<>();
        this.inspectTickRate = inspectTickRate > 0 ? inspectTickRate : 1;
        this.fireTickRate = 1;
        this.adsTickRate = 1;
        this.reloadTickRate = 1;
        this.reloadTacticalFrames = new ArrayList<>();
        this.reloadIntroFrames = new ArrayList<>();
        this.reloadLoopFrames = new ArrayList<>();
        this.reloadEndFrames = new ArrayList<>();
        this.reloadIntroTicks = 0;
        this.reloadLoopTicks = 0;
        this.reloadEndTicks = 0;
        this.frameModels = new HashMap<>();
    }

    private void invalidateKeyCache() {
        this.cachedKeyIdentity = "";
        this.resolvedKeyCache.clear();
    }

    public int getIdleModelData() {
        return idleModelData;
    }

    public void setIdleModelData(int idleModelData) {
        this.idleModelData = idleModelData;
        invalidateKeyCache();
    }

    public int getDeployModelData() {
        return deployModelData;
    }

    public void setDeployModelData(int deployModelData) {
        this.deployModelData = deployModelData;
        invalidateKeyCache();
    }

    public int getDeployTicks() {
        return deployTicks;
    }

    public void setDeployTicks(int deployTicks) {
        this.deployTicks = deployTicks;
    }

    public int getAdsModelData() {
        return adsModelData;
    }

    public void setAdsModelData(int adsModelData) {
        this.adsModelData = adsModelData;
        invalidateKeyCache();
    }

    public int getAdsFireModelData() {
        return adsFireModelData;
    }

    public void setAdsFireModelData(int adsFireModelData) {
        this.adsFireModelData = adsFireModelData;
        invalidateKeyCache();
    }

    public List<Integer> getFireFrames() {
        return fireFrames;
    }

    public void setFireFrames(List<Integer> fireFrames) {
        this.fireFrames = fireFrames != null ? new ArrayList<>(fireFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getSlash2Frames() {
        return slash2Frames;
    }

    public void setSlash2Frames(List<Integer> slash2Frames) {
        this.slash2Frames = slash2Frames != null ? new ArrayList<>(slash2Frames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getHeavyFrames() {
        return heavyFrames;
    }

    public void setHeavyFrames(List<Integer> heavyFrames) {
        this.heavyFrames = heavyFrames != null ? new ArrayList<>(heavyFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> resolveSlash2Frames() {
        if (slash2Frames != null && !slash2Frames.isEmpty()) {
            return slash2Frames;
        }
        return adsInFrames != null ? adsInFrames : List.of();
    }

    public List<Integer> resolveHeavyFrames() {
        if (heavyFrames != null && !heavyFrames.isEmpty()) {
            return heavyFrames;
        }
        return adsFireFrames != null ? adsFireFrames : List.of();
    }

    public List<Integer> getInspectFrames() {
        return inspectFrames;
    }

    public void setInspectFrames(List<Integer> inspectFrames) {
        this.inspectFrames = inspectFrames != null ? new ArrayList<>(inspectFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getReloadFrames() {
        return reloadFrames;
    }

    public void setReloadFrames(List<Integer> reloadFrames) {
        this.reloadFrames = reloadFrames != null ? new ArrayList<>(reloadFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public int getInspectTickRate() {
        return inspectTickRate;
    }

    public void setInspectTickRate(int inspectTickRate) {
        this.inspectTickRate = inspectTickRate;
    }

    public List<Integer> getDeployFrames() {
        return deployFrames;
    }

    public void setDeployFrames(List<Integer> deployFrames) {
        this.deployFrames = deployFrames != null ? new ArrayList<>(deployFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public int getDeployTickRate() {
        return deployTickRate;
    }

    public void setDeployTickRate(int deployTickRate) {
        this.deployTickRate = deployTickRate > 0 ? deployTickRate : 1;
    }

    public List<Integer> getAdsInFrames() {
        return adsInFrames;
    }

    public void setAdsInFrames(List<Integer> adsInFrames) {
        this.adsInFrames = adsInFrames != null ? new ArrayList<>(adsInFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getAdsFireFrames() {
        return adsFireFrames;
    }

    public void setAdsFireFrames(List<Integer> adsFireFrames) {
        this.adsFireFrames = adsFireFrames != null ? new ArrayList<>(adsFireFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getAdsOutFrames() {
        return adsOutFrames;
    }

    public void setAdsOutFrames(List<Integer> adsOutFrames) {
        this.adsOutFrames = adsOutFrames != null ? new ArrayList<>(adsOutFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public int getFireTickRate() {
        return fireTickRate;
    }

    public void setFireTickRate(int fireTickRate) {
        this.fireTickRate = fireTickRate > 0 ? fireTickRate : 1;
    }

    public int getAdsTickRate() {
        return adsTickRate;
    }

    public void setAdsTickRate(int adsTickRate) {
        this.adsTickRate = adsTickRate > 0 ? adsTickRate : 1;
    }

    public int getReloadTickRate() {
        return reloadTickRate;
    }

    public void setReloadTickRate(int reloadTickRate) {
        this.reloadTickRate = reloadTickRate > 0 ? reloadTickRate : 1;
    }

    public List<Integer> getReloadTacticalFrames() {
        return reloadTacticalFrames;
    }

    public void setReloadTacticalFrames(List<Integer> reloadTacticalFrames) {
        this.reloadTacticalFrames = reloadTacticalFrames != null ? new ArrayList<>(reloadTacticalFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getReloadIntroFrames() {
        return reloadIntroFrames;
    }

    public void setReloadIntroFrames(List<Integer> reloadIntroFrames) {
        this.reloadIntroFrames = reloadIntroFrames != null ? new ArrayList<>(reloadIntroFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getReloadLoopFrames() {
        return reloadLoopFrames;
    }

    public void setReloadLoopFrames(List<Integer> reloadLoopFrames) {
        this.reloadLoopFrames = reloadLoopFrames != null ? new ArrayList<>(reloadLoopFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public List<Integer> getReloadEndFrames() {
        return reloadEndFrames;
    }

    public void setReloadEndFrames(List<Integer> reloadEndFrames) {
        this.reloadEndFrames = reloadEndFrames != null ? new ArrayList<>(reloadEndFrames) : new ArrayList<>();
        invalidateKeyCache();
    }

    public int getReloadIntroTicks() {
        return reloadIntroTicks;
    }

    public void setReloadIntroTicks(int reloadIntroTicks) {
        this.reloadIntroTicks = Math.max(0, reloadIntroTicks);
    }

    public int getReloadLoopTicks() {
        return reloadLoopTicks;
    }

    public void setReloadLoopTicks(int reloadLoopTicks) {
        this.reloadLoopTicks = Math.max(0, reloadLoopTicks);
    }

    public int getReloadEndTicks() {
        return reloadEndTicks;
    }

    public void setReloadEndTicks(int reloadEndTicks) {
        this.reloadEndTicks = Math.max(0, reloadEndTicks);
    }

    public String getItemModelNamespace() {
        return itemModelNamespace;
    }

    public void setItemModelNamespace(String itemModelNamespace) {
        if (itemModelNamespace == null || itemModelNamespace.isBlank()) {
            this.itemModelNamespace = "cookie_csg";
        } else {
            this.itemModelNamespace = itemModelNamespace.trim();
        }
        invalidateKeyCache();
    }

    public boolean isItemModelDisabled() {
        return itemModelNamespace == null
            || itemModelNamespace.isBlank()
            || "none".equalsIgnoreCase(itemModelNamespace)
            || "vanilla".equalsIgnoreCase(itemModelNamespace);
    }

    public boolean hasCustomVisuals() {
        if (idleModelData > 0 || deployModelData > 0 || adsModelData > 0 || adsFireModelData > 0) {
            return true;
        }
        if (frameModels != null && !frameModels.isEmpty()) {
            return true;
        }
        return (deployFrames != null && !deployFrames.isEmpty())
            || (adsInFrames != null && !adsInFrames.isEmpty())
            || (adsFireFrames != null && !adsFireFrames.isEmpty())
            || (adsOutFrames != null && !adsOutFrames.isEmpty())
            || (fireFrames != null && !fireFrames.isEmpty())
            || (slash2Frames != null && !slash2Frames.isEmpty())
            || (heavyFrames != null && !heavyFrames.isEmpty())
            || (inspectFrames != null && !inspectFrames.isEmpty())
            || (reloadFrames != null && !reloadFrames.isEmpty())
            || (reloadTacticalFrames != null && !reloadTacticalFrames.isEmpty())
            || (reloadIntroFrames != null && !reloadIntroFrames.isEmpty())
            || (reloadLoopFrames != null && !reloadLoopFrames.isEmpty())
            || (reloadEndFrames != null && !reloadEndFrames.isEmpty());
    }

    public Map<Integer, String> getFrameModels() {
        return frameModels;
    }

    public void setFrameModels(Map<Integer, String> frameModels) {
        this.frameModels = frameModels != null ? new HashMap<>(frameModels) : new HashMap<>();
        invalidateKeyCache();
    }

    public void putFrameModel(int modelData, String modelKey) {
        if (modelData > 0 && modelKey != null && !modelKey.isBlank()) {
            this.frameModels.put(modelData, modelKey);
            invalidateKeyCache();
        }
    }

    public String resolveModelKey(String weaponId, WeaponType type, int modelData) {
        String idPart = weaponId != null ? weaponId.toLowerCase() : "weapon";
        String typePart = type != null ? type.name() : "";
        String identity = idPart + "|" + typePart;
        if (!identity.equals(this.cachedKeyIdentity)) {
            this.resolvedKeyCache.clear();
            this.cachedKeyIdentity = identity;
        }
        String cached = this.resolvedKeyCache.get(modelData);
        if (cached != null) {
            return cached;
        }
        String resolved = computeResolvedModelKey(weaponId, type, modelData);
        this.resolvedKeyCache.put(modelData, resolved);
        return resolved;
    }

    private String computeResolvedModelKey(String weaponId, WeaponType type, int modelData) {
        if (frameModels != null && frameModels.containsKey(modelData)) {
            return frameModels.get(modelData);
        }
        String base = weaponId != null ? weaponId.toLowerCase() : "weapon";
        boolean melee = type != null && type.isMelee();
        if (modelData <= 0 || modelData == idleModelData) {
            return base;
        }
        if (deployFrames != null) {
            int idx = deployFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_deploy_" + (idx + 1);
            }
        }
        if (modelData == deployModelData) {
            return base + "_deploy";
        }
        if (adsInFrames != null) {
            int idx = adsInFrames.indexOf(modelData);
            if (idx >= 0) {
                return (melee ? base + "_stance_" : base + "_ads_in_") + (idx + 1);
            }
        }
        if (modelData == adsModelData) {
            return melee ? base + "_stance" : base + "_ads";
        }
        if (adsFireFrames != null) {
            int idx = adsFireFrames.indexOf(modelData);
            if (idx >= 0) {
                return (melee ? base + "_heavy_" : base + "_ads_fire_") + (idx + 1);
            }
        }
        if (modelData == adsFireModelData) {
            return melee ? base + "_heavy" : base + "_ads_fire";
        }
        if (adsOutFrames != null) {
            int idx = adsOutFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_ads_out_" + (idx + 1);
            }
        }
        if (fireFrames != null) {
            int idx = fireFrames.indexOf(modelData);
            if (idx >= 0) {
                return (melee ? base + "_slash_" : base + "_fire_") + (idx + 1);
            }
        }
        if (slash2Frames != null) {
            int idx = slash2Frames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_slash2_" + (idx + 1);
            }
        }
        if (heavyFrames != null) {
            int idx = heavyFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_heavy_" + (idx + 1);
            }
        }
        if (inspectFrames != null) {
            int idx = inspectFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_inspect_" + (idx + 1);
            }
        }
        if (reloadFrames != null) {
            int idx = reloadFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_reload_" + (idx + 1);
            }
        }
        if (reloadTacticalFrames != null) {
            int idx = reloadTacticalFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_reload_tactical_" + (idx + 1);
            }
        }
        if (reloadIntroFrames != null) {
            int idx = reloadIntroFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_reload_intro_" + (idx + 1);
            }
        }
        if (reloadLoopFrames != null) {
            int idx = reloadLoopFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_reload_loop_" + (idx + 1);
            }
        }
        if (reloadEndFrames != null) {
            int idx = reloadEndFrames.indexOf(modelData);
            if (idx >= 0) {
                return base + "_reload_end_" + (idx + 1);
            }
        }
        return base;
    }
}
