package data.campaign.econ.industries;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.IntervalUtil;

public class FleetDailyResupply implements EveryFrameScript {

	private static final float INTERVAL_DAYS = 1f; // 改 7f 就是每周一次
	private static final int SUPPLIES_THRESHOLD = 50; // 补给品阈值
	private static final int SUPPLIES_BATCH = 50; // 每次补给量
	private static final int FUEL_THRESHOLD = 50; // 燃料阈值
	private static final int FUEL_BATCH = 50; // 每次补给量
	private static final float CR_THRESHOLD = 0.7f; // 设置CR的阈值，例如低于70%就恢复
	private static final float CR_RECOVERY_AMOUNT = 1f; // 每次恢复的CR量，例如10%

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

		// —— 1) 补给品检查 ——
		float suppliesQty = cargo.getCommodityQuantity(Commodities.SUPPLIES);
		float suppliesSpace = cargo.getSpaceLeft();
		if (suppliesQty < SUPPLIES_THRESHOLD && suppliesSpace >= SUPPLIES_BATCH) {
			cargo.addCommodity(Commodities.SUPPLIES, SUPPLIES_BATCH);
			// 第一行，金黄色
			Global.getSector().getCampaignUI().addMessage("[#FFD700]--广告位招租- 时光科技-吃了么送餐快线 800-810-8888[]");
			// 第二行，白色（默认）
			Global.getSector().getCampaignUI().addMessage("出门在外，带够干粮哦，免费送您补给一份");
		}

		// —— 2) 燃料检查 ——
		float fuelQty = cargo.getCommodityQuantity(Commodities.FUEL);
		float fuelSpace = cargo.getSpaceLeft();
		if (fuelQty < FUEL_THRESHOLD && fuelSpace >= FUEL_BATCH) {
			cargo.addCommodity(Commodities.FUEL, FUEL_BATCH);
			// 第一行，红色
			Global.getSector().getCampaignUI().addMessage("[#FF0000]--广告位招租-- 时光科技-哒哒送油快线 800-810-8888[]");
			// 第二行，白色（默认）
			Global.getSector().getCampaignUI().addMessage("出门在外，油箱不能空，免费送您燃油一份");
			// —— 调试日志：打印舰队仓库所有物品 ——
			Global.getLogger(getClass()).info("== Fleet Cargo Contents ==");
			for (CargoStackAPI stack : cargo.getStacksCopy()) {
				String id = stack.getCommodityId();
				String name = stack.getDisplayName();
				float qty = stack.getSize();
				Global.getLogger(getClass()).info(String.format("Item ID: %s, Name: %s, Qty: %.0f", id, name, qty));
			}
		}

		// 3. 恢复战备值（CR）
		checkAndRestoreFleetCR(fleet); // 在每次每日检查时调用 CR 恢复方法
	}

	private void checkAndRestoreFleetCR(CampaignFleetAPI fleet) {
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member != null) {
				RepairTrackerAPI repairTracker = member.getRepairTracker();
				if (repairTracker != null) {
					float currentCR = repairTracker.getCR();
					if (currentCR < CR_THRESHOLD) {
						float newCR = Math.min(1f, currentCR + CR_RECOVERY_AMOUNT);
						repairTracker.setCR(newCR); // 使用 RepairTrackerAPI 的 setCR() 方法
						// 可选：发送消息
						// 第一行，金黄色
						Global.getSector().getCampaignUI().addMessage("[#FFD700]--广告位招租-- 时光科技-狗东快修 800-810-8888[]");
						// 第二行，白色（默认）
						Global.getSector().getCampaignUI().addMessage("道路千万条，安全第一条！帮你快速恢复了舰队CR");
					}
				}
			}
		}
	}
}
