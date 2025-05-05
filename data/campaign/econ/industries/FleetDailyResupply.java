package data.campaign.econ.industries;

import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import lunalib.lunaSettings.LunaSettings;

public class FleetDailyResupply implements EveryFrameScript {
	private static final String MOD_ID = "ChronoSupplyCenter";

	// ====== 可配置参数（原先的 static final 改为普通 static） ======
	private static int INTERVAL_DAYS; // 检查间隔（天）
	private static int SUPPLIES_THRESHOLD; // 补给品阈值
	private static int SUPPLIES_BATCH; // 补给批量倍数
	private static int FUEL_THRESHOLD; // 燃料阈值
	private static int FUEL_BATCH; // 燃料批量倍数
	private static float CR_THRESHOLD; // CR 阈值
	private static float CR_RECOVERY_AMOUNT; // CR 恢复量

	private static boolean ONcheckAndSupplySupplies = true;
	private static boolean ONcheckAndSupplyFuel = true;
	private static boolean ONcheckAndRestoreFleetCR = true;
	private static boolean ONcheckAndGiveFunds = true;

	// 新增：金币阈值与重置值
	private static float MAX_CREDITS_THRESHOLD;
	private static float MAX_CREDITS_RESET;

	static {
		boolean hasLuna = Global.getSettings().getModManager().isModEnabled("lunalib");

		INTERVAL_DAYS = hasLuna ? LunaSettings.getInt(MOD_ID, "intervalDays") : 1;
		SUPPLIES_THRESHOLD = hasLuna ? LunaSettings.getInt(MOD_ID, "suppliesThreshold") : 50;
		SUPPLIES_BATCH = hasLuna ? LunaSettings.getInt(MOD_ID, "suppliesBatch") : 20;
		FUEL_THRESHOLD = hasLuna ? LunaSettings.getInt(MOD_ID, "fuelThreshold") : 10;
		FUEL_BATCH = hasLuna ? LunaSettings.getInt(MOD_ID, "fuelBatch") : 10;
		CR_THRESHOLD = hasLuna ? LunaSettings.getDouble(MOD_ID, "crThreshold").floatValue() : 0.4f;
		CR_RECOVERY_AMOUNT = hasLuna ? LunaSettings.getDouble(MOD_ID, "crRecoveryAmount").floatValue() : 0.5f;
		ONcheckAndSupplySupplies = hasLuna ? LunaSettings.getBoolean(MOD_ID, "onCheckAndSupplySupplies") : true;
		ONcheckAndSupplyFuel = hasLuna ? LunaSettings.getBoolean(MOD_ID, "onCheckAndSupplyFuel") : true;
		ONcheckAndRestoreFleetCR = hasLuna ? LunaSettings.getBoolean(MOD_ID, "onCheckAndRestoreFleetCR") : true;
		ONcheckAndGiveFunds = hasLuna ? LunaSettings.getBoolean(MOD_ID, "onCheckAndGiveFunds") : true;

		// 读取新参数：金币上限与重置值
		MAX_CREDITS_THRESHOLD = hasLuna ? LunaSettings.getDouble(MOD_ID, "maxCreditsThreshold").floatValue()
				: 100_000_000f; // 默认 1 亿
		MAX_CREDITS_RESET = hasLuna ? LunaSettings.getDouble(MOD_ID, "maxCreditsReset").floatValue() : 0.2f; // 默认扣除20%
	}

	private final IntervalUtil tracker = new IntervalUtil(INTERVAL_DAYS, INTERVAL_DAYS);

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void advance(float amount) {
		tracker.advance(amount);
		if (!tracker.intervalElapsed())
			return;

		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		if (fleet == null)
			return;

		CargoAPI cargo = fleet.getCargo();

		checkAndSupplySupplies(cargo); // 加补给，出错概率小
		checkAndSupplyFuel(cargo); // 加油，出错概率小
		checkAndRestoreFleetCR(fleet); // 维修，魔改船出错概率大
		checkAndGiveFunds(); // 加钱，出错概率小
	}

	/** V5 500，我是秦始皇 */
	private void checkAndGiveFunds() {

		CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
		float money = cargo.getCredits().get();

		// —— 新增：金币过高时自动扣减 ——
		if (money > MAX_CREDITS_THRESHOLD) {
			float excess = money * MAX_CREDITS_RESET;
			cargo.getCredits().subtract(excess);
			String formatted = String.format("C$ %,.0f", excess);
			Global.getSector().getCampaignUI().addMessage(String.format("资金过多，自动扣缴星际税 %s", formatted), Color.YELLOW);
			// 不继续发放额外资金
			return;
		}

		// 原有：金币不足 1M 时发放 10M 资金
		if (!ONcheckAndGiveFunds)
			return;
		if (money < 1_000_000f) {
			cargo.getCredits().add(10_000_000f);
			Global.getSector().getCampaignUI().addMessage("你又一笔遗产到账，略解燃眉之急", new Color(255, 215, 0));
		}
	}

	/** 检查并补给补给品 */
	private void checkAndSupplySupplies(CargoAPI cargo) {
		if (!ONcheckAndSupplySupplies) {
			return;
		}
		float qty = cargo.getCommodityQuantity(Commodities.SUPPLIES);
		float space = cargo.getSpaceLeft();
		if (qty < SUPPLIES_THRESHOLD && space >= SUPPLIES_BATCH) {
			cargo.addCommodity(Commodities.SUPPLIES,
					SUPPLIES_BATCH * cargo.getFleetData().getFleet().getFleetSizeCount());
			Global.getSector().getCampaignUI().addMessage("--广告位招租：时光科技-吃了么送餐快线 800-810-8888", Color.BLUE);
			Global.getSector().getCampaignUI().addMessage("出门在外，带够干粮哦，免费送您补给一份");
		}
	}

