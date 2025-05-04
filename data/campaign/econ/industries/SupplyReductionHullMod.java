package data.campaign.econ.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import lunalib.lunaSettings.LunaSettings;

public class SupplyReductionHullMod extends BaseHullMod {
	private static final String MOD_ID = "ChronoSupplyCenter";

	// 默认常量
	private static final float DEFAULT_SUPPLY_BASE = 0f;
	private static final int DEFAULT_CREW_BONUS = 500;
	private static final int DEFAULT_CARGO_BONUS = 1000;
	private static final int DEFAULT_FUEL_BONUS = 1000;
	private static final float DEFAULT_SPEED_BONUS = 0f;
	private static final float DEFAULT_MANEUVER_BONUS = 0f;
	private static final float DEFAULT_SENSOR_RANGE_BONUS = 0f;
	private static final float DEFAULT_DETECTION_REDUCTION = 0f;
	private static final int DEFAULT_HULL_BONUS = 0;
	private static final float DEFAULT_WEAPON_RANGE_BONUS = 0f;
	private static final float DEFAULT_MISSILE_RANGE_BONUS = 0f;
	private static final float DEFAULT_FLUX_CAPACITY_BONUS = 0f;
	private static final float DEFAULT_FLUX_DISSIPATION_BONUS = 0f;
	private static final float DEFAULT_MAX_BURN_LEVEL = 0f;
	private static final int DEFAULT_SIGHT_BONUS = 0;

	// 类字段，用于 getDescriptionParam
	private float supplyBase = DEFAULT_SUPPLY_BASE;
	private int crewBonus = DEFAULT_CREW_BONUS;
	private int cargoBonus = DEFAULT_CARGO_BONUS;
	private int fuelBonus = DEFAULT_FUEL_BONUS;
	private float speedBonus = DEFAULT_SPEED_BONUS;
	private float maneuverBonus = DEFAULT_MANEUVER_BONUS;
	private float sensorRangeBonus = DEFAULT_SENSOR_RANGE_BONUS;
	private float detectReduction = DEFAULT_DETECTION_REDUCTION;
	private int hullBonus = DEFAULT_HULL_BONUS;
	private float weapRangeBonus = DEFAULT_WEAPON_RANGE_BONUS;
	private float missileRangeBonus = DEFAULT_MISSILE_RANGE_BONUS;
	private float fluxCapacityBonus = DEFAULT_FLUX_CAPACITY_BONUS;
	private float fluxDissipationBonus = DEFAULT_FLUX_DISSIPATION_BONUS;
	private float maxBurnLevel = DEFAULT_MAX_BURN_LEVEL;
	private int sightBonus = DEFAULT_SIGHT_BONUS;

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		// 先将字段重置为默认值
		supplyBase = DEFAULT_SUPPLY_BASE;
		crewBonus = DEFAULT_CREW_BONUS;
		cargoBonus = DEFAULT_CARGO_BONUS;
		fuelBonus = DEFAULT_FUEL_BONUS;
		speedBonus = DEFAULT_SPEED_BONUS;
		maneuverBonus = DEFAULT_MANEUVER_BONUS;
		sensorRangeBonus = DEFAULT_SENSOR_RANGE_BONUS;
		detectReduction = DEFAULT_DETECTION_REDUCTION;
		hullBonus = DEFAULT_HULL_BONUS;
		weapRangeBonus = DEFAULT_WEAPON_RANGE_BONUS;
		missileRangeBonus = DEFAULT_MISSILE_RANGE_BONUS;
		fluxCapacityBonus = DEFAULT_FLUX_CAPACITY_BONUS;
		fluxDissipationBonus = DEFAULT_FLUX_DISSIPATION_BONUS;
		maxBurnLevel = DEFAULT_MAX_BURN_LEVEL;
		sightBonus = DEFAULT_SIGHT_BONUS;

