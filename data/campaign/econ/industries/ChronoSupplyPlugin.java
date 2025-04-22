package data.campaign.econ.industries;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

public class ChronoSupplyPlugin extends BaseModPlugin {
	@Override
	public void onNewGame() {
		registerResupplyScript();
	}

	@Override
	public void onGameLoad(boolean newGame) {
		registerResupplyScript();
	}

	private void registerResupplyScript() {
		SectorAPI sector = Global.getSector();
		// 防止重复
		if (!sector.getScripts().contains(FleetDailyResupply.class.getName())) {
			sector.addScript(new FleetDailyResupply());
		}
	}
}
