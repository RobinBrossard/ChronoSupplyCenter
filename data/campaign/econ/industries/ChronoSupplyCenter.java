package data.campaign.econ.industries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

/**
 * “时光科技”——所有功能硬编码，含注释说明。 动态稀有物品产出 & 指定武器/战机/基础物资 保底补给
 *
 * 主要流程： 1) 检查与上次触发的间隔（DAYS_PER_CHECK），未到间隔则跳过。 2) 产出 special items（带概率）。 3)
 * 保底补给武器与战机。 4) 产出 AI 核心。 5) 产出遗物组件、舰船零件、宠物食品。 6) 统一提示。
 */
public class ChronoSupplyCenter extends BaseIndustry {

	// ====== 用户可调：产出周期 ======
	// 每隔多少天触发一次补给逻辑
	private static final int DAYS_PER_CHECK = 1; // 也可改为7实现每周一次
	private static final String MEM_KEY_SUFFIX = "_lastDay";

	// ====== 用户可调：各类库存阈值 ======
	private static final int INVENTORY_THRESHOLD_SP = 2; // special items 阈值
	private static final int INVENTORY_THRESHOLD = 30; // 武器阈值
	private static final int MAX_WEAPON_ADD = 10;
	private static final int FIGHTER_INVENTORY_THRESH = 30; // 战机阈值
	private static final int MAX_FIGHTER_ADD = 10;
	private static final int CORE_THRESHOLD = 3; // AI 核心阈值
	private static final int MAX_CORES_ADD = 1;
	private static final int RELIC_THRESHOLD = 200; // 遗物组件阈值
	private static final int MAX_RELIC_ADD = 100;
	private static final int SHIP_PARTS_THRESHOLD = 200; // 舰船零件阈值
	private static final int MAX_SHIP_PARTS_ADD = 100;
	private static final int PETFOOD_THRESHOLD = 20; // 宠物食品阈值
	private static final int MAX_PETFOOD_ADD = 10;
	// ====== 用户可调：宠物补给 ======
	private static final int PET_THRESHOLD = 1; // 库存低于 5 才补
	private static final int MAX_PET_ADD = 1; // 每次最多补 2 只

