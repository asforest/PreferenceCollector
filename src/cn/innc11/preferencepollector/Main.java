package cn.innc11.preferencepollector;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.LoginChainData;
import cn.nukkit.utils.TextFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class Main extends PluginBase implements Listener
{
	LinkedHashMap<String, PlayerPlatformData> data = new LinkedHashMap<>();

	private boolean modified = false;
	private boolean saving = false;
	private PluginTask<Main> saveTask;

	@Override
	public void onLoad()
	{
		saveTask = new PluginTask<Main>(this)
		{
			@Override
			public void onRun(int currentTicks)
			{
				while(modified)
				{
					modified = false;
					Save();
				}

				saving = false;
			}
		};
	}

	@Override
	public void onEnable()
	{
		saveDefaultConfig();
		reload();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(sender.isOp() && sender instanceof Player)
		{
			FormWindowSimple window = new FormWindowSimple(TextFormat.colorize("&l&cUser LCD Overview"), "");

			StringBuffer sb = new StringBuffer();

			sb.append(String.format("&d&l              %s Players&r\n", data.size()));
			sb.append("\n");

			data.forEach((player, ppd)->
			{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				sb.append(String.format("&l&b%s:&r     &7%s&r\n", player, sdf.format(ppd.lastQuit)));

				for(String dev : ppd.deviceModels)
				{
					if(dev.length()>30)
					{
						dev = dev.substring(0, 30);
						dev += "&7...&r";
					}
					sb.append("      &6- "+dev+"\n");
				}

				for(String dev : ppd.languageCodes)
				{
					sb.append("      &9- "+dev+"\n");
				}

				if(ppd.Android!=0) sb.append(String.format("&a      - Android: &e%d&r\n", ppd.Android));
				if(ppd.iOS!=0) sb.append(String.format("&a      - iOS: &e%d&r\n", ppd.iOS));
				if(ppd.Mac!=0) sb.append(String.format("&a      - Mac: &e%d&r\n", ppd.Mac));
				if(ppd.Fire!=0) sb.append(String.format("&a      - Fire: &e%d&r\n", ppd.Fire));
				if(ppd.GearVR!=0) sb.append(String.format("&a      - GearVR: &e%d&r\n", ppd.GearVR));
				if(ppd.HoloLens!=0) sb.append(String.format("&a      - HoloLens: &e%d&r\n", ppd.HoloLens));
				if(ppd.Windows10!=0) sb.append(String.format("&a      - Windows10: &e%d&r\n", ppd.Windows10));
				if(ppd.Windows!=0) sb.append(String.format("&a      - Windows: &e%d&r\n", ppd.Windows));
				if(ppd.Dedicated!=0) sb.append(String.format("&a      - Dedicated: &e%d&r\n", ppd.Dedicated));
				if(ppd.tvOS!=0) sb.append(String.format("&a      - tvOS: &e%d&r\n", ppd.tvOS));
				if(ppd.PlayStation!=0) sb.append(String.format("&a      - PlayStation: &e%d&r\n", ppd.PlayStation));
				if(ppd.NX!=0) sb.append(String.format("&a      - NX: &e%d&r\n", ppd.NX));
				if(ppd.Xbox!=0) sb.append(String.format("&a      - Xbox: &e%d&r\n", ppd.Xbox));
				if(ppd.Unknown!=0) sb.append(String.format("&a      - Unknown: &e%d&r\n", ppd.Unknown));

				sb.append("\n");

			});


			window.setContent(TextFormat.colorize(sb.toString()));

			((Player) sender).showFormWindow(window);
		}

		return true;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		String playerName = e.getPlayer().getName();

		PlayerPlatformData ppd = null;

		if(!data.containsKey(playerName))
		{
			ppd = new PlayerPlatformData();
			data.put(playerName, ppd);
		}else{
			ppd = data.get(playerName);
		}

		LinkedHashMap<String, PlayerPlatformData> temp = new LinkedHashMap<>();
		data.remove(playerName);
		temp.putAll(data);
		data.clear();
		data.put(playerName, ppd);
		data.putAll(temp);
		ppd.update(e.getPlayer().getLoginChainData());
		save();

		//showPlayerLCD(e.getPlayer(), "&e");
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		String playerName = e.getPlayer().getName();

		PlayerPlatformData ppd = null;

		if(!data.containsKey(playerName))
		{
			ppd = new PlayerPlatformData();
			data.put(playerName, ppd);
		}else{
			ppd = data.get(playerName);
		}

		LinkedHashMap<String, PlayerPlatformData> temp = new LinkedHashMap<>();
		data.remove(playerName);
		temp.putAll(data);
		data.clear();
		data.put(playerName, ppd);
		data.putAll(temp);
		ppd.lastQuit = System.currentTimeMillis();
		save();

		showPlayerLCD(e.getPlayer(), "");
	}

	public void showPlayerLCD(Player player, String colorCode)
	{
		LoginChainData lcd = player.getLoginChainData();
		String playerName = player.getName();
		String os = PlayerPlatformData.parse(lcd.getDeviceOS());
		String device = lcd.getDeviceModel();
		String lang = lcd.getLanguageCode();

		getLogger().info(TextFormat.colorize(String.format("__%s&r: __%s&r | __%s&r | __%s&r", playerName, os, device, lang).replace("__", colorCode)));
	}

	void save()
	{
		if(saveTask!=null)
		{
			modified = true;

			if(!saving)
			{
				saving = true;
				getServer().getScheduler().scheduleTask(this, saveTask, true);
			}
		}
	}

	void reload()
	{
		data.clear();
		Config config = getConfig();
		config.reload();

		int i = 0;
		for(String key : config.getKeys(false))
		{
			String prefix = key;

			String playerName = config.getString(prefix+".player");

			PlayerPlatformData ppd = new PlayerPlatformData();
			ppd.lastQuit = config.getLong(prefix+".lastQuit", 0);
			ppd.Android = config.getInt(prefix+".platforms.Android", 0);
			ppd.iOS = config.getInt(prefix+".platforms.iOS", 0);
			ppd.Mac = config.getInt(prefix+".platforms.Mac", 0);
			ppd.Fire = config.getInt(prefix+".platforms.Fire", 0);
			ppd.GearVR = config.getInt(prefix+".platforms.GearVR", 0);
			ppd.HoloLens = config.getInt(prefix+".platforms.HoloLens", 0);
			ppd.Windows10 = config.getInt(prefix+".platforms.Windows10", 0);
			ppd.Windows = config.getInt(prefix+".platforms.Windows", 0);
			ppd.Dedicated = config.getInt(prefix+".platforms.Dedicated", 0);
			ppd.tvOS = config.getInt(prefix+".platforms.tvOS", 0);
			ppd.PlayStation = config.getInt(prefix+".platforms.PlayStation", 0);
			ppd.NX = config.getInt(prefix+".platforms.NX", 0);
			ppd.Xbox = config.getInt(prefix+".platforms.Xbox", 0);
			ppd.Unknown = config.getInt(prefix+".platforms.Unknown", 0);

			ppd.deviceModels = (ArrayList<String>) config.getStringList(prefix+".deviceModels");
			ppd.languageCodes = (ArrayList<String>) config.getStringList(prefix+".languageCodes");

			data.put(playerName, ppd);
			i++;
		}

		getLogger().info(TextFormat.colorize(String.format("found &b%d&r player data", i)));
	}

	void Save()
	{
		Config config = getConfig();
		config.getRootSection().clear();

		data.forEach((player, ppd)->
		{
			String section = UUID.randomUUID().toString();
			config.set(section+".player", player);
			if(ppd.lastQuit!=0) config.set(section+".lastQuit", ppd.lastQuit);
			if(ppd.Android!=0) config.set(section+".platforms.Android", ppd.Android);
			if(ppd.iOS!=0) config.set(section+".platforms.iOS", ppd.iOS);
			if(ppd.Mac!=0) config.set(section+".platforms.Mac", ppd.Mac);
			if(ppd.Fire!=0) config.set(section+".platforms.Fire", ppd.Fire);
			if(ppd.GearVR!=0) config.set(section+".platforms.GearVR", ppd.GearVR);
			if(ppd.HoloLens!=0) config.set(section+".platforms.HoloLens", ppd.HoloLens);
			if(ppd.Windows10!=0) config.set(section+".platforms.Windows10", ppd.Windows10);
			if(ppd.Windows!=0) config.set(section+".platforms.Windows", ppd.Windows);
			if(ppd.Dedicated!=0) config.set(section+".platforms.Dedicated", ppd.Dedicated);
			if(ppd.tvOS!=0) config.set(section+".platforms.tvOS", ppd.tvOS);
			if(ppd.PlayStation!=0) config.set(section+".platforms.PlayStation", ppd.PlayStation);
			if(ppd.NX!=0) config.set(section+".platforms.NX", ppd.NX);
			if(ppd.Xbox!=0) config.set(section+".platforms.Xbox", ppd.Xbox);
			if(ppd.Unknown!=0) config.set(section+".platforms.Unknown", ppd.Unknown);


			if(!ppd.deviceModels.isEmpty()) config.set(section+".deviceModels", ppd.deviceModels);
			if(!ppd.languageCodes.isEmpty()) config.set(section+".languageCodes", ppd.languageCodes);
		});

		config.save();
	}

	static class PlayerPlatformData
	{
		public int Android = 0;
		public int iOS = 0;
		public int Mac = 0;
		public int Fire = 0;
		public int GearVR = 0;
		public int HoloLens = 0;
		public int Windows10 = 0;
		public int Windows = 0;
		public int Dedicated = 0;
		public int tvOS = 0;
		public int PlayStation = 0;
		public int NX = 0;
		public int Xbox = 0;
		public int Unknown = 0;

		public long lastQuit;

		public ArrayList<String> deviceModels = new ArrayList<>();
		public ArrayList<String> languageCodes = new ArrayList<>();

		public void update(LoginChainData data)
		{
			switch (data.getDeviceOS())
			{
				case 1: Android++; break;
				case 2: iOS++; break;
				case 3: Mac++; break;
				case 4: Fire++; break;
				case 5: GearVR++; break;
				case 6: HoloLens++; break;
				case 7: Windows10++; break;
				case 8: Windows++; break;
				case 9: Dedicated++; break;
				case 10: tvOS++; break;
				case 11: PlayStation++; break;
				case 12: NX++; break;
				case 13: Xbox++; break;
				default: Unknown++; break;
			}

			String deviceModel = data.getDeviceModel();
			String languageCode = data.getLanguageCode();

			if(deviceModels.contains(deviceModel))
			{
				deviceModels.remove(deviceModel);
			}
			deviceModels.add(0, deviceModel);

			if(languageCodes.contains(languageCode))
			{
				languageCodes.remove(languageCode);
			}
			languageCodes.add(0, languageCode);
		}

		public static String parse(int playFormId)
		{
			switch (playFormId)
			{
				case 1: return "Android";
				case 2: return "iOS";
				case 3: return "Mac";
				case 4: return "Fire";
				case 5: return "GearVR";
				case 6: return "HoloLens";
				case 7: return "Windows10";
				case 8: return "Windows";
				case 9: return "Dedicated";
				case 10: return "tvOS";
				case 11: return "PlayStation";
				case 12: return "NX";
				case 13: return "Xbox";
				default: return "Unknown";
			}
		}

	}
}