	/**
	 * 检查并补给燃料
	 * 
	 * @return 如果进行了补给则返回 true，否则 false
	 */
	private void checkAndSupplyFuel(CargoAPI cargo) {
		if (!ONcheckAndSupplyFuel) {
			return;
		}
		float qty = cargo.getCommodityQuantity(Commodities.FUEL);
		float space = cargo.getSpaceLeft();
		if (qty < FUEL_THRESHOLD && space >= FUEL_BATCH) {
			cargo.addCommodity(Commodities.FUEL, FUEL_BATCH * cargo.getFleetData().getFleet().getFleetSizeCount());
			Global.getSector().getCampaignUI().addMessage("--广告位招租：时光科技-哒哒送油快线 800-810-8888", Color.RED);
			Global.getSector().getCampaignUI().addMessage("出门在外，油箱不能空，免费送您燃油一份");
//			logCargoContents(cargo);
			return;
		}

	}

	/** 检查并恢复舰队战备值（CR） */
	private void checkAndRestoreFleetCR(CampaignFleetAPI fleet) {
		if (!ONcheckAndRestoreFleetCR) {
			return;
		}
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			RepairTrackerAPI rt = member.getRepairTracker();
			if (rt != null && rt.getCR() < CR_THRESHOLD) {

				float newCR = Math.min(1f, rt.getCR() + CR_RECOVERY_AMOUNT);
//======== 测试节============
//				float baseCR, crDecreaseRate = 0f;
//				baseCR = rt.getBaseCR() * 100;
//				crDecreaseRate = rt.getDecreaseRate() * 100;
//
//				Global.getSector().getCampaignUI().addMessage(String.format(
//						"[CR维修测试] current=%.2f, TH=%.2f, new=%.2f, baseCR=%.2f,crDecreaseRate=%.2f, result=%.2f",
//						rt.getCR(), CR_THRESHOLD, newCR, baseCR, crDecreaseRate, crDecreaseRate * baseCR));
//==============================
				rt.setCR(newCR);
				Color gold = new Color(255, 215, 0);
				Global.getSector().getCampaignUI().addMessage("--广告位招租：时光科技-狗东快修 800-810-8888", gold);
				Global.getSector().getCampaignUI().addMessage("道路千万条，安全第一条！（如果维修频繁，保证舰队无人舰点数不超限）");

			}
		}
	}

	/** 将舰队货舱内容详细地打印到日志，这是测试数据用的，不用管 */
	private void logCargoContents(CargoAPI cargo) {
		Global.getLogger(getClass()).info("== Fleet Cargo Contents ==");
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			// 1. 普通商品
			if (stack.isCommodityStack()) {
				String id = stack.getCommodityId();
				String name = stack.getDisplayName();
				float qty = stack.getSize();
				Global.getLogger(getClass()).info(String.format("[COMMODITY] ID=%s, Name=%s, Qty=%.0f", id, name, qty));
				continue;
			}
			// 2. 特殊物品
			if (stack.isSpecialStack()) {
				SpecialItemData sid = stack.getSpecialDataIfSpecial();
				String specId = sid.getId();
				String subtype = sid.getData();
				String name = stack.getDisplayName();
				int qty = (int) stack.getSize();
				Global.getLogger(getClass()).info(
						String.format("[SPECIAL ] specId=%s, subtype=%s, Name=%s, Qty=%d", specId, subtype, name, qty));
				continue;
			}
			// 3. 武器库存
			if (stack.isWeaponStack()) {
				WeaponSpecAPI wspec = stack.getWeaponSpecIfWeapon();
				String weaponId = wspec.getWeaponId();
				String name = stack.getDisplayName();
				int qty = (int) stack.getSize();
				Global.getLogger(getClass())
						.info(String.format("[WEAPON  ] ID=%s, Name=%s, Qty=%d", weaponId, name, qty));
				continue;
			}
			// 4. 战机库存
			if (stack.isFighterWingStack()) {
				FighterWingSpecAPI fws = stack.getFighterWingSpecIfWing();
				String wingId = fws.getId();
				String name = stack.getDisplayName();
				int qty = (int) stack.getSize();
				Global.getLogger(getClass())
						.info(String.format("[FTR_WING] ID=%s, Name=%s, Qty=%d", wingId, name, qty));
				continue;
			}
			// 5. 停泊舰船
			if (stack.getData() instanceof FleetMemberAPI) {
				FleetMemberAPI member = (FleetMemberAPI) stack.getData();
				String variantId = member.getVariant().getHullVariantId();
				String name = member.getVariant().getFullDesignationWithHullName();
				int qty = (int) stack.getSize();
				Global.getLogger(getClass())
						.info(String.format("[MOTH_SHIP] Variant=%s, Name=%s, Qty=%d", variantId, name, qty));
				continue;
			}
			// 6. 其它类型
			String name = stack.getDisplayName();
			int qty = (int) stack.getSize();
			Global.getLogger(getClass())
					.info(String.format("[OTHER   ] Type=%s, Name=%s, Qty=%d", stack.getType(), name, qty));
		}
	}

}
