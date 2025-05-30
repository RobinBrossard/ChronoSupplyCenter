package data.campaign.econ.industries;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize; // HullSize 的正确导入

import lunalib.lunaSettings.LunaSettings;

public class SupplyReductionHullMod extends BaseHullMod {
	private static final String MOD_ID = "ChronoSupplyCenter";
	public static final Logger LOG = Global.getLogger(SupplyReductionHullMod.class);

	// =================================================================================
	// DEFAULT CONSTANT DECLARATIONS
	// =================================================================================
	private static final int DEFAULT_SUPPLY_BASE_CONFIG = 0;
	private static final int DEFAULT_CREW_BONUS_CONFIG = 0;
	private static final int DEFAULT_CARGO_BONUS_CONFIG = 0;
	private static final int DEFAULT_FUEL_BONUS_CONFIG = 0;
	private static final int DEFAULT_SPEED_BONUS_CONFIG = 0;
	private static final int DEFAULT_MANEUVER_BONUS_CONFIG = 0;
	private static final int DEFAULT_SENSOR_RANGE_BONUS_CONFIG = 0;
	private static final int DEFAULT_DETECTION_REDUCTION_CONFIG = 0;
	private static final int DEFAULT_HULL_BONUS_FLAT_CONFIG = 0;
	private static final int DEFAULT_FLUX_CAPACITY_BONUS_FLAT_CONFIG = 0;
	private static final int DEFAULT_FLUX_DISSIPATION_BONUS_FLAT_CONFIG = 0;
	private static final int DEFAULT_MAX_BURN_LEVEL_FLAT_CONFIG = 0;
	private static final int DEFAULT_SIGHT_BONUS_FLAT_CONFIG = 0;
	private static final int DEFAULT_ARMOR_BONUS_FLAT_VALUE_CONFIG = 0;
	private static final int DEFAULT_ZERO_FLUX_SPEED_BOOST_FLAT_CONFIG = 0;
	private static final int DEFAULT_NUM_FIGHTER_BAYS_BONUS_CONFIG = 0;

	private static final int DEFAULT_WEAPON_RANGE_CONFIG = 100;
	private static final int DEFAULT_MISSILE_RANGE_CONFIG = 100;
	private static final int DEFAULT_SHIELD_DAMAGE_TAKEN_CONFIG = 100;
	private static final int DEFAULT_WEAPON_FLUX_COST_CONFIG = 100;
	private static final int DEFAULT_FIGHTER_REFIT_TIME_CONFIG = 100;
	private static final int DEFAULT_ACCELERATION_CONFIG = 100;
	private static final int DEFAULT_DECELERATION_CONFIG = 100;
	private static final int DEFAULT_ENGINE_HEALTH_CONFIG = 100;
	private static final int DEFAULT_WEAPON_HEALTH_CONFIG = 100;
	private static final int DEFAULT_SHIELD_UNFOLD_RATE_CONFIG = 100;
	private static final int DEFAULT_OVERLOAD_TIME_CONFIG = 100;
	private static final int DEFAULT_CR_DECAY_CONFIG = 100;
	private static final int DEFAULT_FIGHTER_WING_RANGE_CONFIG = 100;
	// 移除了 DEFAULT_ORDNANCE_POINTS_CONFIG
	// =================================================================================
	// END DEFAULT CONSTANT DECLARATIONS
	// =================================================================================

