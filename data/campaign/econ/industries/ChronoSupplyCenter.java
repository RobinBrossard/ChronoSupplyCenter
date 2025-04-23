package data.campaign.econ.industries;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

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

				// === 以下添加舰船到 storage 子市场 ===

				// 初始化 mothballed ships 列表
				cargo.initMothballedShips(market.getFactionId());

				// 准备舰船 Hull ID 列表（代码注释为中文名称）
				List<String> ships = Arrays.asList(
//======= 下面这几个是  舰船/武器拓展 [Ship/Weapon Pack] MOD 的新增船。不过这个MOD优化可能有问题，卡机器
//						"swp_boss_doom", // 路西法
//						"swp_boss_dominator_luddic_path", // 统治者 (LP)
//						"swp_boss_frankenstein", // 弗兰肯斯坦
//						"swp_boss_odyssey", // 伊利亚特
//						"swp_boss_atlas", // 利维坦
//						"swp_boss_onslaught_luddic_path", // 攻势 (LP)
//						"swp_boss_onslaught", // 阿瑞斯
//						"swp_boss_conquest", // 尼姬
//						"swp_boss_paragon", // 宙斯

//======  以下是原版，放心用 
						"paragon", // 典范
						"pegasus", // 天马
						"onslaught", // 攻势
						"radiant", // 辐射
						"conquest", // 征服者
						"legion", // 军团
						"retribution", // 惩戒
						"nova", // 新星
						"astral", // 星体
						"ziggurat", // 通灵塔
						"prometheus2", // 普罗米修斯 Mk.II
						"invictus", // 不败
						"atlas2", // 阿特拉斯 Mk.II
						"dominator", // 统治者
						"odyssey" // 奥德赛
				);

				for (String hullId : ships) {
					addShipsToStorage(hullId, 1);
				}

				// 给玩家增加一千万资金
				CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
				playerCargo.getCredits().add(10000000f);
				Color goldColor = new Color(255, 215, 0);
				Global.getSector().getCampaignUI().addMessage("你小时候的邻居王叔叔过世了，给你留下的大笔遗产已到账", goldColor);

				// 3) 打标记，后面绝不再来
				data.put(blueprintKey, true);

				// 弹出一次性提示
				Global.getSector().getCampaignUI().addMessage("时光科技：发放一次性蓝图包、稀有物和超级战舰奖励");
			}

			// 4. 武器保底补给。列表是全部标准版武器，射程排序，打开了常用好用和稀有的，其它的想要自己开，但有些特殊的，是装不了的，只占格子
			List<String> weaponIds = Arrays.asList("pilum", // Pilum 短矛 LRM 发射器
					"pilum_large", // Pilum Large 短矛 LRM 投射舱
//				    "harpoon",             // Harpoon 鱼叉 MRM
//				    "harpoon_single",      // Harpoon Single 鱼叉 MRM (双发)
//				    "harpoonpod",          // Harpoon Pod 鱼叉 MRM 发射舱
					"hurricane", // Hurricane 飓风 MIRV 发射器
					"hydra", // Hydra 九头蛇 MDEM 发射器
//				    "squall",              // Squall 暴风 MLRS
//				    "resonatormrm",        // Resonator MRM 共鸣体 MRM 发射器
//				    "bomb",                // Bomb 标准炸弹发射架
					"heatseeker", // Heatseeker 火蛇 MRM
					"dragon", // Dragon 龙炎 DEM 鱼雷
					"salamanderpod", // Salamander Pod 火蛇 MRM 发射舱
//				    "phasecl_bomber",      // Phasecl Bomber 感应空雷发射器 (战机型)
//				    "dragonpod",           // Dragon Pod 龙炎 鱼雷 发射舱
//				    "rifttorpedo",         // Rift Torpedo 裂隙鱼雷发射器
//				    "clusterbomb",         // Clusterbomb 毒刺-级感应空雷
//				    "annihilator",         // Annihilator 歼灭者火箭炮
//				    "annihilator_fighter", // Annihilator Fighter 歼灭者火箭发射器
//				    "breach",              // Breach 破舱 SRM
					"gorgon", // Gorgon 戈耳工 DEM SRM
					"gorgonpod", // Gorgon Pod 戈耳工 SRM 发射舱
//				    "breachpod",           // Breach Pod 破舱 SRM 发射舱
//				    "annihilatorpod",      // Annihilator Pod 歼灭者火箭发射舱
//				    "terminator_missile",  // Terminator Missile 终结者导弹改装
//				    "locust",              // Locust 蝗虫 SRM 发射器
					"gauss", // Gauss 高斯炮
					"reaper", // Reaper 死神鱼雷
					"atropos", // Atropos 阿特罗波斯鱼雷发射架
					"atropos_single", // Atropos Single 阿特罗波斯鱼雷 (单发)
					"hammer", // Hammer 大锤级鱼雷
					"hammer_single", // Hammer Single 大锤级鱼雷 (单发)
					"sabot", // Sabot 赛博 SRM
					"sabot_single", // Sabot Single 赛博 SRM (双发)
					"sabot_fighter", // Sabot Fighter 赛博 SRM (单发)
					"gazer", // Gazer 眼魔 DEM SRM
					"gazerpod", // Gazer Pod 眼魔 SRM 发射舱
					"sabotpod", // Sabot Pod 赛博 SRM 发射舱
//				    "jackhammer",          // Jackhammer 气锤
					"typhoon", // Typhoon 台风级死神鱼雷发射器
					"cyclone", // Cyclone 旋风型死神鱼雷发射器
					"hammerrack", // Hammerrack 大锤鱼雷发射舱
					"amsrm", // AMSRM 反物质 SRM 发射器
					"heavymauler", // Heavy Mauler 重型撕裂者
					"hveldriver", // Hveldriver 高速打击者
					"swarmer", // Swarmer 蜂群 SRM 发射器
					"swarmer_fighter", // Swarmer Fighter 蜂群 SRM 发射器
//					"gazer_payload", // Gazer Payload 眼魔 - 光束
//					"dragon_payload", // Dragon Payload 龙炎 - 光束
					"phasecl", // Phasecl 感应空雷发射器
//				    "taclaser",            // Taclaser 战术激光炮
					"gravitonbeam", // Graviton Beam 引力子束
					"irautolance", // IRAutoLance IR 自动长矛
					"ionbeam", // Ion Beam 离子束
					"hil", // HIL 高强度激光
					"tachyonlance", // Tachyon Lance 速子长矛
//				    "riftcascade",         // Rift Cascade 裂隙洪流发射极
//				    "vpdriver",            // VPDriver 不稳定粒子投射器
//				    "realitydisruptor",    // Reality Disruptor 现实干扰器
//				    "minelayer1",          // Minelayer I 感应空雷
//				    "minelayer2",          // Minelayer II 重型感应空雷
//				    "od_bomblauncher",     // OD Bomb Launcher 猎户座装置 炸弹发射器
//				    "nb_bomblauncher",     // NB Bomb Launcher 新星爆裂驱动器 炸弹发射器
//				    "tpc",                 // TPC 热脉冲加农炮
					"hephag", // Hephag 赫菲斯托斯突击炮
					"mark9", // Mark IX 马克 IX 自动炮
					"devastator", // Devastator 蹂躏者加农炮
					"mjolnir", // Mjolnir 雷神炮
					"hellbore", // Hellbore 炼狱炮
//				    "heavyac",             // Heavy AC 重型自动炮
//				    "lrpdlaser",           // LRPD Laser LR PD 激光炮
					"guardian", // Guardian 保卫者 PD 系统
					"lightac", // Light AC 轻型自动炮
//				    "lightag",             // Light AG 轻型突击炮
					"lightneedler", // Light Needler 轻型针刺
					"railgun", // Railgun 轨道炮
					"shredder", // Shredder 噪音
//				    "heavymortar",         // Heavy Mortar 重型迫击炮
//				    "arbalest",            // Arbalest 劲弩自动炮
					"heavyneedler", // Heavy Needler 重型针刺
					"multineedler", // Multi Needler 风暴针刺
//				    "gorgon_payload",      // Gorgon Payload 戈耳工 - 光束
//				    "hydra_payload",       // Hydra Payload 九头蛇 - 光束
					"plasma", // Plasma 等离子炮
					"autopulse", // Autopulse 自动脉冲激光
					"gigacannon", // Giga Cannon 千兆加农炮
//				    "disintegrator",       // Disintegrator 裂解炮
//				    "interdictorbeam",     // Interdictor Beam 指示光束
//				    "lightmortar",         // Light Mortar 轻型迫击炮
//				    "lightdualac",         // Light Dual AC 轻型双管自动炮
//				    "mininglaser",         // Mining Laser 钻探激光炮
					"phasebeam", // Phase Beam 相位长矛
//				    "pulselaser",          // Pulse Laser 脉冲激光炮
					"heavyblaster", // Heavy Blaster 重型冲击波
//				    "kineticblaster",      // Kinetic Blaster 动能冲击波
//				    "heavyburst",          // Heavy Burst 重型瞬发激光炮
//				    "riftlance",           // Rift Lance 裂隙长矛
//				    "riftbeam",            // Rift Beam 裂隙射线
//				    "cryoblaster",         // Cryoblaster 低温冲击波
//				    "fragbomb",            // Fragbomb 毒刺-级感应空雷
//				    "flak",                // Flak 高射炮
//				    "ioncannon",           // Ion Cannon 离子炮
//				    "ioncannon_fighter",   // Ion Cannon Fighter 离子炮 (战机型)
//				    "irpulse",             // IR Pulse Laser IR 脉冲激光炮
//				    "irpulse_fighter",     // IR Pulse Laser Fighter IR 脉冲激光炮 (战机型)
//				    "pdburst",             // PD Burst 瞬发 PD 激光炮
//				    "pdburst_fighter",     // PD Burst Fighter 瞬发 PD 激光炮 (战机型)
					"miningblaster", // Mining Blaster 钻探冲击波
					"ionpulser", // Ion Pulser 离子脉冲
//				    "minipulser",          // Mini Pulser 微型脉冲
//				    "flarelauncher3",      // Flare Launcher 3 追踪热诱弹发射器
//				    "motelauncher",        // Mote Launcher 光尘发射器
//				    "motelauncher_hf",     // Mote Launcher HF 光尘发射器
					"heavymg", // Heavy MG 重机枪
					"chaingun", // Chaingun 突击链炮
//				    "lightmortar_fighter", // Light Mortar (High Delay)
//				    "dualflak",            // Dual Flak 双管高射炮
//				    "pdlaser",             // PD Laser PD 激光炮
					"amblaster", // AM Blaster 反物质冲击波
//				    "cryoflux",            // Cryoflux 低温喷射器
//				    "flarelauncher1",      // Flare Launcher 1 热诱弹发射器
//				    "lightmg",             // Light MG 轻机枪
//				    "lightdualmg",         // Light Dual MG 轻型双管机枪
					"shockrepeater", // Shock Repeater 电冲连发器
//				    "canister_flak",       // Canister Flak 罐式高射炮
//					"flarelauncher2", // Flare Launcher 2 诱饵发射器
//					"flarelauncher21", // Flare Launcher 21 诱饵发射器 (单发)
					"vulcan" // Vulcan 火神炮
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

			// 5. 战机保底补给，同样，航程排序，名称和装配点见注释，想要多的自己开。注释为战机名称，类型，装配点
			List<String> fighterIds = Arrays.asList("broadsword_wing", // 宽剑战机 – 重型战斗机 – 10
//				    "warthog_wing",                  // 野猪战机 – 重型战斗机 – 10
//				    "thunder_wing",                  // 雷霆截击机 – 重型截击机 – 15
//				    "gladius_wing",                  // 格拉迪乌斯截击机 – 重型截击机 – 10
//				    "xyphos_wing",                   // 西福斯支援战机 – 支援战斗机 – 15
					"sarissa_wing", // 长矛支援战机 – 支援战斗机 – 10
//				    "claw_wing",                     // 爪式战机 – 战斗机 – 8
//				    "lux_wing",                      // 路克斯无人战机 – 无人战斗机 – 12
//				    "spark_wing",                    // 火花无人截击机 – 无人截击机 – 10
//				    "mining_drone_wing",             // 采矿无人机中队 – 无人机 – 10
//				    "borer_wing",                    // 钻孔者无人机中队 – 无人机 – 5
//				    "flash_wing",                    // 闪电无人轰炸机 – 无人轰炸机 – 18
//				    "aspect_shock_wing",             // 震荡无常机队 – 无常 – 20
//				    "aspect_shieldbreaker_wing",     // 破盾无常机队 – 无常 – 20
//				    "aspect_attack_wing",            // 攻击无常机队 – 无常 – 20
//				    "aspect_missile_wing",           // 导弹无常机队 – 无常 – 20
//				    "talon_wing",                    // 猛爪截击机 – 截击机 – 5
					"trident_wing", // 三叉轰炸机 – 轰炸机 – 25
//				    "cobra_wing",                    // 眼镜蛇轰炸机 – 轰炸机 – 20
//				    "dagger_wing",                   // 匕首轰炸机 – 轰炸机 – 18
					"longbow_wing", // 长弓轰炸机 – 轰炸机 – 18
//				    "hoplon_wing",                   // 希波隆轰炸机 – 轰炸机 – 15
//				    "piranha_wing",                  // 食人鱼轰炸机 – 轰炸机 – 15
					"perdition_wing", // 毁灭轰炸机 – 轰炸机 – 15
//				    "terminator_wing",                // 终结者点防无人机 – 点防无人机 – 20
					"wasp_wing" // 黄蜂无人截击机 – 无人截击机 – 5
			);

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

			// 保底最喜欢的战舰
			// 准备舰船 Hull ID 列表（代码注释为中文名称）
			List<String> ships = Arrays.asList("astral", // 星体
					"odyssey" // 奥德赛
			);

			for (String hullId : ships) {
				replenishmentShipsToStorage(hullId, 1);
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

			List<String> petIds = Arrays.asList("IndEvo_captain", // 前海盗舰长
					"IndEvo_lordandsaviour", // 护国公 Foog 二世
					"IndEvo_slaghound", // 矿渣猎犬
					"IndEvo_goopuppies", // 凝胶狗
					"IndEvo_voidsquid", // 星缘乌贼
					"IndEvo_bugdog", // 漏洞犬
					"IndEvo_schafunschaf" // 虚空羊
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

		} catch (

		Throwable t) {
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

	/**
	 * 向 storage 仓库中添加指定数量的舰船，仅在尚未存在时进行。
	 *
	 * @param hullId 舰船 Hull Spec ID
	 * @param amount 添加数量
	 */
	/**
	 * 向 storage 仓库中添加指定数量的舰船，仅在尚未存在时进行。
	 * 
	 * @param hullId 舰船 Hull Spec ID
	 * @param amount 添加数量
	 */

	private void addShipsToStorage(String hullId, int amount) {
		if (market == null)
			return;
		SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
		if (storage == null) {
			Global.getLogger(getClass()).warn("addShipsToStorage: 找不到 storage 子市场");
			return;
		}
		CargoAPI cargo = storage.getCargo();
		// 初始化 mothballed ships 列表
		cargo.initMothballedShips(market.getFactionId());

		// 获取 hullId 对应的默认变体ID（通常为 hullId + "_Hull"）
		String variantId = hullId + "_Hull";
		if (Global.getSettings().getVariant(variantId) == null) {
			Global.getLogger(getClass()).warn("addShipsToStorage: 未找到默认变体 " + variantId);
			return;
		}

		// 创建并添加指定数量的新舰船实例
		for (int i = 0; i < amount; i++) {
			try {
				FleetMemberAPI newShip = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
				cargo.getMothballedShips().addFleetMember(newShip);
			} catch (Exception e) {
				Global.getLogger(getClass()).warn("addShipsToStorage: 创建或添加舰船失败 " + variantId);
				break;
			}
		}

		// 对仓库内容排序
		cargo.sort();
		// 提示玩家--关闭消息打搅
		// Global.getSector().getCampaignUI().addMessage(String.format("时光科技：已向仓库新增舰船 %s
		// ×%d", variantId, amount));
	}

	/**
	 * 向 storage 子市场补充舰船：只有当当前库存少于目标量时，才会创建并添加新舰船。
	 *
	 * @param hullId 舰船 Hull Spec ID（如 "paragon"）
	 * @param amount 期望的库存数量
	 */
	private void replenishmentShipsToStorage(String hullId, int amount) {
		if (market == null)
			return;

		SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
		if (storage == null) {
			Global.getLogger(getClass()).warn("addShipsToStorage: 找不到 storage 子市场");
			return;
		}

		CargoAPI cargo = storage.getCargo();
		// 初始化 mothballed ships 列表
		cargo.initMothballedShips(market.getFactionId());

		// 推导默认变体 ID
		String variantId = hullId + "_Hull";
		if (Global.getSettings().getVariant(variantId) == null) {
			Global.getLogger(getClass()).warn("addShipsToStorage: 未找到默认变体 " + variantId);
			return;
		}

		// 统计当前库存数量
		FleetDataAPI mothballed = cargo.getMothballedShips();
		int currentCount = 0;
		for (FleetMemberAPI member : mothballed.getMembersListCopy()) {
			if (variantId.equals(member.getVariant().getHullVariantId())) {
				currentCount++;
			}
		}

		// 库存已足够，直接返回
		if (currentCount >= amount) {
			return;
		}

		// 计算缺口并补充
		int toAdd = amount - currentCount;
		for (int i = 0; i < toAdd; i++) {
			try {
				FleetMemberAPI newShip = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
				cargo.getMothballedShips().addFleetMember(newShip);
			} catch (Exception e) {
				Global.getLogger(getClass()).warn("addShipsToStorage: 创建或添加舰船失败 " + variantId);
				break;
			}
		}

		// 对仓库内容排序
		cargo.sort();
		// 如需提示玩家可取消下行注释
		// Global.getSector().getCampaignUI().addMessage(
		// String.format("时光科技：已向仓库补充舰船 %s ×%d", variantId, toAdd)
		// );
	}

}