	@Override
	public void apply() {
		super.apply(true);
		if (market == null)
			return;

		// —— 只在第一次建造时清一次时间戳，其它时候都不清 ——
		String initKey = "$" + getModId() + "_initialized";
		String memKey = "$" + getModId() + MEM_KEY_SUFFIX;
		MemoryAPI memNo = market.getMemoryWithoutUpdate();
		MemoryAPI mem = market.getMemory();
		if (!Boolean.TRUE.equals(memNo.get(initKey))) {
			// 第一次 apply，清掉旧标记，并设置 initialized 标记
			mem.unset(memKey);
			mem.set(initKey, true);
		}

		// 1. 基础商品供应，基于市场等级 ×2
		int size = market.getSize() * 2;
		supply(Commodities.SUPPLIES, size);
		supply(Commodities.FUEL, size);
		supply(Commodities.CREW, size);
		supply(Commodities.MARINES, size);
		supply(Commodities.FOOD, size);
		supply(Commodities.ORGANICS, size);
		supply(Commodities.VOLATILES, size);
		supply(Commodities.ORE, size);
		supply(Commodities.RARE_ORE, size);
		supply(Commodities.METALS, size);
		supply(Commodities.RARE_METALS, size);
		supply(Commodities.HEAVY_MACHINERY, size);
		supply(Commodities.DOMESTIC_GOODS, size);
		supply(Commodities.ORGANS, market.getSize());
		supply(Commodities.DRUGS, market.getSize());
		supply(Commodities.HAND_WEAPONS, size);
		supply(Commodities.LUXURY_GOODS, size);
		supply(Commodities.LOBSTER, size);
		supply(Commodities.SHIPS, size);

		// 2. 数值加成，随基地等级线性增长
		int lvl = market.getSize();
		float incomeMult = 1f + 0.1f * lvl;
		float defenseMult = 1f + 0.5f * lvl;
		float stabilityFlat = 8f - lvl;
		float fleetSizeFlat = lvl;
		float qualityFlat = lvl;

		market.getIncomeMult().modifyMult(getModId() + "_income", incomeMult,
				"时光科技—收入 +" + Math.round((incomeMult - 1f) * 100) + "%");
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(getModId() + "_defense",
				defenseMult, "时光科技—地面防御 +" + Math.round((defenseMult - 1f) * 100) + "%");
		market.getStability().modifyFlat(getModId() + "_stability", stabilityFlat, "时光科技—稳定度 +" + (int) stabilityFlat);
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(getModId() + "_fleet_size",
				fleetSizeFlat, "时光科技—舰队规模 +" + (int) fleetSizeFlat);
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(getModId() + "_quality",
				qualityFlat, "时光科技—生产质量 +" + (int) qualityFlat);
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		if (market == null || !isFunctional())
			return;

		try {

			// 1. 拿到时钟和两种 memory
			CampaignClockAPI clock = Global.getSector().getClock();
			String memKey = "$" + getModId() + "_lastTimestamp";
			MemoryAPI memRead = market.getMemoryWithoutUpdate();
			MemoryAPI memWrite = market.getMemory();

			// 2. 读取上次触发的时间戳（第一次可能为 null）
			Object o = memRead.get(memKey);
			long lastTs;
			if (o instanceof Long) {
				lastTs = (Long) o;
			} else {
				// 第一次运行，先记录当前时间，不触发生产
				lastTs = clock.getTimestamp();
				memWrite.set(memKey, lastTs);
				return;
			}

			// 3. 计算已经过去的天数
			float daysPassed = clock.getElapsedDaysSince(lastTs);

			// 4. 调试打印
//			Global.getSector().getCampaignUI().addMessage(
//					String.format("调试：上次ts=%d, 当前ts=%d, 已过天数=%.2f", lastTs, clock.getTimestamp(), daysPassed));

			// 5. 判断间隔
			if (daysPassed < DAYS_PER_CHECK) {
				return; // 未到指定天数，跳过
			}

			// 获取 storage 子市场
			SubmarketAPI storage = market.getSubmarket("storage");
			if (storage == null) {
				Global.getLogger(getClass()).warn("找不到 storage 子市场，跳过补给");
				return;
			}
			CargoAPI cargo = storage.getCargo();

			// 3. 一次性发放蓝图等特殊物品 ——
			// 使用 market memory 记录是否已发过，key 不会因游戏重载而丢失
			String blueprintKey = getModId() + "_ChronoSupplyCenter_onetime_given";
			Map<String, Object> data = Global.getSector().getPersistentData();
			if (!data.containsKey(blueprintKey)) {
				// 下面按列表顺序发放，每种蓝图包仅 1 份
				List<SpecialConfig> specials = Arrays.asList(new SpecialConfig("special", "sensor_array", 1, 1f),
						new SpecialConfig("special", "pristine_nanoforge", 1, 1f), // pristine_nanoforge 完好的纳米锻炉
						new SpecialConfig("special", "LC_package", 1, 1f), // LC_package 卢德教会蓝图包
						new SpecialConfig("special", "LP_package", 1, 1f), // LP_package 卢德左径蓝图包
						new SpecialConfig("special", "XIV_package", 1, 1f), // XIV_package 第十四战斗群蓝图包
						new SpecialConfig("special", "drone_replicator", 1, 1f), // drone_replicator 战斗无人机复制器
						new SpecialConfig("special", "fullerene_spool", 1, 1f), // fullerene_spool 富勒烯线轴
						new SpecialConfig("special", "heg_aux_package", 1, 1f), // heg_aux_package 霸主军事配件蓝图包
						new SpecialConfig("special", "high_tech_package", 1, 1f), // high_tech_package 扩展纪元蓝图包
						new SpecialConfig("special", "low_tech_package", 1, 1f), // low_tech_package 主宰纪元蓝图包
						new SpecialConfig("special", "midline_package", 1, 1f), // midline_package 核心纪元蓝图包
						new SpecialConfig("special", "pirate_package", 1, 1f), // pirate_package 海盗蓝图包
						new SpecialConfig("special", "plasma_dynamo", 1, 1f) // plasma_dynamo 等离子发电机
				);
				for (SpecialConfig sc : specials) {
					sc.addToCargoSafely(cargo);
				}

				// 3) 打标记，后面绝不再来
				data.put(blueprintKey, true);

				// 弹出一次性提示
				Global.getSector().getCampaignUI().addMessage("时光科技：已发放所有一次性蓝图包和加成稀有物");
			}

			// 3. 稀有 special 按几率产出，增加趣味性
//            List<SpecialConfig> specials = Arrays.asList(
//                new SpecialConfig("special", "sensor_array",            1, 0.50f),
//                new SpecialConfig("special", "nanoforge",               1, 0.10f),
//                new SpecialConfig("special", "drone_recovery_unit",     1, 0.25f),
//                new SpecialConfig("special", "salvage_field_generator",1, 0.20f),
//                new SpecialConfig("special", "ship_blueprint",          1, 0.50f)
//            );
//            for (SpecialConfig sc : specials) {
//                int cur = sc.currentCount(cargo);
//                if (cur < INVENTORY_THRESHOLD_SP && Math.random() < sc.chance) {
//                    sc.addToCargoSafely(cargo);
//                }
//            }

			// 4. 武器保底补给
			List<String> weaponIds = Arrays.asList("heatseeker", // Heat Seeker 导弹
					"salamanderpod", // Salamander Pod 导弹
					"realitydisruptor", // Reality Disruptor 幻境破坏者
					"motelauncher", // Mote Launcher 微粒发射器
					"motelauncher_hf", // 高频微粒发射器
					"tachyonlance", // Tachyon Lance 超光速长矛
					"ionpulser", // Ion Pulser 离子脉冲炮
					"terminator_missile", // Terminator Missile 终结者导弹
					"pilum", // Pilum 标枪
					"pilum_large", // Pilum Large 大型标枪
					"rifttorpedo", // Rift Torpedo 裂隙鱼雷
					"reaper", // Reaper 歼灭者
					"dragon", // Dragon 龙形激光
					"typhoon", // Typhoon 台风级炮
					"cyclone", // Cyclone 气旋级炮
					"dragonpod", // Dragon Pod 龙形炸弹舱
					"gigacannon", // Giga Cannon 千兆加农炮
					"minelayer1", // Minelayer I 地雷投放器Ⅰ
					"minelayer2" // Minelayer II 地雷投放器Ⅱ add test
			);

			for (String id : weaponIds) {
				int cur = 0;
				try {
					cur = cargo.getNumWeapons(id);
				} catch (Exception ignore) {
				}
				if (cur < INVENTORY_THRESHOLD) {
					int add = Math.min(MAX_WEAPON_ADD, INVENTORY_THRESHOLD - cur);
					try {
						cargo.addWeapons(id, add);
					} catch (Exception ignore) {
					}
				}
			}

			// 5. 战机保底补给
			List<String> fighterIds = Arrays.asList("trident_wing", "perdition_wing", "dagger_wing", "flash_wing",
					"warthog_wing", "thunder_wing", "xyphos_wing");
			for (String id : fighterIds) {
				int cur = 0;
				try {
					cur = cargo.getNumFighters(id);
				} catch (Exception ignore) {
				}
				if (cur < FIGHTER_INVENTORY_THRESH) {
					int add = Math.min(MAX_FIGHTER_ADD, FIGHTER_INVENTORY_THRESH - cur);
					try {
						cargo.addFighters(id, add);
					} catch (Exception ignore) {
					}
				}
			}

			// 6. AI 核心产出
			for (String coreId : Arrays.asList(Commodities.ALPHA_CORE, Commodities.BETA_CORE, Commodities.GAMMA_CORE)) {
				int cur = (int) cargo.getCommodityQuantity(coreId);
				if (cur < CORE_THRESHOLD) {
					int add = Math.min(MAX_CORES_ADD, CORE_THRESHOLD - cur);
					cargo.addCommodity(coreId, add);
				}
			}

			// 7. 遗物组件、舰船零件、宠物食品产出
			CommoditySpecAPI relicSpec = Global.getSettings().getCommoditySpec("IndEvo_rare_parts");
			if (relicSpec != null) {
				int cur = (int) cargo.getCommodityQuantity("IndEvo_rare_parts");
				if (cur < RELIC_THRESHOLD) {
					int toAdd = Math.min(MAX_RELIC_ADD, RELIC_THRESHOLD - cur);
					cargo.addCommodity("IndEvo_rare_parts", toAdd);
				}
			}
			CommoditySpecAPI partsSpec = Global.getSettings().getCommoditySpec("IndEvo_parts");
			if (partsSpec != null) {
				int cur = (int) cargo.getCommodityQuantity("IndEvo_parts");
				if (cur < SHIP_PARTS_THRESHOLD) {
					int toAdd = Math.min(MAX_SHIP_PARTS_ADD, SHIP_PARTS_THRESHOLD - cur);
					cargo.addCommodity("IndEvo_parts", toAdd);
				}
			}
			CommoditySpecAPI petFoodSpec = Global.getSettings().getCommoditySpec("IndEvo_pet_food");
			if (petFoodSpec != null) {
				int cur = (int) cargo.getCommodityQuantity("IndEvo_pet_food");
				if (cur < PETFOOD_THRESHOLD) {
					int toAdd = Math.min(MAX_PETFOOD_ADD, PETFOOD_THRESHOLD - cur);
					cargo.addCommodity("IndEvo_pet_food", toAdd);
				}
			}

			// 8. 工革宠物补给（库存<阈值 & 单次上限）
			indevo.industries.petshop.memory.Pet pet;

			List<String> petIds = Arrays.asList("captain", // 前海盗舰长
					"lordandsaviour", // 护国公 Foog 二世
					"slaghound", // 矿渣猎犬
					"goopuppies", // 凝胶狗
					"voidsquid", // 星缘乌贼
					"bugdog", // 漏洞犬
					"schafunschaf" // 虚空羊
			);

			for (String petId : petIds) {
				CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(petId);
				if (spec != null) {
					int cur = (int) cargo.getCommodityQuantity(petId);
					if (cur < PET_THRESHOLD) {
						int toAdd = Math.min(MAX_PET_ADD, PET_THRESHOLD - cur);
						cargo.addCommodity(petId, toAdd);
					}
				} else {
					Global.getLogger(getClass()).warn("找不到宠物 commodity spec: " + petId + "，跳过补给");
				}
			}

			// 生产完成后，写回当前时间戳到持久 memory
			memWrite.set(memKey, clock.getTimestamp());

			// 最终提示 打搅人，默认关上。
			// Global.getSector().getCampaignUI().addMessage("时光科技本期已完成所有补给，详见储存舱。");

		} catch (Throwable t) {
			Global.getLogger(getClass()).error("ChronoSupplyCenter.advance 出错", t);
		}
	}

	@Override
	public void unapply() {
		super.unapply();
		if (market == null)
			return;
		market.getIncomeMult().unmodify(getModId() + "_income");
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(getModId() + "_defense");
		market.getStability().unmodify(getModId() + "_stability");
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(getModId() + "_fleet_size");
		market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodify(getModId() + "_quality");
	}

	@Override
	public boolean isAvailableToBuild() {
		return true;
	}

	@Override
	public boolean showWhenUnavailable() {
		return true;
	}

	private static class SpecialConfig {
		String type, id;
		int amount;
		float chance;

		SpecialConfig(String type, String id, int amount, float chance) {
			this.type = type;
			this.id = id;
			this.amount = amount;
			this.chance = chance;
		}

		int currentCount(CargoAPI cargo) {
			try {
				if ("special".equals(type)) {
					return (int) cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(id, null));
				}
			} catch (Exception ignore) {
			}
			return Integer.MAX_VALUE;
		}

		void addToCargoSafely(CargoAPI cargo) {
			try {
				cargo.addSpecial(new SpecialItemData(id, null), amount);
			} catch (Exception ignore) {
			}
		}
	}
}