	// =================================================================================
	// CLASS FIELD DECLARATIONS
	// =================================================================================
	private int supplyBaseConfig = DEFAULT_SUPPLY_BASE_CONFIG;
	private int crewBonusConfig = DEFAULT_CREW_BONUS_CONFIG;
	private int cargoBonusConfig = DEFAULT_CARGO_BONUS_CONFIG;
	private int fuelBonusConfig = DEFAULT_FUEL_BONUS_CONFIG;
	private int speedBonusConfig = DEFAULT_SPEED_BONUS_CONFIG;
	private int maneuverBonusConfig = DEFAULT_MANEUVER_BONUS_CONFIG;
	private int sensorRangeBonusConfig = DEFAULT_SENSOR_RANGE_BONUS_CONFIG;
	private int detectReductionConfig = DEFAULT_DETECTION_REDUCTION_CONFIG;
	private int hullBonusFlatConfig = DEFAULT_HULL_BONUS_FLAT_CONFIG;
	private int fluxCapacityBonusFlatConfig = DEFAULT_FLUX_CAPACITY_BONUS_FLAT_CONFIG;
	private int fluxDissipationBonusFlatConfig = DEFAULT_FLUX_DISSIPATION_BONUS_FLAT_CONFIG;
	private int maxBurnLevelFlatConfig = DEFAULT_MAX_BURN_LEVEL_FLAT_CONFIG;
	private int sightBonusFlatConfig = DEFAULT_SIGHT_BONUS_FLAT_CONFIG;
	private int armorBonusFlatValueConfig = DEFAULT_ARMOR_BONUS_FLAT_VALUE_CONFIG;
	private int zeroFluxSpeedConfig = DEFAULT_ZERO_FLUX_SPEED_BOOST_FLAT_CONFIG;
	private int numFighterBaysBonusConfig = DEFAULT_NUM_FIGHTER_BAYS_BONUS_CONFIG;

	private int weaponRangeConfig = DEFAULT_WEAPON_RANGE_CONFIG;
	private int missileRangeConfig = DEFAULT_MISSILE_RANGE_CONFIG;
	private int shieldDamageTakenConfig = DEFAULT_SHIELD_DAMAGE_TAKEN_CONFIG;
	private int weaponFluxCostConfig = DEFAULT_WEAPON_FLUX_COST_CONFIG;
	private int fighterRefitTimeConfig = DEFAULT_FIGHTER_REFIT_TIME_CONFIG;
	private int accelerationConfig = DEFAULT_ACCELERATION_CONFIG;
	private int decelerationConfig = DEFAULT_DECELERATION_CONFIG;
	private int engineHealthConfig = DEFAULT_ENGINE_HEALTH_CONFIG;
	private int weaponHealthConfig = DEFAULT_WEAPON_HEALTH_CONFIG;
	private int shieldUnfoldRateConfig = DEFAULT_SHIELD_UNFOLD_RATE_CONFIG;
	private int overloadTimeConfig = DEFAULT_OVERLOAD_TIME_CONFIG;
	private int crDecayConfig = DEFAULT_CR_DECAY_CONFIG;
	private int fighterWingRangeConfig = DEFAULT_FIGHTER_WING_RANGE_CONFIG;
	// 移除了 ordnancePointsConfig 字段
	// =================================================================================
	// END CLASS FIELD DECLARATIONS
	// =================================================================================

