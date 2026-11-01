package com.kador.corpsegraves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kador.corpsegraves.CorpseGraves;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server authoritative configuration. Every value can be changed in
 * config/corpsegraves.json without rebuilding the mod.
 */
public final class GraveConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static Path path;
	private static GraveConfig instance = new GraveConfig();

	public boolean enabled = true;
	public int despawnSeconds = 3600;
	public int unlockSeconds = 1800;
	public boolean pauseDespawnWhileOwnerOffline = true;
	public boolean pauseDespawnInUnloadedChunks = true;
	public boolean operatorsBypassProtection = true;
	public boolean ownedGravesAlwaysAccessible = true;
	public boolean storeXp = true;
	public double xpRecoveryPercent = 50.0;
	public boolean respectCurseOfVanishing = true;
	public int maxGravesPerPlayer = 10;
	public boolean dropLootOnDespawn = true;
	public boolean removeWhenEmpty = true;
	public boolean vanillaClientQuickLoot = true;
	public boolean announceCoordinates = true;

	public Corpse corpse = new Corpse();
	public Hologram hologram = new Hologram();
	public VoidRescue voidRescue = new VoidRescue();

	public static final class Corpse {
		public String pose = "SLEEPING";
		public boolean displayArmor = false;
		public boolean displayOffhand = false;
		public double yOffset = 0.0;
		public double forwardOffset = 0.0;
		public float yawOffset = 0.0f;
		public float hitboxWidth = 3.2f;
		public float hitboxHeight = 1.4f;
		public double hitboxForwardOffset = 0.0;
		public boolean particles = true;
		public int particleIntervalTicks = 40;
	}

	public static final class Hologram {
		public boolean enabled = true;
		public boolean showName = true;
		public boolean showDespawn = true;
		public boolean showProtection = true;
		public double height = 1.45;
		public double forwardOffset = 0.0;
		public double scale = 1.4;
		public int lineWidth = 220;
		public String background = "#66000000";
		public String nameColor = "#FFAA00";
		public String despawnColor = "#55FFFF";
		public String protectedColor = "#FF5555";
		public String unlockedColor = "#55FF55";
		public boolean shadow = true;
		public boolean seeThrough = false;
		public double viewRange = 64.0;
	}

	public static final class VoidRescue {
		public boolean enabled = true;
		public int triggerBelowY = -60;
		public int searchRadius = 8;
		public boolean placeOnSurface = true;
	}

	private GraveConfig() {
	}

	public static GraveConfig get() {
		return instance;
	}

	public static synchronized void load() {
		Path configDir = FMLPaths.CONFIGDIR.get();
		path = configDir.resolve("corpsegraves.json");
		try {
			Files.createDirectories(configDir);
			if (!Files.exists(path)) {
				save();
				return;
			}
			String json = Files.readString(path, StandardCharsets.UTF_8);
			GraveConfig loaded = GSON.fromJson(json, GraveConfig.class);
			if (loaded == null) {
				throw new JsonSyntaxException("empty config");
			}
			if (loaded.corpse == null) loaded.corpse = new Corpse();
			if (loaded.hologram == null) loaded.hologram = new Hologram();
			if (loaded.voidRescue == null) loaded.voidRescue = new VoidRescue();
			boolean legacy = !com.google.gson.JsonParser.parseString(json).getAsJsonObject().has("onlyOwnerCanLoot");
			instance = loaded.sanitized();
			if (legacy) {
				instance.applyUpdateDefaults();
			}
			save();
		} catch (Exception e) {
			CorpseGraves.LOGGER.error("Could not read config, using defaults", e);
			instance = new GraveConfig();
		}
	}

	public static synchronized void reload() {
		load();
	}

	public static synchronized void save() {
		if (path == null) {
			return;
		}
		try {
			Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
		} catch (IOException e) {
			CorpseGraves.LOGGER.error("Could not save config", e);
		}
	}

	private GraveConfig sanitized() {
		despawnSeconds = Math.max(-1, despawnSeconds);
		unlockSeconds = Math.max(-1, unlockSeconds);
		xpRecoveryPercent = Math.max(0.0, Math.min(100.0, xpRecoveryPercent));
		maxGravesPerPlayer = Math.max(1, maxGravesPerPlayer);
		corpse.particleIntervalTicks = Math.max(5, corpse.particleIntervalTicks);
		hologram.scale = Math.max(0.5, Math.min(8.0, hologram.scale));
		hologram.height = Math.max(0.2, Math.min(6.0, hologram.height));
		hologram.viewRange = Math.max(8.0, Math.min(256.0, hologram.viewRange));
		hologram.lineWidth = Math.max(20, Math.min(1000, hologram.lineWidth));
		voidRescue.searchRadius = Math.max(0, Math.min(32, voidRescue.searchRadius));
		return this;
	}

	/**
	 * Existing installations keep their world config; this quietly adopts the
	 * new owner-only, 50% XP and slimmer hologram defaults once.
	 */
	private void applyUpdateDefaults() {
		xpRecoveryPercent = 50.0;
		corpse.displayArmor = false;
		corpse.displayOffhand = false;
		corpse.hitboxWidth = 3.2f;
		corpse.hitboxHeight = 1.4f;
		corpse.hitboxForwardOffset = 0.0;
		hologram.scale = 1.4;
		hologram.height = 1.45;
		hologram.lineWidth = 220;
		hologram.background = "#66000000";
	}

	public static int parseArgb(String value, int fallback) {
		try {
			String hex = value.trim();
			if (hex.startsWith("#")) {
				hex = hex.substring(1);
			}
			if (hex.startsWith("0x") || hex.startsWith("0X")) {
				hex = hex.substring(2);
			}
			long parsed = Long.parseLong(hex, 16);
			if (hex.length() <= 6) {
				parsed |= 0xFF000000L;
			}
			return (int) parsed;
		} catch (Exception e) {
			return fallback;
		}
	}

	public static int parseRgb(String value, int fallback) {
		return parseArgb(value, fallback) & 0xFFFFFF;
	}
}