		// 如果安装了 LunaLib，则尝试读取玩家设置
		if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
			try {
				supplyBase = LunaSettings.getDouble(MOD_ID, "supplyRecoveryBase").floatValue();
				crewBonus = LunaSettings.getInt(MOD_ID, "crewSpaceBonus");
				cargoBonus = LunaSettings.getInt(MOD_ID, "cargoSpaceBonus");
				fuelBonus = LunaSettings.getInt(MOD_ID, "fuelSpaceBonus");
				speedBonus = LunaSettings.getDouble(MOD_ID, "speedBonus").floatValue();
				maneuverBonus = LunaSettings.getDouble(MOD_ID, "maneuverBonus").floatValue();
				sensorRangeBonus = LunaSettings.getDouble(MOD_ID, "sensorRangeBonus").floatValue();
				detectReduction = LunaSettings.getDouble(MOD_ID, "detectionRangeReduction").floatValue();
				hullBonus = LunaSettings.getInt(MOD_ID, "hullBonus");
				weapRangeBonus = LunaSettings.getDouble(MOD_ID, "weaponRangeBonus").floatValue();
				missileRangeBonus = LunaSettings.getDouble(MOD_ID, "missileRangeBonus").floatValue();
				fluxCapacityBonus = LunaSettings.getDouble(MOD_ID, "fluxCapacityBonus").floatValue();
				fluxDissipationBonus = LunaSettings.getDouble(MOD_ID, "fluxDissipationBonus").floatValue();
				maxBurnLevel = LunaSettings.getDouble(MOD_ID, "maxBurnLevel").floatValue();
				sightBonus = LunaSettings.getInt(MOD_ID, "sightRadiusBonus");

			} catch (Exception e) {
				// 读取失败时保持默认值
			}
		}

		// 应用到船体属性
		stats.getSuppliesToRecover().unmodify(id);
		stats.getSuppliesToRecover().setBaseValue(supplyBase);

		stats.getMaxCrewMod().modifyFlat(id, crewBonus);
		stats.getCargoMod().modifyFlat(id, cargoBonus);
		stats.getFuelMod().modifyFlat(id, fuelBonus);

		stats.getMaxSpeed().modifyFlat(id, speedBonus);
		stats.getMaxTurnRate().modifyFlat(id, maneuverBonus);
		stats.getSensorStrength().modifyFlat(id, sensorRangeBonus);
		stats.getSensorProfile().modifyFlat(id, -detectReduction);

		stats.getHullBonus().modifyFlat(id, hullBonus);

		stats.getBallisticWeaponRangeBonus().modifyPercent(id, weapRangeBonus);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, weapRangeBonus);
		stats.getMissileWeaponRangeBonus().modifyPercent(id, missileRangeBonus);

		stats.getFluxCapacity().modifyFlat(id, fluxCapacityBonus);
		stats.getFluxDissipation().modifyFlat(id, fluxDissipationBonus);
		stats.getMaxBurnLevel().modifyFlat(id, maxBurnLevel);
		stats.getSightRadiusMod().modifyFlat(id, sightBonus);

	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		switch (index) {
		case 0:
			return String.format("%.0f", supplyBase);
		case 1:
			return String.valueOf(crewBonus);
		case 2:
			return String.valueOf(cargoBonus);
		case 3:
			return String.valueOf(fuelBonus);
		case 4:
			return String.format("%.0f", speedBonus);
		case 5:
			return String.format("%.0f", maneuverBonus);
		case 6:
			return String.format("%.0f", sensorRangeBonus);
		case 7:
			return String.format("%.0f", detectReduction);
		case 8:
			return String.valueOf(hullBonus);
		case 9:
			return String.format("%.0f%%", weapRangeBonus);
		case 10:
			return String.format("%.0f%%", missileRangeBonus);
		case 11:
			return String.format("%.0f", fluxCapacityBonus);
		case 12:
			return String.format("%.0f", fluxDissipationBonus);
		case 13:
			return String.format("%.0f", maxBurnLevel);
		case 14:
			return String.valueOf(sightBonus);
		default:
			return null;
		}
	}
}