	private void printAllLoadedValues(String context) {
		LOG.info("--- Values " + context + " (raw int config values) ---");
		LOG.info("supplyBaseConfig = " + supplyBaseConfig);
		LOG.info("crewBonusConfig = " + crewBonusConfig);
		LOG.info("cargoBonusConfig = " + cargoBonusConfig);
		LOG.info("fuelBonusConfig = " + fuelBonusConfig);
		LOG.info("speedBonusConfig = " + speedBonusConfig);
		LOG.info("maneuverBonusConfig = " + maneuverBonusConfig);
		LOG.info("sensorRangeBonusConfig = " + sensorRangeBonusConfig);
		LOG.info("detectReductionConfig = " + detectReductionConfig);
		LOG.info("hullBonusFlatConfig = " + hullBonusFlatConfig);
		LOG.info("fluxCapacityBonusFlatConfig = " + fluxCapacityBonusFlatConfig);
		LOG.info("fluxDissipationBonusFlatConfig = " + fluxDissipationBonusFlatConfig);
		LOG.info("maxBurnLevelFlatConfig = " + maxBurnLevelFlatConfig);
		LOG.info("sightBonusFlatConfig = " + sightBonusFlatConfig);
		LOG.info("armorBonusFlatValueConfig = " + armorBonusFlatValueConfig);
		LOG.info("zeroFluxSpeedConfig = " + zeroFluxSpeedConfig);
		LOG.info("numFighterBaysBonusConfig = " + numFighterBaysBonusConfig);
		LOG.info("weaponRangeConfig = " + weaponRangeConfig);
		LOG.info("missileRangeConfig = " + missileRangeConfig);
		LOG.info("shieldDamageTakenConfig = " + shieldDamageTakenConfig);
		LOG.info("weaponFluxCostConfig = " + weaponFluxCostConfig);
		LOG.info("fighterRefitTimeConfig = " + fighterRefitTimeConfig);
		LOG.info("accelerationConfig = " + accelerationConfig);
		LOG.info("decelerationConfig = " + decelerationConfig);
		LOG.info("engineHealthConfig = " + engineHealthConfig);
		LOG.info("weaponHealthConfig = " + weaponHealthConfig);
		LOG.info("shieldUnfoldRateConfig = " + shieldUnfoldRateConfig);
		LOG.info("overloadTimeConfig = " + overloadTimeConfig);
		LOG.info("crDecayConfig = " + crDecayConfig);
		LOG.info("fighterWingRangeConfig = " + fighterWingRangeConfig);
		// LOG.info("ordnancePointsConfig = " + ordnancePointsConfig); // 移除了OP日志
		LOG.info("--- End of values " + context + " ---");
	}

