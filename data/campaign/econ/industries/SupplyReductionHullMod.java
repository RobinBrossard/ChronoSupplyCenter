package data.campaign.econ.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import lunalib.lunaSettings.LunaSettings;

public class SupplyReductionHullMod extends BaseHullMod {
	private static final String MOD_ID = "ChronoSupplyCenter";

	// 回退常量（当未安装 LunaLib 或读取失败时使用）
	private static final float DEFAULT_SUPPLY_BASE = 0f;
	private static final int DEFAULT_CREW_BONUS = 500;
	private static final int DEFAULT_CARGO_BONUS = 1000;
	private static final int DEFAULT_FUEL_BONUS = 1000;
	float supplyBase = DEFAULT_SUPPLY_BASE;
	int crewBonus = DEFAULT_CREW_BONUS;
	int cargoBonus = DEFAULT_CARGO_BONUS;
	int fuelBonus = DEFAULT_FUEL_BONUS;

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

		// 只有当 LunaLib 启用时才尝试读取
		if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
			supplyBase = LunaSettings.getDouble(MOD_ID, "supplyRecoveryBase").floatValue();
			crewBonus = LunaSettings.getInt(MOD_ID, "crewSpaceBonus");
			cargoBonus = LunaSettings.getInt(MOD_ID, "cargoSpaceBonus");
			fuelBonus = LunaSettings.getInt(MOD_ID, "fuelSpaceBonus");
		}

		// 强制覆盖基础恢复量
		stats.getSuppliesToRecover().unmodify(id);
		stats.getSuppliesToRecover().setBaseValue(supplyBase);

		// 其它扁平加成
		stats.getMaxCrewMod().modifyFlat(id, crewBonus);
		stats.getCargoMod().modifyFlat(id, cargoBonus);
		stats.getFuelMod().modifyFlat(id, fuelBonus);
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
		default:
			return null;
		}
	}
}