	private void loadSettings() {
		// 重置所有字段
		supplyBaseConfig = DEFAULT_SUPPLY_BASE_CONFIG;
		crewBonusConfig = DEFAULT_CREW_BONUS_CONFIG;
		cargoBonusConfig = DEFAULT_CARGO_BONUS_CONFIG;
		fuelBonusConfig = DEFAULT_FUEL_BONUS_CONFIG;
		speedBonusConfig = DEFAULT_SPEED_BONUS_CONFIG;
		maneuverBonusConfig = DEFAULT_MANEUVER_BONUS_CONFIG;
		sensorRangeBonusConfig = DEFAULT_SENSOR_RANGE_BONUS_CONFIG;
		detectReductionConfig = DEFAULT_DETECTION_REDUCTION_CONFIG;
		hullBonusFlatConfig = DEFAULT_HULL_BONUS_FLAT_CONFIG;
		fluxCapacityBonusFlatConfig = DEFAULT_FLUX_CAPACITY_BONUS_FLAT_CONFIG;
		fluxDissipationBonusFlatConfig = DEFAULT_FLUX_DISSIPATION_BONUS_FLAT_CONFIG;
		maxBurnLevelFlatConfig = DEFAULT_MAX_BURN_LEVEL_FLAT_CONFIG;
		sightBonusFlatConfig = DEFAULT_SIGHT_BONUS_FLAT_CONFIG;
		armorBonusFlatValueConfig = DEFAULT_ARMOR_BONUS_FLAT_VALUE_CONFIG;
		zeroFluxSpeedConfig = DEFAULT_ZERO_FLUX_SPEED_BOOST_FLAT_CONFIG;
		numFighterBaysBonusConfig = DEFAULT_NUM_FIGHTER_BAYS_BONUS_CONFIG;
		weaponRangeConfig = DEFAULT_WEAPON_RANGE_CONFIG;
		missileRangeConfig = DEFAULT_MISSILE_RANGE_CONFIG;
		shieldDamageTakenConfig = DEFAULT_SHIELD_DAMAGE_TAKEN_CONFIG;
		weaponFluxCostConfig = DEFAULT_WEAPON_FLUX_COST_CONFIG;
		fighterRefitTimeConfig = DEFAULT_FIGHTER_REFIT_TIME_CONFIG;
		accelerationConfig = DEFAULT_ACCELERATION_CONFIG;
		decelerationConfig = DEFAULT_DECELERATION_CONFIG;
		engineHealthConfig = DEFAULT_ENGINE_HEALTH_CONFIG;
		weaponHealthConfig = DEFAULT_WEAPON_HEALTH_CONFIG;
		shieldUnfoldRateConfig = DEFAULT_SHIELD_UNFOLD_RATE_CONFIG;
		overloadTimeConfig = DEFAULT_OVERLOAD_TIME_CONFIG;
		crDecayConfig = DEFAULT_CR_DECAY_CONFIG;
		fighterWingRangeConfig = DEFAULT_FIGHTER_WING_RANGE_CONFIG;
		// ordnancePointsConfig = DEFAULT_ORDNANCE_POINTS_CONFIG; // 移除了OP重置

		if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
			LOG.info("LunaLib IS enabled. Attempting to load settings for MOD_ID: " + MOD_ID);
			try {
				supplyBaseConfig = LunaSettings.getInt(MOD_ID, "supplyBaseConfig");
				crewBonusConfig = LunaSettings.getInt(MOD_ID, "crewBonusConfig");
				cargoBonusConfig = LunaSettings.getInt(MOD_ID, "cargoBonusConfig");
				fuelBonusConfig = LunaSettings.getInt(MOD_ID, "fuelBonusConfig");
				speedBonusConfig = LunaSettings.getInt(MOD_ID, "speedBonusConfig");
				maneuverBonusConfig = LunaSettings.getInt(MOD_ID, "maneuverBonusConfig");
				sensorRangeBonusConfig = LunaSettings.getInt(MOD_ID, "sensorRangeBonusConfig");
				detectReductionConfig = LunaSettings.getInt(MOD_ID, "detectReductionConfig");
				hullBonusFlatConfig = LunaSettings.getInt(MOD_ID, "hullBonusFlatConfig");
				fluxCapacityBonusFlatConfig = LunaSettings.getInt(MOD_ID, "fluxCapacityBonusFlatConfig");
				fluxDissipationBonusFlatConfig = LunaSettings.getInt(MOD_ID, "fluxDissipationBonusFlatConfig");
				maxBurnLevelFlatConfig = LunaSettings.getInt(MOD_ID, "maxBurnLevelFlatConfig");
				sightBonusFlatConfig = LunaSettings.getInt(MOD_ID, "sightBonusFlatConfig");
				armorBonusFlatValueConfig = LunaSettings.getInt(MOD_ID, "armorBonusFlatValueConfig");
				zeroFluxSpeedConfig = LunaSettings.getInt(MOD_ID, "zeroFluxSpeedConfig");
				numFighterBaysBonusConfig = LunaSettings.getInt(MOD_ID, "numFighterBaysBonusConfig");
				weaponRangeConfig = LunaSettings.getInt(MOD_ID, "weaponRangeConfig");
				missileRangeConfig = LunaSettings.getInt(MOD_ID, "missileRangeConfig");
				shieldDamageTakenConfig = LunaSettings.getInt(MOD_ID, "shieldDamageTakenConfig");
				weaponFluxCostConfig = LunaSettings.getInt(MOD_ID, "weaponFluxCostConfig");
				fighterRefitTimeConfig = LunaSettings.getInt(MOD_ID, "fighterRefitTimeConfig");
				accelerationConfig = LunaSettings.getInt(MOD_ID, "accelerationConfig");
				decelerationConfig = LunaSettings.getInt(MOD_ID, "decelerationConfig");
				engineHealthConfig = LunaSettings.getInt(MOD_ID, "engineHealthConfig");
				weaponHealthConfig = LunaSettings.getInt(MOD_ID, "weaponHealthConfig");
				shieldUnfoldRateConfig = LunaSettings.getInt(MOD_ID, "shieldUnfoldRateConfig");
				overloadTimeConfig = LunaSettings.getInt(MOD_ID, "overloadTimeConfig");
				crDecayConfig = LunaSettings.getInt(MOD_ID, "crDecayConfig");
				fighterWingRangeConfig = LunaSettings.getInt(MOD_ID, "fighterWingRangeConfig");
				// ordnancePointsConfig = LunaSettings.getInt(MOD_ID, "ordnancePointsConfig");
				// // 移除了OP加载

				printAllLoadedValues("after attempting LunaSettings load (end of try block)");

			} catch (Exception e) {
				LOG.error("FAILED to load settings from LunaSettings for " + MOD_ID + ". Exception: ", e);
				printAllLoadedValues("after CATCHING EXCEPTION during LunaSettings load");
			}
		} else {
			LOG.info("LunaLib IS NOT enabled. Using Java defaults for MOD_ID: " + MOD_ID);
			printAllLoadedValues("as LunaLib NOT ENABLED (Java defaults)");
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		loadSettings();

		// 与上一版本相同，除了不包含任何OP修改逻辑
		stats.getSuppliesToRecover().unmodify(id);
		stats.getSuppliesToRecover().setBaseValue((float) supplyBaseConfig);
		stats.getMaxCrewMod().modifyFlat(id, crewBonusConfig);
		stats.getCargoMod().modifyFlat(id, cargoBonusConfig);
		stats.getFuelMod().modifyFlat(id, fuelBonusConfig);
		stats.getMaxSpeed().modifyFlat(id, (float) speedBonusConfig);
		stats.getMaxTurnRate().modifyFlat(id, (float) maneuverBonusConfig);
		stats.getSensorStrength().modifyFlat(id, (float) sensorRangeBonusConfig);
		stats.getSensorProfile().modifyFlat(id, -(float) detectReductionConfig);
		stats.getHullBonus().modifyFlat(id, hullBonusFlatConfig);
		stats.getFluxCapacity().modifyFlat(id, fluxCapacityBonusFlatConfig);
		stats.getFluxDissipation().modifyFlat(id, fluxDissipationBonusFlatConfig);
		stats.getMaxBurnLevel().modifyFlat(id, (float) maxBurnLevelFlatConfig);
		stats.getSightRadiusMod().modifyFlat(id, sightBonusFlatConfig);
		stats.getArmorBonus().modifyFlat(id, armorBonusFlatValueConfig);
		stats.getZeroFluxSpeedBoost().modifyFlat(id, (float) zeroFluxSpeedConfig);
		if (numFighterBaysBonusConfig != 0) {
			stats.getNumFighterBays().modifyFlat(id, numFighterBaysBonusConfig);
		}

		stats.getBallisticWeaponRangeBonus().modifyMult(id, weaponRangeConfig / 100.0f);
		stats.getEnergyWeaponRangeBonus().modifyMult(id, weaponRangeConfig / 100.0f);
		stats.getMissileWeaponRangeBonus().modifyMult(id, missileRangeConfig / 100.0f);
		stats.getShieldDamageTakenMult().modifyMult(id, shieldDamageTakenConfig / 100.0f);

		float weaponFluxCostMultiplier = weaponFluxCostConfig / 100.0f;
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, weaponFluxCostMultiplier);
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, weaponFluxCostMultiplier);
		stats.getMissileWeaponFluxCostMod().modifyMult(id, weaponFluxCostMultiplier);

		stats.getFighterRefitTimeMult().modifyMult(id, fighterRefitTimeConfig / 100.0f);
		stats.getAcceleration().modifyMult(id, accelerationConfig / 100.0f);
		stats.getDeceleration().modifyMult(id, decelerationConfig / 100.0f);
		stats.getEngineHealthBonus().modifyMult(id, engineHealthConfig / 100.0f);
		stats.getWeaponHealthBonus().modifyMult(id, weaponHealthConfig / 100.0f);
		stats.getShieldUnfoldRateMult().modifyMult(id, shieldUnfoldRateConfig / 100.0f);
		stats.getOverloadTimeMod().modifyMult(id, overloadTimeConfig / 100.0f);
		stats.getCRLossPerSecondPercent().modifyMult(id, crDecayConfig / 100.0f);

		if (fighterWingRangeConfig != 0 || DEFAULT_FIGHTER_WING_RANGE_CONFIG != 100) {
			stats.getFighterWingRange().modifyMult(id, fighterWingRangeConfig / 100.0f);
		}
	}

	// applyEffectsAfterShipCreation 方法已被完全移除
	// @Override
	// public void applyEffectsAfterShipCreation(ShipAPI ship, String id) { ... }

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) {
			loadSettings();
			StringBuilder sb = new StringBuilder();

			sb.append(String.format("\n无人舰占用点数: %d 点\n", supplyBaseConfig));
			sb.append(String.format("船员上限: +%d\n", crewBonusConfig));
			sb.append(String.format("货舱容量: +%d\n", cargoBonusConfig));
			sb.append(String.format("燃料容量: +%d\n", fuelBonusConfig));
			// 移除了OP加成的描述行
			sb.append(String.format("最高航速: +%d\n", speedBonusConfig));
			sb.append(String.format("舰船机动: +%d\n", maneuverBonusConfig));
			sb.append(String.format("传感器范围: +%d\n", sensorRangeBonusConfig));
			sb.append(String.format("被侦测范围: -%d\n", detectReductionConfig));
			sb.append(String.format("舰体结构: +%d\n", hullBonusFlatConfig));
			sb.append(String.format("额外装甲值: +%d\n", armorBonusFlatValueConfig));
			sb.append(String.format("幅能上限: +%d\n", fluxCapacityBonusFlatConfig));
			sb.append(String.format("幅能散逸: +%d\n", fluxDissipationBonusFlatConfig));
			sb.append(String.format("零幅能速度加成: +%d\n", zeroFluxSpeedConfig));
			if (numFighterBaysBonusConfig != 0) {
				sb.append(String.format("额外舰载机起降舱口: %+d\n", numFighterBaysBonusConfig));
			}
			sb.append(String.format("最大巡航等级: +%d\n", maxBurnLevelFlatConfig));
			sb.append(String.format("战术索敌范围: +%d\n", sightBonusFlatConfig));

			appendTargetPercentageStat(sb, "非导弹武器射程", weaponRangeConfig);
			appendTargetPercentageStat(sb, "导弹武器射程", missileRangeConfig);
			appendTargetPercentageStat(sb, "护盾承受伤害", shieldDamageTakenConfig);
			appendTargetPercentageStat(sb, "武器幅能消耗", weaponFluxCostConfig);
			appendTargetPercentageStat(sb, "舰载机整备时间", fighterRefitTimeConfig);
			appendTargetPercentageStat(sb, "引擎加速度", accelerationConfig);
			appendTargetPercentageStat(sb, "引擎减速度", decelerationConfig);
			appendTargetPercentageStat(sb, "引擎耐久", engineHealthConfig);
			appendTargetPercentageStat(sb, "武器耐久", weaponHealthConfig);
			appendTargetPercentageStat(sb, "护盾展开速率", shieldUnfoldRateConfig);
			appendTargetPercentageStat(sb, "过载时间", overloadTimeConfig);
			if (fighterWingRangeConfig != 0 || DEFAULT_FIGHTER_WING_RANGE_CONFIG != 100) {
				appendTargetPercentageStat(sb, "舰载机联队作战半径", fighterWingRangeConfig);
			}
			appendTargetPercentageStat(sb, "CR损耗速度", crDecayConfig, true);

			String result = sb.toString();
			if (result.endsWith("\n")) {
				result = result.substring(0, result.length() - 1);
			}
			return result;
		}
		return null;
	}

	private void appendTargetPercentageStat(StringBuilder sb, String label, int configValue) {
		appendTargetPercentageStat(sb, label, configValue, false);
	}

	private void appendTargetPercentageStat(StringBuilder sb, String label, int configValue, boolean isLast) {
		sb.append(label).append(": ");
		if (configValue == 100) {
			sb.append("不变");
		} else {
			sb.append(String.format("变为 %d%%", configValue));
		}
		if (!isLast) {
			sb.append("\n");
		}
	}
}