import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.*;
import java.sql.*;

public class iConomy extends Plugin {
	protected static final Logger log = Logger.getLogger("Minecraft");
	private Listener l = new Listener(this);

	static boolean debugging = true;

	// Directory
	private String directory = "iConomy/";
	private String lDirectory = "logs/";

	// Property Files
	private iProperty props;
	private iProperty buying;
	private iProperty selling;
	private iProperty auctions;
	private iProperty auctioner;
	private iProperty prizes;

	// Hashmaps for lottery
	private Map<String, String> hashPrizes;

	// Hashmaps for items
	BidiMap items = new TreeBidiMap();

	// Data Control
	public iData data;

	// Ranking
	private LinkedList<String> rankedList;

	// Money Settings
	private int moneyGive = 0;
	private int moneyGiveInterval = 0;
	private int moneyTake = 0;
	private int moneyTakeInterval = 0;
	public int startingBalance;
	private Timer mTime1;
	private Timer mTime2;
	public String moneyName;

	// Permissions
	public String canPay;
	public String canCredit;
	public String canDebit;
	public String canReset;
	public String canRank;
	public String canTop;
	public String canView;
	public String canSell;
	public String canBuy;
	public String canAuction;
	public String canBid;
	public String canEnd;
	public String canLottery;

	// Shop Details
	public boolean globalAuction;
	public boolean globalShop;
	public boolean physicalShop;
	public boolean globalLottery;

	// Auction settings
	public Timer auctionTimer = new Timer();
	boolean auctionTimerRunning;
	public String auctionName;
	public String auctionStarter;
	public int auctionStartingBid = 0;
	public int auctionInterval;
	public int auctionItem = 0;
	public int auctionAmount = 0;
	public int auctionMin = 0;
	public int auctionReserve = 0;
	public String auctionCurName;
	public int auctionCurAmount = 0;
	public int auctionCurBid = 0;
	public int auctionCurBidCount = 0;
	public int auctionCurSecretBid = 0;
	public boolean auctionReserveMet = false;

	// Buying Template
	public String buyInvalidAmount;
	public String buyNotEnough;
	public String buyReject;
	public String buyGive;

	// Selling Template
	public String sellInvalidAmount;
	public String sellReject;
	public String sellGiveAmount;
	public String sellGive;
	public String sellNone;

	// Lottery Template
	public String lotteryNotEnough;
	public String lotteryLoser;
	public String lotteryNotAvailable;
	public String lotteryWinner;
	public String lotteryLimit;
	public String lotteryTag = Colors.White + "["+Colors.Green+"Lottery"+Colors.White+"]";
	public String lotteryCost;

	// Logging
	public boolean logPay;
	public boolean logBuy;
	public boolean logSell;
	private boolean logAuction;

	// Tickets
	public int ticketCost;

	// Database
	private boolean mysql;
	private String driver;
	private String user;
	private String pass;
	private String db;

	// Versioning
	private String version = "0.9.3.7";
	private String sversion = "0.6";
	private String aversion = "0.5";
	private String lversion = "0.3";

	public iConomy() {
		this.props = null;
		this.data = null;
		this.mTime1 = null;
		this.mTime2 = null;
		this.auctionTimer = null;
	}

	public void enable() {
		if (load()) {
			log.info("[iConomy v" + this.version + "] Plugin Enabled.");

			if(debugging)
				log.info("[iConomy Debugging] Enabled.");

			String latest = getVersion();
			if(latest != null) {
				if(!latest.equals(version)) {
					log.info("[iConomy Update] A new version of iConomy has been released! v"+latest+"!");
				}
			}
		} else {
			log.info("[iConomy v" + this.version + "] Plugin failed to load.");
		}

		etc.getInstance().addCommand("/money", "? - For more information");

		if(this.globalShop)
			etc.getInstance().addCommand("/shop", "? - For more information");

		if(this.globalAuction)
			etc.getInstance().addCommand("/auction", "? - For more information");

		if(this.globalLottery)
			etc.getInstance().addCommand("/lottery", "- Test your luck on the lottery!");
	}

	public void initialize() {
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, l, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.LOW);
	}

	public void disable() {
		etc.getInstance().removeCommand("/money");

		if(this.globalShop)
			etc.getInstance().removeCommand("/shop");

		if(this.globalAuction)
			etc.getInstance().removeCommand("/auction");

		if(this.globalLottery)
			etc.getInstance().removeCommand("/lottery");

		if (this.mTime1 != null)
			this.mTime1.cancel();

		if (this.mTime2 != null) 
			this.mTime2.cancel();

		if (this.auctionTimer != null)
			this.auctionTimer.cancel();

		log.info("[iConomy v" + this.version + "] Plugin Disabled.");
	}

	private void halp(Player player, String type) {
		if (type.equals("money")) {
			player.sendMessage(Colors.Rose + "iConomy [Money] v" + this.version + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<p> = player, <a> = amount");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/money - Shows your balance");

			if (this.can(player, "view")) {
				player.sendMessage(Colors.Rose + "/money <p> - Shows player balance");
			}

			if (this.can(player, "pay")) {
				player.sendMessage(Colors.Rose + "/money pay <p> <a> - Pay a player money");
			}

			if (this.can(player, "credit")) {
				player.sendMessage(Colors.Rose + "/money credit <p> <a> - Give a player money");
			}

			if (this.can(player, "debit")) {
				player.sendMessage(Colors.Rose + "/money debit <p> <a> - Take a players money");
			}

			if (this.can(player, "rank")) {
				player.sendMessage(Colors.Rose + "/money rank <p> - Show your rank or another players");
			}

			if (this.can(player, "reset")) {
				player.sendMessage(Colors.Rose + "/money reset <p> - Reset a players account balance.");
			}

			if (this.can(player, "top")) {
				player.sendMessage(Colors.Rose + "/money top - Shows top 5");
				player.sendMessage(Colors.Rose + "/money top <a> - Shows top <a> richest players");
			}

			player.sendMessage(Colors.Rose + "/money help|? - Displays this.");
		} else if (type.equals("shop")) {
			player.sendMessage(Colors.Rose + "iConomy [Shop] v" + this.sversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<i> = item, <a> = amount, [] = optional");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/shop <i> - Shows amount per item for sell/buy");
			player.sendMessage(Colors.Rose + "/shop <i> <a> - Shows amount per <a> for sell/buy");
			player.sendMessage(Colors.Rose + "/shop list buy [page #] - Shows buying list");
			player.sendMessage(Colors.Rose + "/shop list sell [page #] - Shows selling list");

			if (this.can(player, "buy")) {
				player.sendMessage(Colors.Rose + "/shop buy <i> - Purchase 1 item");
				player.sendMessage(Colors.Rose + "/shop buy <i> <a> - Purchase multiple items");
			}

			if (this.can(player, "sell")) {
				player.sendMessage(Colors.Rose + "/shop sell <i> - Sell 1 item");
				player.sendMessage(Colors.Rose + "/shop sell <i> <a> - Sell multiple items");
			}

			player.sendMessage(Colors.Rose + "/shop help|? - Displays this.");
		} else if (type.equals("auction")) {
			player.sendMessage(Colors.Rose + "iConomy [Auction] v" + this.aversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "<i> Item, <a> Amount, <s> Secret bid");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/auction - Shows current auction details or auction running information.");

			if(this.can(player, "auction"))
				player.sendMessage(Colors.Rose + "/auction start <time-seconds> <item> <amount> <start-bid>");
				player.sendMessage(Colors.Rose + "    Optional after <start-bid>: [min-bid] [max-bid]");
				player.sendMessage(Colors.Rose + "    Desc: Starts the auction with name for concurrent bids");

			if(this.can(player, "bid"))
				player.sendMessage(Colors.Rose + "/auction bid <a> - bid on the current auction");
				player.sendMessage(Colors.Rose + "/auction bid <a> <s> - bid with a secret amount");

			player.sendMessage(Colors.Rose + "/auction end - ends the current auction");
			player.sendMessage(Colors.Rose + "/auction ? - help documentation");
		} else if (type.equals("lottery")) {
			player.sendMessage(Colors.Rose + "iConomy [Lottery] v" + this.lversion + " - by Nijikokun");
			player.sendMessage(Colors.Rose + "---------------");
			player.sendMessage(Colors.Rose + "/lottery - Try your luck at winning the lottery!");
			player.sendMessage(Colors.Rose + "/lottery ? - help documentation");
		}
	}

	/**
	 * Update user state in plugin
	 */
	public void updateState(Player player, boolean write) {
		String str = player.getName();
		this.rankedList.remove(str);
		insertIntoRankedList(str);
	}

	private boolean load() {
		// Create directory if it doesn't exist.
		(new File(directory)).mkdir();
		(new File(directory + lDirectory)).mkdir();

		// File Data
		this.props = new iProperty(directory + "settings.properties");

		// Switches
		this.mysql = this.props.getBoolean("use-mysql", false);
		this.globalShop = this.props.getBoolean("use-shop", true);
		this.globalAuction = this.props.getBoolean("use-auction", true);
		this.globalLottery = this.props.getBoolean("use-lottery", true);

		// Debugging
		debugging = this.props.getBoolean("debugging", false);

		// Money Starting Balance
		this.startingBalance = this.props.getInt("starting-balance", 0);

		// Lottery Ticket Cost
		this.ticketCost = this.props.getInt("ticket-cost", 150);

		// Ticker Amounts
		this.moneyGive = this.props.getInt("money-give", 0);
		this.moneyTake = this.props.getInt("money-take", 0);

		// Ticker Intervals
		this.moneyGiveInterval = (1000 * this.props.getInt("money-give-interval", 0));
		this.moneyTakeInterval = (1000 * this.props.getInt("money-take-interval", 0));

		// Money Name
		this.moneyName = (" " + this.props.getString("money-name", "coin"));

		// Groups per Command
		this.canPay = this.props.getString("can-pay", "*");
		this.canDebit = this.props.getString("can-debit", "admins,");
		this.canCredit = this.props.getString("can-credit", "admins,");
		this.canReset = this.props.getString("can-reset", "admins,");
		this.canRank = this.props.getString("can-rank", "*");
		this.canTop = this.props.getString("can-top", "*");
		this.canView = this.props.getString("can-view-player-balance", "*");
		this.canBuy = this.props.getString("can-buy", "*");
		this.canSell = this.props.getString("can-sell", "*");
		this.canAuction = this.props.getString("can-auction", "*");
		this.canBid = this.props.getString("can-bid", "*");
		this.canEnd = this.props.getString("can-end", "admins,");
		this.canLottery = this.props.getString("can-lottery", "*");

		// Buying / Selling logging
		this.logPay = this.props.getBoolean("log-pay", false);
		this.logBuy = this.props.getBoolean("log-buy", false);
		this.logSell = this.props.getBoolean("log-sell", false);
		this.logAuction = this.props.getBoolean("log-auction", false);

		// Buy / Sell messages
		this.sellGive = this.props.getString("sell-success", "Your account has been credited with %s!");
		this.sellGiveAmount = this.props.getString("sell-success-sold", "Sold %d out of %d!");
		this.sellReject = this.props.getString("sell-rejected", "Sorry, that item is currently unavailable!");
		this.sellNone = this.props.getString("sell-none", "Whoops, you seem to not have any of that item!");
		this.buyGive = this.props.getString("buy-success", "Your purchase cost you %s! Here you go :)!");
		this.buyReject = this.props.getString("buy-rejected", "Sorry, that item is currently unavailable!");
		this.buyNotEnough = this.props.getString("buy-not-enough", "Sorry, you currently don't have enough to buy that!");
		this.buyInvalidAmount = this.props.getString("buy-invalid-amount", "Sorry, you must buy these in increments of %d!");
		this.sellInvalidAmount = this.props.getString("sell-invalid-amount", "Sorry, you must sell these in increments of %d!");

		// Lottery messages
		this.lotteryNotEnough = this.props.getString("lottery-not-enough", "Sorry, you do not have enough to buy a ticket!");
		this.lotteryWinner = this.props.getString("lottery-winner", "Congratulations %s won %d %s in the lottery!");
		this.lotteryLoser = this.props.getString("lottery-loser", "The lady looks at you and shakes her head. Try Again!.");
		this.lotteryNotAvailable = this.props.getString("lottery-not-available", "Lottery seems to be unavailable this time. Try again!");
		this.lotteryCost = this.props.getString("lottery-cost", "The lady snatches %s from your hand and gives you a ticket.");


		// Shop Data
		this.buying = new iProperty(directory + "buying.properties");
		this.selling = new iProperty(directory + "selling.properties");
		this.auctions = new iProperty(directory + "auction.properties");
		this.auctioner = new iProperty(directory + "auctioner.properties");
		this.prizes = new iProperty(directory + "prizes.properties");

		try {
			this.hashPrizes = this.prizes.returnMap();
		} catch (Exception ex) {
			log.severe("[iConomy Lottery] Error: " + ex);
		}
		
		// MySQL
		this.driver = this.props.getString("driver", "com.mysql.jdbc.Driver");
		this.user = this.props.getString("user", "root");
		this.pass = this.props.getString("pass", "root");
		this.db = this.props.getString("db", "jdbc:mysql://localhost:3306/minecraft");
		
		// Data
		this.data = new iData(this.mysql, this.startingBalance, this.driver, this.user, this.pass, this.db);

		// Buying
		items.put("1", "stone");
		items.put("2", "grass");
		items.put("3", "dirt");
		items.put("4", "cobblestone");
		items.put("5", "wood");
		items.put("6", "sapling");
		items.put("7", "bedrock");
		items.put("8", "water");
		items.put("9", "still-water");
		items.put("10", "lava");
		items.put("11", "still-lava");
		items.put("12", "sand");
		items.put("13", "gravel");
		items.put("14", "gold-ore");
		items.put("15", "iron-ore");
		items.put("16", "coal-ore");
		items.put("17", "log");
		items.put("18", "leaves");
		items.put("19", "sponge");
		items.put("20", "glass");
		items.put("35", "gray-cloth");
		items.put("37", "yellow-flower");
		items.put("38", "red-rose");
		items.put("39", "brown-mushroom");
		items.put("40", "red-mushroom");
		items.put("41", "gold-block");
		items.put("42", "iron-block");
		items.put("43", "double-step");
		items.put("44", "step");
		items.put("45", "brick");
		items.put("46", "tnt");
		items.put("47", "bookcase");
		items.put("48", "mossy-cobblestone");
		items.put("49", "obsidian");
		items.put("50", "torch");
		items.put("51", "fire");
		items.put("52", "mob-spawner");
		items.put("53", "wooden-stairs");
		items.put("54", "chest");
		items.put("55", "redstone-wire");
		items.put("56", "diamond-ore");
		items.put("57", "diamond-block");
		items.put("58", "workbench");
		items.put("59", "crops");
		items.put("60", "soil");
		items.put("61", "furnace");
		items.put("62", "burning-furnace");
		items.put("63", "sign-post");
		items.put("64", "wooden-door");
		items.put("65", "ladder");
		items.put("66", "mine-cart-tracks");
		items.put("67", "cobblestone-stairs");
		items.put("68", "wall-sign");
		items.put("69", "lever");
		items.put("70", "stone-pressure-plate");
		items.put("71", "iron-door");
		items.put("72", "wooden-pressure-plate");
		items.put("73", "redstone-ore");
		items.put("74", "glowing-redstone-ore");
		items.put("75", "redstone-torch-off");
		items.put("76", "redstone-torch-on");
		items.put("77", "stone-button");
		items.put("78", "snow");
		items.put("79", "ice");
		items.put("80", "snow-block");
		items.put("81", "cactus");
		items.put("82", "clay");
		items.put("83", "reed");
		items.put("84", "jukebox");
		items.put("85", "fence");
		items.put("86", "pumpkin");
		items.put("87", "red-mossy-cobblestone");
		items.put("88", "mud");
		items.put("89", "brittle-gold");
		items.put("90", "portal");
		items.put("91", "jack-o-lantern");
		items.put("256", "iron-spade");
		items.put("257", "iron-pickaxe");
		items.put("258", "iron-axe");
		items.put("259", "steel-and-flint");
		items.put("260", "apple");
		items.put("261", "bow");
		items.put("262", "arrow");
		items.put("263", "coal");
		items.put("264", "diamond");
		items.put("265", "iron-ingot");
		items.put("266", "gold-ingot");
		items.put("267", "iron-sword");
		items.put("268", "wooden-sword");
		items.put("269", "wooden-spade");
		items.put("270", "wooden-pickaxe");
		items.put("271", "wooden-axe");
		items.put("272", "stone-sword");
		items.put("273", "stone-spade");
		items.put("274", "stone-pickaxe");
		items.put("275", "stone-axe");
		items.put("276", "diamond-sword");
		items.put("277", "diamond-spade");
		items.put("278", "diamond-pickaxe");
		items.put("279", "diamond-axe");
		items.put("280", "stick");
		items.put("281", "bowl");
		items.put("282", "mushroom-soup");
		items.put("283", "gold-sword");
		items.put("284", "gold-spade");
		items.put("285", "gold-pickaxe");
		items.put("286", "gold-axe");
		items.put("287", "string");
		items.put("288", "feather");
		items.put("289", "gunpowder");
		items.put("290", "wooden-hoe");
		items.put("291", "stone-hoe");
		items.put("292", "iron-hoe");
		items.put("293", "diamond-hoe");
		items.put("294", "gold-hoe");
		items.put("295", "seeds");
		items.put("296", "wheat");
		items.put("297", "bread");
		items.put("298", "leather-helmet");
		items.put("299", "leather-chestplate");
		items.put("300", "leather-pants");
		items.put("301", "leather-boots");
		items.put("302", "chainmail-helmet");
		items.put("303", "chainmail-chestplate");
		items.put("304", "chainmail-pants");
		items.put("305", "chainmail-boots");
		items.put("306", "iron-helmet");
		items.put("307", "iron-chestplate");
		items.put("308", "iron-pants");
		items.put("309", "iron-boots");
		items.put("310", "diamond-helmet");
		items.put("311", "diamond-chestplate");
		items.put("312", "diamond-pants");
		items.put("313", "diamond-boots");
		items.put("314", "gold-helmet");
		items.put("315", "gold-chestplate");
		items.put("316", "gold-pants");
		items.put("317", "gold-boots");
		items.put("318", "flint");
		items.put("319", "pork");
		items.put("320", "grilled-pork");
		items.put("321", "painting");
		items.put("322", "golden-apple");
		items.put("323", "sign");
		items.put("324", "wooden-door");
		items.put("325", "bucket");
		items.put("326", "water-bucket");
		items.put("327", "lava-bucket");
		items.put("328", "mine-cart");
		items.put("329", "saddle");
		items.put("330", "iron-door");
		items.put("331", "redstone");
		items.put("332", "snowball");
		items.put("333", "boat");
		items.put("334", "leather");
		items.put("335", "milk-bucket");
		items.put("336", "clay-brick");
		items.put("337", "clay-balls");
		items.put("338", "reed");
		items.put("339", "paper");
		items.put("340", "book");
		items.put("341", "slime-ball");
		items.put("342", "storage-mine-cart");
		items.put("343", "powered-mine-cart");
		items.put("344", "egg");
		items.put("345", "compass");
		items.put("346", "fishing-rod");
		items.put("347", "watch");
		items.put("348", "brittle-gold-dust");
		items.put("349", "raw-fish");
		items.put("350", "cooked-fish");
		items.put("2256", "gold-record");
		items.put("2257", "green-record");

		// Setup Listing
		this.rankedList = new LinkedList();

		// Setup Timers
		this.mTime1 = new Timer();
		this.mTime2 = new Timer();

		// Start the ticking for giving
		if (this.moneyGiveInterval > 0) {
			this.mTime1.schedule(new TimerTask() {

				public void run() {
					etc.getInstance();
					List<Player> localList = etc.getServer().getPlayerList();
					for (Player localPlayer : localList) {
						iConomy.this.deposit(null, localPlayer.getName(), iConomy.this.moneyGive, false);
					}
				}
			}, 0L, this.moneyGiveInterval);
		}

		// Start the ticking for taking
		if (this.moneyTakeInterval > 0) {
			this.mTime2.schedule(new TimerTask() {

				public void run() {
					etc.getInstance();
					List<Player> localList = etc.getServer().getPlayerList();
					for (Player localPlayer : localList) {
						iConomy.this.debit(null, localPlayer.getName(), iConomy.this.moneyTake, false);
					}
				}
			}, 0L, this.moneyTakeInterval);
		}

		return true;
	}

	private static String getVersion()
	{
		String content = null;

		// many of these calls can throw exceptions, so i've just
		// wrapped them all in one try/catch statement.
		try
		{
			URL url = new URL("http://mc.nexua.org/plugins/iConomy/?latest");
			URLConnection urlConnection = url.openConnection();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			String line;

			// read from the urlconnection via the bufferedreader
			while ((line = bufferedReader.readLine()) != null){
				content = line;
			}
			bufferedReader.close();
		} catch(Exception e) {
			log.severe("[iConomy Version Check] " + e);
		}

		return content;
	}


	public static String logDate() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		return format.format(now);
	}

	public void shopLog(String type, String data) {
		String date = logDate();
		if (type.equalsIgnoreCase("pay") && this.logPay) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "pay.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Pay Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("buy") && this.logBuy) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "buy.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Buy Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("sell") && this.logSell) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "sell.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Sell Logging] " + es.getMessage());
			}
		} else if (type.equalsIgnoreCase("auction") && this.logAuction) {
			try {
				FileWriter fstream = new FileWriter(directory + lDirectory + "auction.log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(date + "|" + data);
				out.newLine();
				out.close();
			} catch (Exception es) {
				log.severe("[iConomy Auction Loggin] " + es.getMessage());
			}
		}
	}

	public static String cInt(int i) {
		return Integer.toString(i);
	}

	public int itemNeedsAmount(String type, String itemId) {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int cost = 0;

			try {
				conn = this.data.MySQL();
				if(type.equals("buy")) {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iBuy WHERE id = ? LIMIT 1");
				} else {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iSell WHERE id = ? LIMIT 1");
				}
				ps.setInt(1, Integer.parseInt(itemId));
				rs = ps.executeQuery();

				if (rs.next()) {
					if (rs.getInt("perbundle") > 1) {
						return rs.getInt("perbundle");
					} else {
						if (rs.getInt("cost") == 0) {
							return 0;
						} else {
							return 1;
						}
					}
				} else {
					return 0;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the item [" + itemId + "] " + type + " price from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return cost;
		} else {
			String info = "";

			if (type.equals("buy")) {
				info = this.buying.getString((String) this.items.get(itemId));
			} else if (type.equals("sell")) {
				info = this.selling.getString((String) this.items.get(itemId));
			}

			if (info.equals("")) {
				return 0;
			}

			if (info.contains(",")) {
				String[] item = info.split(",");
				return Integer.parseInt(item[0]);
			} else {
				return 1;
			}
		}
	}

	public boolean itemCan(String type, String itemId, int amount) {
		int itemAmount = this.itemNeedsAmount(type, itemId);

		// Maximum
		if (amount > 6400) {
			return false;
		}

		if (itemAmount == 0) {
			return false;
		} else if (itemAmount > 1) {
			if ((amount * itemAmount) < 6400) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public int itemCost(String type, String itemId, int amount, boolean total) {
		String info = "";
		int perBundle = 0;
		int cost = 0;

		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				if(type.equals("buy")) {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iBuy WHERE id = ? LIMIT 1");
				} else {
					ps = conn.prepareStatement("SELECT cost,perbundle FROM iSell WHERE id = ? LIMIT 1");
				}
				ps.setInt(1, Integer.parseInt(itemId));
				rs = ps.executeQuery();

				if (rs.next()) {
					if (!this.itemCan(type, itemId, amount)) {
						return 0;
					}

					// Settings
					cost = rs.getInt("cost");
					perBundle = rs.getInt("perbundle");

					if (total) {
						return cost*amount;
					} else {
						return cost;
					}
				} else {
					return 0;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the item [" + itemId + "] " + type + " price from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return cost;
		} else {
			if (type.equals("buy")) {
				info = this.buying.getString((String) this.items.get(itemId));
			} else if (type.equals("sell")) {
				info = this.selling.getString((String) this.items.get(itemId));
			}

			if (info.equals("")) {
				return 0;
			}

			if (info.contains(",")) {
				String[] item = info.split(",");
				perBundle = Integer.parseInt(item[0]);
				cost = Integer.parseInt(item[1]);

				// Check if we can
				if (!this.itemCan(type, itemId, amount)) {
					return 0;
				}

				if (total) {
					return cost*amount;
				} else {
					return cost;
				}
			} else {
				cost = Integer.parseInt(info);

				if (total) {
					return cost*amount;
				} else {
					return cost;
				}
			}
		}
	}

	public void doSellList(Player player, int page) {
		List available = new ArrayList();
		Map aList;

		try {
			aList = this.selling.returnMap();
		} catch (Exception ex) {
			log.info("[iConomy] Listing failed for buying list"); return;
		}

		for (Object key: aList.keySet()) {
			String cost = (String) aList.get(key);
			String name = (String) key;
			Boolean bundle = false;
			int perBundle = 0;

			if(cost.equals("0")) {
				continue;
			} else if(cost.contains(",")) {
				String[] item = cost.split(",");
				perBundle = Integer.parseInt(item[0]);
				cost = item[1];
			}

			if(bundle)
				available.add(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
			else {
				available.add(Colors.Green + name + Colors.LightGray + " is worth " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
			}
		}

		player.sendMessage(Colors.White + "Shop Selling (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(available.size() / 10.0D) + Colors.White + "):");

		try {
			int amount = page;

			if (amount > 0){
				amount = (amount - 1) * 10;
			} else {
				amount = 0;
			}

			for (int i = amount; i < amount + 10; i++)
				if (available.size() > i)
					player.sendMessage((String)available.get(i));
		}
		catch (NumberFormatException ex)
		{
			player.sendMessage("§cNot a valid page number.");
		}
	}

	public void doBuyList(Player player, int page) {
		List available = new ArrayList();
		Map aList;

		try {
			aList = this.buying.returnMap();
		} catch (Exception ex) {
			log.info("[iConomy] Listing failed for buying list"); return;
		}

		for (Object key: aList.keySet()) {
			String cost = (String) aList.get(key);
			String name = (String) key;
			Boolean bundle = false;
			int perBundle = 0;

			if(cost.equals("0")) {
				continue;
			} else if(cost.contains(",")) {
				String[] item = cost.split(",");
				perBundle = Integer.parseInt(item[0]);
				cost = item[1];
			}

			if(bundle)
				available.add(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " at " + Colors.Green + perBundle + Colors.LightGray + " per bundle.");
			else {
				available.add(Colors.Green + name + Colors.LightGray + " costs " + Colors.Green + cost + this.moneyName + Colors.LightGray + " per item.");
			}
		}

		player.sendMessage(Colors.White + "Shop Buying (Page " + Colors.Green  + (page) + Colors.White + " of " + Colors.Green  + (int)Math.ceil(available.size() / 10.0D) + Colors.White + "):");

		try {
			int amount = page;

			if (amount > 0){
				amount = (amount - 1) * 10;
			} else {
				amount = 0;
			}

			for (int i = amount; i < amount + 10; i++)
				if (available.size() > i)
					player.sendMessage((String)available.get(i));
		}
		catch (NumberFormatException ex)
		{
			player.sendMessage("§cNot a valid page number.");
		}
	}

	public boolean doPurchase(Player player, int itemId, int amount) {
		int itemAmount = this.itemCost("buy", cInt(itemId), amount, false);
		int needsAmount = this.itemNeedsAmount("buy", cInt(itemId));

		if(this.data.getBalance(player.getName()) < itemAmount){
			player.sendMessage(Colors.Rose + this.buyNotEnough);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to buy more [" + itemId + "] [" + amount + "] than they have in "+this.moneyName+" [" + itemAmount + "].");
			this.shopLog("buy", player.getName() + "|0|203|" + itemId + "|" + amount + "|" + itemAmount + this.moneyName);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] ["+amount+"] ["+itemAmount+"] [#20335]");
		}

		if (!this.itemCan("buy", cInt(itemId), amount)) {
			player.sendMessage(Colors.Rose + String.format(this.buyInvalidAmount, needsAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to buy bundle [" + itemId + "] with the offset amount [" + amount + "].");
			this.shopLog("buy", player.getName() + "|0|202|" + itemId + "|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] ["+needsAmount+"] ["+amount+"] [#20336]");

			return true;
		}

		if (itemAmount != 0) {
			int total = this.itemCost("buy", cInt(itemId), amount, true);
			String totalAmount = total + this.moneyName;

			if (this.data.getBalance(player.getName()) < total) {
				player.sendMessage(Colors.Rose + this.buyNotEnough);
				return true;
			}

			// Take dat money!
			this.debit(null, player.getName(), total, true);

			// Total giving
			int totalGive = (needsAmount > 1) ? needsAmount*amount : amount;

			// Give dat item!
			player.giveItem(itemId, totalGive);

			// Send Message
			player.sendMessage(Colors.Green + String.format(this.buyGive, totalAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " bought item [" + itemId + "] amount [" + amount + "] total [" + totalAmount + "].");
			this.shopLog("buy", player.getName() + "|1|200|" + itemId + "|" + amount + "|" + total);
		} else {
			// Send Message
			player.sendMessage(Colors.Rose + this.buyReject);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " requested to buy an unavailable item: [" + itemId + "].");
			this.shopLog("buy", player.getName() + "|0|201|" + itemId);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20337]");

			return true;
		}

		return false;
	}

	public boolean doSell(Player player, int itemId, int amount) {
		Inventory bag = player.getInventory();
		int needsAmount = this.itemNeedsAmount("sell", cInt(itemId));

		if (!this.itemCan("sell", cInt(itemId), amount)) {
			player.sendMessage(Colors.Rose + String.format(this.sellInvalidAmount, needsAmount));
			log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to sell [" + amount + "] bundles of [" + itemId + "].");
			this.shopLog("sell", player.getName() + "|0|204|" + itemId + "|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + player.getName() + "] ["+amount+"] ["+needsAmount*amount+"] ["+itemId+"] [#20338]");

			return true;
		}

		int itemAmount = this.itemCost("sell", cInt(itemId), amount, false);

		if (itemAmount != 0) {
			int amt = (needsAmount > 1) ? needsAmount*amount : amount;
			int sold = 0;
			while (amt > 0) {
				if (bag.hasItem(itemId, ((amt > 64) ? 64 : amt), 6400)) {
					sold += ((amt > 64) ? 64 : amt);
					bag.removeItem(new Item(itemId, ((amt > 64) ? 64 : amt)));
					amt -= 64;
				} else {
					break;
				}
			}

			// Really didn't sell anything did we?
			if (sold == 0) {
				player.sendMessage(Colors.Rose + this.sellNone);
				log.info("[iConomy Shop] " + "Player " + player.getName() + " attempted to sell itemId [" + itemId + "] but had none.");
				this.shopLog("sell", player.getName() + "|0|202|" + itemId);

				if(debugging)
					log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20339]");

				return true;
			} else {
				bag.updateInventory();
			}

			// Total
			int total = this.itemCost("sell", cInt(itemId), (sold/needsAmount), true);
			String totalAmount = total + this.moneyName;

			// Send Message
			player.sendMessage(Colors.LightGray + String.format(this.sellGiveAmount, sold, ((needsAmount > 1) ? needsAmount*amount : amount)));
			player.sendMessage(Colors.Green + String.format(this.sellGive, totalAmount));

			// Take dat money!
			this.deposit(null, player.getName(), total, false);

			// Show Balance
			showBalance(player.getName(), null, true);

			log.info("[iConomy Shop] " + "Player " + player.getName() + " sold item [" + itemId + "] amount [" + amount + "] total [" + totalAmount + "].");
			this.shopLog("sell", player.getName() + "|1|200|" + itemId + "|" + amount + "|" + total);
			return true;
		} else {
			// Send Message
			player.sendMessage(Colors.Rose + this.sellReject);
			log.info("[iConomy Shop] " + "Player " + player.getName() + " requested to sell an unsellable item: [" + itemId + "].");
			this.shopLog("sell", player.getName() + "|0|201|" + itemId);

				if(debugging)
					log.info("[iConomy Debugging] [" + player.getName() + "] ["+itemId+"] [#20340]");

			return true;
		}
	}

	/**
	 * Gives money to player 2 idk what the really boolean is all about.
	 */
	public void deposit(String pdata1, String pdata2, int amount, boolean really) {
		// Check
		Player player1 = this.getPlayer(pdata1);
		Player player2 = this.getPlayer(pdata2);

		// Balance
		int i = this.data.getBalance(pdata2);

		i += amount;
		this.data.setBalance(pdata2, i);

		if (really && player2 != null) {
			player2.sendMessage(Colors.Green + "You received " + amount + this.moneyName);
			showBalance(pdata2, null, true);

			if (player1 != null) {
				player1.sendMessage(Colors.Green + amount + this.moneyName + " deposited into " + pdata2 + "'s account");
			}

		}

		if(debugging)
			log.info("[iConomy Debugging] [" + player1 + "] [" + player2 + "] ["+amount+"] [" + really + "] [#20341]");

		if(player2 != null) {
			updateState(player2, true);
		}
	}

	/**
	 * Takes money from player 2
	 * really = ticker usage
	 */
	public void debit(String pdata1, String pdata2, int amount, boolean really) {
		// Check
		Player player1 = this.getPlayer(pdata1);
		Player player2 = this.getPlayer(pdata2);

		// Balance
		int i = this.data.getBalance(pdata2);

		if (amount > i) {
			amount = i;
		}

		i -= amount;
		this.data.setBalance(pdata2, i);

		if (really && player2 != null) {
			player2.sendMessage(Colors.Green + amount + this.moneyName + " was deducted from your account.");
			showBalance(pdata2, null, true);

			if (player1 != null) {
				player1.sendMessage(Colors.Green + amount + this.moneyName + " removed from " + pdata2 + "'s account");
			}

		}

		if(debugging)
			log.info("[iConomy Debugging] [" + player1 + "] [" + player2 + "] ["+amount+"] [" + really + "] [#20342]");

		if(player2 != null) {
			updateState(player2, true);
		}
	}

	public void reset(String pdata, Player local, boolean notify) {
		// Check
		Player player = this.getPlayer(pdata);

		// Reset
		this.data.setBalance(pdata, this.startingBalance);

		// Notify
		if (notify) {
			if (player != null) {
				player.sendMessage(Colors.Green + "Your account has been reset.");
			}
		}

		// Notify the resetter and server regardless.
		local.sendMessage(Colors.Rose + pdata + "'s account has been reset.");
		log.info("[iConomy Money] " + pdata + "'s account has been reset by " + local.getName());

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] [" + local + "] [" + notify + "] [#20343]");

		// Update
		if (player != null) {
			updateState(player, true);
		}
	}

	/**
	 * Takes money from player1, and gives it to player2 determined by amount given.
	 */
	public void pay(Player player1, Player player2, int amount) {
		// Playerdata
		String pdata1 = player1.getName();
		String pdata2 = player2.getName();

		// Balances
		int i = this.data.getBalance(pdata1);
		int j = this.data.getBalance(pdata2);

		if (pdata1.equals(pdata2)) {
			player1.sendMessage(Colors.Rose + "You cannot send yourself money");

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20344]");
		} else if (amount > i) {
			player1.sendMessage(Colors.Rose + "You do not have enough money.");

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20345]");
		} else {
			// Update player one balance
			i -= amount;
			this.data.setBalance(pdata1, i);

			// Update player two balance
			j += amount;
			this.data.setBalance(pdata2, j);

			// Send messages
			player1.sendMessage(Colors.LightGray + "You have sent " + Colors.Green + amount + this.moneyName + Colors.LightGray + " to " + Colors.Green + pdata2);
			player2.sendMessage(Colors.Green + pdata1 + Colors.LightGray + " has sent you " + Colors.Green + amount + this.moneyName);
			
			// Log
			this.shopLog("pay", pdata1 + "|"+pdata2+"|1|200|" + amount);

			if(debugging)
				log.info("[iConomy Debugging] [" + pdata1 + "] [" + pdata2 + "] [" + amount + "] [#20346]");

			// Show each balance
			showBalance(pdata1, null, true);
			showBalance(pdata2, null, true);

			// Update each players state
			updateState(player1, false);
			updateState(player2, true);
		}
	}

	// Return that shit yo :D
	public int getBalance(Player player) {
		return this.data.getBalance(player.getName());
	}

	/**
	 * Gets and displays rank of a player
	 *
	 * If not you @isMe is false, if you @isMe is true.
	 */
	public void showBalance(String name, Player local, boolean isMe) {
		int i = this.data.getBalance(name);
		if (isMe) {
			Player player = this.getPlayer(name);
			player.sendMessage(Colors.LightGray + "Balance: " + Colors.Green + i + this.moneyName);
		} else {
			local.sendMessage(Colors.LightGray + name + "'s Balance: " + Colors.Green + i + this.moneyName);
		}
	}

	/**
	 * Gets and displays rank of a player
	 *
	 * If not you isme is false, if you isme is true.
	 */
	public void rank(Player player, Player local, boolean isMe) {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT player,balance FROM iBalances ORDER BY balance DESC");
				rs = ps.executeQuery();

				while (rs.next()) {
					if (isMe) {
						if (rs.getString("player").equalsIgnoreCase(player.getName())) {
							player.sendMessage(Colors.LightGray + "Your rank is " + Colors.Green + i);
							break;
						}
					} else {
						if (rs.getString("player").equalsIgnoreCase(local.getName())) {
							player.sendMessage(Colors.Green + rs.getString("player") + Colors.LightGray + " rank is " + Colors.Green + i);
							break;
						}
					}
					i++;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the sqlrank for from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}
		} else {
			Map accounts;
			TreeMap<String,Integer> sorted_accounts = null;
			ValueComparator bvc = null;
			int i = 1;

			try {
				accounts = this.data.accounts.returnMap();
				bvc =  new ValueComparator(accounts);
				sorted_accounts = new TreeMap(bvc);
				sorted_accounts.putAll(accounts);
			} catch (Exception ex) {
				Logger.getLogger(iConomy.class.getName()).log(Level.SEVERE, null, ex);
			}

			for (Object key : sorted_accounts.keySet()) {
				String name = (String) key;

				if (isMe) {
					if (name.equalsIgnoreCase(player.getName())) {
						player.sendMessage(Colors.LightGray + "Your rank is " + Colors.Green + i); break;
					}
				} else {
					if (name.equalsIgnoreCase(local.getName())) {
						player.sendMessage(Colors.Green + name + Colors.LightGray + " rank is " + Colors.Green + i); break;
					}
				}

				i++;
			}
		}
	}

	/**
	 * Iterates through the top amount of richest players
	 *
	 * Amount shown is determined by... well the amount given.
	 */
	public void top(Player player, int amount) {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int i = 1;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT player,balance FROM iBalances ORDER BY balance DESC LIMIT 0,?");
				ps.setInt(1, amount);
				rs = ps.executeQuery();
				player.sendMessage(Colors.LightGray + "Top " + Colors.Green + amount + Colors.LightGray + " Richest People:");

				while (rs.next()) {
					player.sendMessage(Colors.LightGray + "   " + i + ". " + Colors.Green + rs.getString("player") + Colors.LightGray + " - " + Colors.Green + rs.getInt("balance") + this.moneyName);
					i++;
				}
			} catch (SQLException ex) {
				log.severe("[iConomy] Unable to grab the sqltop from database!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

		} else {
			Map accounts;
			TreeMap<String,Integer> sorted_accounts = null;
			ValueComparator bvc = null;
			int i = 1;

			try {
				accounts = this.data.accounts.returnMap();
				bvc =  new ValueComparator(accounts);
				sorted_accounts = new TreeMap(bvc);
				sorted_accounts.putAll(accounts);
			} catch (Exception ex) {
				log.severe("[iConomy] Unable to retrieve array of balances!");
			}

			player.sendMessage(Colors.LightGray + "Top " + Colors.Green + amount + Colors.LightGray + " Richest People:");

			if (sorted_accounts.size() < 1) {
				player.sendMessage(Colors.LightGray + "   Nobody Yet!"); return;
			}

			if (amount > sorted_accounts.size()) {
				amount = sorted_accounts.size();
			}

			for (Object key : sorted_accounts.keySet()) {
				String name = (String) key;
				String balance = "" + sorted_accounts.get(name);

				if(i <= amount) {
					// Send top players
					player.sendMessage(Colors.LightGray + "   " + i + ". " + Colors.Green + name + Colors.LightGray + " - " + Colors.Green + balance + this.moneyName);
				} else {
					break;
				}

				i++;
			}
		}
	}

	/**
	 * Retrieves player data
	 */
	private Player getPlayer(String name) {
		etc.getInstance();
		return etc.getServer().getPlayer(name);
	}

	/**
	 * Update list ranking
	 */
	private void insertIntoRankedList(String name) {
		int i = this.data.getBalance(name);
		int j = 0;


		if(debugging)
			log.info("[iConomy Debugging] [" + name + "] [#20347]");

		for (String player : this.rankedList) {
			if (i > this.data.getBalance(player)) {
				break;
			}
			j++;
		}

		this.rankedList.add(j, name);
	}

	public void broadcast(String message)
	{
		for (Player p : etc.getServer().getPlayerList())
			p.sendMessage(message);
	}
	
	public boolean startAuction(Player player, int inter, int itemId, int itemAmount, int startingBid, int minBid, int maxBid) {
		Inventory bag = player.getInventory();
		int amt = itemAmount;
		int sold = 0;
		while (amt > 0) {
			if (bag.hasItem(itemId, ((amt > 64) ? 64 : amt), 6400)) {
				sold = sold + ((amt > 64) ? 64 : amt);
				bag.removeItem(new Item(itemId, (amt > 64 ? 64 : amt)));
				amt -= 64;
			} else {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Auctioner has attempted to cheat!");
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Specified items are not in his inventory!");

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + inter + "] [" + itemId + "] [" + startingBid + "] [" + minBid + "] [" + maxBid + "] [#20348]");

				return false;
			}
		}

		// Really didn't sell anything did we?
		if (sold == 0) {
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Auctioner has attempted to cheat!");
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Yellow + "Specified items are not in his inventory!");

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] [" + inter + "] [" + itemId + "] [" + startingBid + "] [" + minBid + "] [" + maxBid + "] [#20349]");


			this.shopLog("auction", player.getName() + "|null|0|205|"+itemId+"|"+inter+"|"+startingBid+"|"+minBid+"|"+maxBid);

			return false;
		} else {
			bag.updateInventory();
		}

		this.auctionTimerRunning = true;
		this.auctionItem = itemId;
		this.auctionAmount = itemAmount;
		this.auctionStarter = player.getName();
		this.auctionCurBid = 0;
		this.auctionCurSecretBid = 0;
		this.auctionCurAmount = startingBid;
		this.auctionStartingBid = startingBid;
		this.auctionCurBidCount = 0;
		this.auctionMin = minBid;
		this.auctionReserve = maxBid;

		this.shopLog("auction", player.getName() + "|null|1|205|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

		// Setup finals
		final iConomy iHateJava = this;
		final int interval = inter;

		// The timer whoops.
		this.auctionTimer = new Timer();

		// Start
		auctionTimer.scheduleAtFixedRate(new TimerTask() {
		    int i = interval;
		    iConomy p = iHateJava;
		    public void run() {
			this.i--;
			if (i == 10) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + "10" + Colors.LightGray + " seconds left to bid!");
			}

			if (i < 6 && i > 1) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + i + Colors.LightGray + " seconds left to bid!");
			}

			if(i == 1) {
				this.p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + i + Colors.LightGray + " second left to bid!");
			}

			if (i < 1) { this.p.endAuction(); }
		    }
		}, 0L, 1000);
		
		return true;
	}

	public void bidAuction(Player player, int amount, int secret) {
		Boolean outbid = true;
		if(this.auctionCurAmount < amount) {
			if(this.auctionCurBid != 0 && this.auctionCurAmount != this.auctionStartingBid) {
				if(amount < this.auctionCurSecretBid) {
					if(secret != 0 && secret > this.auctionCurSecretBid) {
						if(secret == this.auctionCurSecretBid) {
							amount = secret;
							secret = 0;
						} else {
							amount = this.auctionCurSecretBid+1;
						}
					} else {
						outbid = false;
					}

					if(!outbid) {
						this.auctionCurBid = amount+1;
						this.auctionCurAmount = amount+1;
						this.auctionCurBidCount += 1;

						if(this.auctionCurBid == this.auctionCurSecretBid) {
							Player previous = this.getPlayer(this.auctionCurName);

							if(previous != null) {
								previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "has reached your secret amount!");
							}
						}

						this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] has been raised to " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid by "+ Colors.Green + this.auctionCurName + Colors.LightGray + " secret bid!");

						if(debugging)
							log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20350]");

						this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|200|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

					} else {
						Player previous = this.getPlayer(this.auctionCurName);

						if(previous != null) {
							previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid!");
						}
					}
				} else {
					Player previous = this.getPlayer(this.auctionCurName);

					if(previous != null) {
						previous.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You have been outbid!");
					}
				}
			}

			if(outbid) {
				this.auctionCurBid = amount;
				this.auctionCurSecretBid = (secret == 0) ? amount : secret;
				this.auctionCurAmount = amount;
				this.auctionCurName = player.getName();
				this.auctionCurBidCount += 1;

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20351]");

				this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|201|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			}

			if(this.auctionCurAmount >= this.auctionReserve && this.auctionReserve != 0 && !this.auctionReserveMet) {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " reserve has been met!");
				this.auctionReserveMet = true;

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionReserve+"] ["+this.auctionReserveMet+"] [#20352]");

				this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|1|202|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			} else {
				if(outbid) {
					this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " is now in the lead!");
					this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.LightGray + "Auction currently stands at " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");
				}
			}
		} else {
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + "You must bid over the auction amount!");

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] [" + amount + "] [" + secret + "] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20351]");

			this.shopLog("auction", player.getName() + "|"+this.auctionCurName+"|0|203|" + amount + "|"+secret+"|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
		}
	}

	public void endAuction() {
		auctionTimer.cancel();
		this.auctionTimerRunning = false;

		if(this.auctionCurBid != 0 && this.auctionCurAmount != this.auctionStartingBid) {
			if(this.auctionCurAmount > this.auctionMin) {
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Green + this.auctionCurName + Colors.LightGray + " has won the auction at " + Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + "!");

				Player player = this.getPlayer(this.auctionCurName);
				Player auctioner = this.getPlayer(this.auctionStarter);

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] ["+auctioner+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20352]");

				this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|1|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);

				if(player == null && auctioner == null) {
					this.auctions.setString(this.auctionCurName, this.auctionItem + "," + this.auctionAmount + "," + this.auctionCurAmount);
					this.auctioner.setInt(this.auctionStarter, this.auctionCurAmount);
				} else if (player == null && auctioner != null) {
					this.auctions.setString(this.auctionCurName, this.auctionItem + "," + this.auctionAmount + "," + this.auctionCurAmount);

					// Weee~
					this.deposit(null, auctioner.getName(), this.auctionCurAmount, false);
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Over!");
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been credited to your account!");
				} else if (player != null && auctioner == null) {
					this.debit(null, player.getName(), this.auctionCurAmount, false);
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won! ");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been debited from your account!");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");
					player.giveItem(this.auctionItem, this.auctionAmount);

					this.auctioner.setInt(this.auctionStarter, this.auctionCurAmount);
				} else {
					this.debit(null, player.getName(), this.auctionCurAmount, false);
					this.deposit(null, auctioner.getName(), this.auctionCurAmount, false);

					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Over!");
					auctioner.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been credited to your account!");
					
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won! ");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + this.auctionCurAmount + this.moneyName + Colors.LightGray + " has been debited from your account!");
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");

					// Give items! :D
					player.giveItem(this.auctionItem, this.auctionAmount);
				}
			} else {
				Player player = this.getPlayer(this.auctionStarter);

				if(player != null) {
					player.giveItem(this.auctionItem, this.auctionAmount);
					player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
				}

				if(debugging)
					log.info("[iConomy Debugging] [" + player + "] ["+this.auctionCurName+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20353]");

				this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|0|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
				this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Has ended. No winner as the minimum bid was not met.");
			}
		} else {
			Player player = this.getPlayer(this.auctionStarter);

			if(player != null) {
				player.giveItem(this.auctionItem, this.auctionAmount);
				player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
			}
			

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.auctionCurName+"] ["+this.auctionItem+"] ["+this.auctionAmount+"] ["+this.auctionCurAmount+"] ["+this.auctionCurBidCount+"] ["+this.auctionCurBid+"] ["+this.auctionCurSecretBid+"] [#20354]");


			this.shopLog("auction", player.getName() + "|"+this.auctionStarter+"|-1|204|"+this.auctionItem+"|"+this.auctionAmount+"|"+this.auctionCurAmount+"|"+this.auctionCurBidCount+"|"+this.auctionCurBid+"|"+this.auctionCurSecretBid+"|"+this.auctionReserveMet+"|"+this.auctionReserve);
			this.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Has ended with no bids.");
		}

		this.auctionItem = 0;
		this.auctionAmount = 0;
		this.auctionStarter = "";
		this.auctionCurBid = 0;
		this.auctionCurSecretBid = 0;
		this.auctionCurAmount = 0;
		this.auctionStartingBid = 0;
		this.auctionCurBidCount = 0;
		this.auctionMin = 0;
		this.auctionReserve = 0;
	}

	public boolean wonAuction(String name) {
		return this.auctions.keyExists(name);
	}
	
	public boolean realAuction(String name) {
		return this.auctions.getString(name).contains(",");
	}
	
	public String[] parseAuction(String name) {
		String pauction = this.auctions.getString(name);
		return pauction.split(",");
	}

	public boolean hasAuctions(String name) {
		return this.auctioner.keyExists(name);
	}

	public boolean auctionFailed(String name) {
		return this.auctioner.getString(name).contains(",");
	}

	public String[] parseAuctioner(String name) {
		String auction = this.auctioner.getString(name);
		return auction.split(",");
	}

	public int auctionTotal(String name) {
		return this.auctioner.getInt(name);
	}

	public void auctionItems(Player player) {
		String name = player.getName();
		if(!realAuction(name)){
			return;
		}
		String[] pauction = parseAuction(name);
		int itemId = Integer.parseInt(pauction[0]);
		int itemAmount = Integer.parseInt(pauction[1]);
		int cost = Integer.parseInt(pauction[3]);
		this.debit(null, name, cost, false);
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "You Won the auction!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + cost + this.moneyName + Colors.LightGray + " has been debited from your account!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Enjoy your item(s)!");
		player.giveItem(itemId, itemAmount);

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] ["+itemId+"] ["+itemAmount+"] ["+cost+"] [#20355]");

		this.auctions.removeKey(name);
	}

	public void auctionerItems(Player player) {
		String name = player.getName();
		if(auctionFailed(name)){
			String[] pauction = parseAuctioner(name);
			player.giveItem(Integer.parseInt(pauction[0]), Integer.parseInt(pauction[1]));
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + "Item(s) have been returned to you!");
			player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Yellow + " Had ended with no bids or your min/max was not met.");
			this.auctioner.removeKey(name);

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+pauction[0]+"] ["+pauction[1]+"] [#20356]");

			return;
		}
		int total = auctionTotal(name);
		this.deposit(null, name, total, false);
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.LightGray + "Auction Ended!");
		player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+Colors.Green + total + this.moneyName + Colors.LightGray + " has been credited to your account!");

		if(debugging)
			log.info("[iConomy Debugging] [" + player + "] ["+total+"] [#20357]");

		this.auctioner.removeKey(name);
	}

	public String lotteryPrize() {
		if (this.mysql) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = this.data.MySQL();
				ps = conn.prepareStatement("SELECT * FROM `iPrizes` WHERE id = (SELECT FLOOR(MAX(id) * RAND()) FROM `iPrizes`) ORDER BY id LIMIT 1");
				rs = ps.executeQuery();

				if (rs.next()) {
					// Settings
					int percent = rs.getInt("percent");
					int item = rs.getInt("item");
					int amount = rs.getInt("max-amount");

					return ""+percent+";"+item+";"+amount+";";
				} else {
					return "";
				}
			} catch (SQLException ex) {
				log.severe("[iConomy Lottery] Unable to grab a prize!");
			} finally {
				try {
					if (ps != null) { ps.close(); }
					if (rs != null) { rs.close(); }
					if (conn != null) { conn.close(); }
				} catch (SQLException ex) { }
			}

			return "";
		} else {
			Random generator = new Random();
			Object[] values = this.hashPrizes.values().toArray();

			if(values.length < 1) {
				return "";
			}

			String randomValue = (String) values[generator.nextInt(values.length)];
			return randomValue;
		}
	}

	public void lottery(Player player) {
		if(this.data.getBalance(player.getName()) < this.ticketCost) {
			player.sendMessage(this.lotteryTag + Colors.Rose + " You do not have enough to purchase a ticket!");
			player.sendMessage(this.lotteryTag + Colors.Rose + " Ticket cost: "+ this.ticketCost);

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.data.getBalance(player.getName())+"] [#20358]");

			 return;
		} else {
			String prize = this.lotteryPrize();
			Random generator = new Random();

			// Do lottery Ticket
			this.debit(null, player.getName(), this.ticketCost, false);
			player.sendMessage(this.lotteryTag + Colors.LightGray + " " + String.format(this.lotteryCost, this.ticketCost + this.moneyName));

			// Do lottery checks
			if(prize.equals("")) {
				player.sendMessage(this.lotteryTag + Colors.Rose + " " + this.lotteryNotAvailable); return;
			}

			String[] data = prize.split(";");
			int percent = Integer.parseInt(data[0]);
			int item = Integer.parseInt(data[1]);
			int amount = Integer.parseInt(data[2]);
			int chance = generator.nextInt(100);
			int amountGiven = (amount > 1) ? generator.nextInt(amount) : 1;
			String itemName = (String) this.items.get(cInt(item));
			itemName.replace("-", " ");

			if(chance < percent) {
				if(amountGiven != 0) {
					player.giveItem(item, amountGiven);
					this.broadcast(this.lotteryTag + Colors.Gold + " " + String.format(this.lotteryWinner, player.getName(), amountGiven, itemName));
					// player.sendMessage(this.lotteryTag + Colors.Gold + " Enjoy your items :)!");
				} else {
					player.sendMessage(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
					// this.broadcast(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
				}
			} else {
				player.sendMessage(this.lotteryTag + Colors.LightGray + " " + this.lotteryLoser);
			}

			if(debugging)
				log.info("[iConomy Debugging] [" + player + "] ["+this.data.getBalance(player.getName())+"] ["+amount+"] ["+amountGiven+"] ["+item+"] ["+percent+"] ["+chance+"] ["+itemName+"] [#20359]");

		}
	}

	public boolean canDo(String can, Player player) {
	    if (!can.equals("*")) {
		    String[] groupies = can.split(",");
		    for (String group : groupies){ if(player.isInGroup(group)){return true;}}
		    return false;
	    }
	    return true;
	}

	public boolean can(Player player, String command) {
		if (command.equals("pay")) {
			return this.canDo(this.canPay, player);
		} else if (command.equals("debit")) {
			return this.canDo(this.canDebit, player);
		} else if (command.equals("credit")) {
			return this.canDo(this.canCredit, player);
		} else if (command.equals("reset")) {
			return this.canDo(this.canReset, player);
		} else if (command.equals("rank")) {
			return this.canDo(this.canRank, player);
		} else if (command.equals("view")) {
			return this.canDo(this.canView, player);
		} else if (command.equals("top")) {
			return this.canDo(this.canTop, player);
		} else if (command.equals("sell")) {
			return this.canDo(this.canSell, player);
		} else if (command.equals("buy")) {
			return this.canDo(this.canBuy, player);
		} else if (command.equals("bid")) {
			return this.canDo(this.canBid, player);
		} else if (command.equals("auction")) {
			return this.canDo(this.canAuction, player);
		} else if (command.equals("end")) {
			return this.canDo(this.canEnd, player);
		} else if (command.equals("lottery")) {
			return this.canDo(this.canLottery, player);
		}

		return false;
	}

	public class Listener extends PluginListener {

		iConomy p;

		Listener(iConomy plugin) {
			p = plugin;
		}

		public void onLogin(Player player) {
			updateState(player, true);

			if(p.wonAuction(player.getName())) {
				p.auctionItems(player);
			}

			if(p.hasAuctions(player.getName())){
				p.auctionerItems(player);
			}
		}

		public boolean onCommand(Player player, String[] split) {
			if (!player.canUseCommand(split[0])) {

				if(debugging)
					log.info("[iConomy Debugging] ["+split[0]+"] [cannot use command] [#20360]");

				return false;
			}

			// Player stuff
			Player localPlayer;
			int i;

			/*
			 *  iConomy [Money]
			 *
			 *  @author: Nijikokun
			 *  @description: Allows a player to interact with an imaginary currency
			 */
			if (split[0].equalsIgnoreCase("/money")) {
				// Level 1, /money
				if ((split.length < 2)) {
					//-------------------------------------------------------------
					// TIER 1 [SELF MONEY CHECK
					//-------------------------------------------------------------
					p.showBalance(player.getName(), null, true);
					return true;
				}

				// Level 2, [player], top, rank
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 2 [PAY]
						//-------------------------------------------------------------
						if (!p.can(player, "pay")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-p|pay] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 2 [CREDIT]
						//-------------------------------------------------------------
						if (!p.can(player, "credit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-c|credit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 2 [DEBIT]
						//-------------------------------------------------------------
						if (!p.can(player, "debit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-d|debit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 2 [RESET]
						//-------------------------------------------------------------
						if (!p.can(player, "reset")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-x|reset] <player> <notify(y|n)>");
						return true;
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 2 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						p.top(player, 5);
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 2 [RANK]
						//-------------------------------------------------------------
						if (!p.can(player, "rank")) {
							return false;
						}

						p.rank(player, null, true);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						//-------------------------------------------------------------
						// TIER 2 [PLAYER MONEY CHECK]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "view")) {
							return false;
						}

						localPlayer = p.getPlayer(split[1]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						// Show another players balance
						p.showBalance(pName, player, false);
						return true;
					}
				}

				// Level 3, top [amount], rank [amount], debit [amount] (self)
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 3 [PAY]
						//-------------------------------------------------------------
						if (!p.can(player, "pay")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-p|pay] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 3 [CREDIT]
						//-------------------------------------------------------------
						if (!p.can(player, "credit")) {
							return false;
						}

						player.sendMessage(Colors.Rose + "Invalid Usage: /money [-c|credit] <player> <amount>");
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 3 [DEBIT]
						//-------------------------------------------------------------
						if (!p.can(player, "debit")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-d|debit] <player> <amount>");
							return true;
						}

						// Show this amount!
						p.debit(player.getName(), player.getName(), Integer.parseInt(split[2]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 3 [RESET]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "reset")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						p.reset(pName, player, true);
						return true;
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 3 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							return true;
						}

						// Show this amount!
						p.top(player, i);
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 3 [RANK]
						//-------------------------------------------------------------
						if (!p.can(player, "rank")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							player.sendMessage(Colors.Rose + "Player not online: " + split[2]);
							return true;
						}

						// Show another players rank
						p.rank(localPlayer, player, false);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /money [command|player] [parameter] [parameter]");
						player.sendMessage(Colors.Rose + "    Commands: pay, credit, debit, top, rank");
						player.sendMessage(Colors.Rose + "Alt-Commands: -p, -c, -d, -t, -r");
						return true;
					}
				}

				// Level 4
				if ((split.length < 5)) {
					if (split[1].equalsIgnoreCase("-p") || split[1].equalsIgnoreCase("pay")) {
						//-------------------------------------------------------------
						// TIER 4 [PAY]
						//-------------------------------------------------------------
						if (!p.can(player, "pay")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							player.sendMessage(Colors.Rose + "Player not online: " + split[2]);
							return true;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-p|pay] <player> <amount>");
							return true;
						}

						// Pay amount
						p.pay(player, localPlayer, Integer.parseInt(split[3]));
						return true;
					} else if (split[1].equalsIgnoreCase("-c") || split[1].equalsIgnoreCase("credit")) {
						//-------------------------------------------------------------
						// TIER 4 [CREDIT]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "credit")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-c|credit] <player> <amount>");
							return true;
						}

						// Credit amount
						p.deposit(player.getName(), pName, Integer.parseInt(split[3]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-d") || split[1].equalsIgnoreCase("debit")) {
						//-------------------------------------------------------------
						// TIER 4 [DEBIT]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "debit")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						i = 0;

						try {
							i = Integer.parseInt(split[3]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-d|debit] <player> <amount>");
							return true;
						}

						// Show this amount!
						p.debit(player.getName(), pName, Integer.parseInt(split[3]), true);
						return true;
					} else if (split[1].equalsIgnoreCase("-x") || split[1].equalsIgnoreCase("reset")) {
						//-------------------------------------------------------------
						// TIER 4 [RESET]
						//-------------------------------------------------------------
						String pName = "";

						if (!p.can(player, "reset")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							if(p.data.hasBalance(split[2])) {
								pName = split[2];
							} else {
								player.sendMessage(Colors.Rose + "Player does not have account: " + split[2]);
								return true;
							}
						} else {
							pName = localPlayer.getName();
						}

						if (split[3].equalsIgnoreCase("y") || split[3].equalsIgnoreCase("yes")) {
							p.reset(pName, player, true);
							return true;
						} else if (split[3].equalsIgnoreCase("n") || split[3].equalsIgnoreCase("no")) {
							p.reset(pName, player, false);
							return true;
						} else {
							player.sendMessage(Colors.Rose + "Invalid Parameter[3] for /shop reset. must be y/n");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("-t") || split[1].equalsIgnoreCase("top")) {
						//-------------------------------------------------------------
						// TIER 4 [TOP]
						//-------------------------------------------------------------
						if (!p.can(player, "top")) {
							return false;
						}

						i = 0;

						try {
							i = Integer.parseInt(split[2]);
							if (i < 1) {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException localNumberFormatException2) {
							player.sendMessage(Colors.Rose + "Invalid amount: " + i);
							player.sendMessage(Colors.Rose + "Usage: /money [-t|top] <amount>");
							return true;
						}

						// Show this amount!
						p.top(player, Integer.parseInt(split[2]));
						return true;
					} else if (split[1].equalsIgnoreCase("-r") || split[1].equalsIgnoreCase("rank")) {
						//-------------------------------------------------------------
						// TIER 4 [RANK]
						//-------------------------------------------------------------
						if (!p.can(player, "rank")) {
							return false;
						}

						localPlayer = p.getPlayer(split[2]);

						if (localPlayer == null) {
							player.sendMessage(Colors.Rose + "Invalid Usage: /money [-r|rank] <player>");
							return true;
						}

						// Show another players rank
						rank(localPlayer, player, false);
						return true;
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "money");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /money [command|player] [parameter] [parameter]");
						player.sendMessage(Colors.Rose + "    Commands: pay, credit, debit, top, rank");
						player.sendMessage(Colors.Rose + "Alt-Commands: -p, -c, -d, -t, -r");
						return true;
					}
				}
				return true;
			}

			/*
			 *  iConomy [Shop] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/auction -s|start <time-seconds> <item> <amount> <start-bid> [min-bid] [max-bid] - starts the auction with name for concurrent bids
			 *	/auction -s 30 stone 20
			 *	/auction -b|bid <a> - bid on the current auction
			 *	/auction -e|end - end the auction
			 *	/auction ?|help - help documentation
			 *	/auction - Lists currenct auction details
			 */
			if (split[0].equalsIgnoreCase("/auction") && p.globalAuction) {
				if ((split.length < 2)) {
					if(p.auctionTimerRunning) {
						String itemName = (String) p.items.get(cInt(p.auctionItem));
						itemName.replace("-", " ");
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] "+ Colors.Yellow + " Started by " + Colors.Gold + p.auctionStarter + Colors.Yellow + " Currently Winning: " + Colors.Gold + ((p.auctionCurName == null) ? "Nobody" : p.auctionCurName));
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Item: ["+Colors.LightGray + p.auctionAmount + Colors.Yellow + "] " + Colors.LightGray + itemName);
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Starting Bid: " + Colors.LightGray + p.auctionStartingBid);
						player.sendMessage(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Current Balance: " + Colors.LightGray + p.auctionCurAmount);

					} else {
						player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction ?"+Colors.Rose+" to learn more!");
					}

					return true;
				}

				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							player.sendMessage(Colors.Rose + "Usage: /auction bid <amount>");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b");
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
						player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
						player.sendMessage(Colors.Rose + "Alt-Commands: -s");
						return true;
					}

					return true;
				}

				// Level 3
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							int amount = Integer.parseInt(split[2]);

							if(amount < p.auctionCurAmount) {
								player.sendMessage(Colors.Rose + "You must bid at least over "+p.auctionCurAmount+p.moneyName+"!");
								return true;
							}

							if(amount > p.getBalance(player)) {
								player.sendMessage(Colors.Rose + "You cannot bid more than you have!");
								p.showBalance(player.getName(), player, true);
								return true;
							}

							p.bidAuction(player, amount, 0);

						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
						player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
						player.sendMessage(Colors.Rose + "Alt-Commands: -s");
						return true;
					}

					return true;
				}

				if ((split.length < 10)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "auction");
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("bid")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							player.sendMessage(Colors.Rose + "Cannot bid on your own auction!");
						} else if (p.can(player, "bid")) {
							int amount = Integer.parseInt(split[2]);
							int secret = 0;

							if(amount < p.auctionCurAmount) {
								player.sendMessage(Colors.Rose + "You must bid at least over "+p.auctionCurAmount+p.moneyName+"!");
								return true;
							}

							if(amount > p.getBalance(player)) {
								player.sendMessage(Colors.Rose + "You cannot bid more than you have!");
								p.showBalance(player.getName(), player, true);
								return true;
							}

							if(split.length < 5) {
								secret = Integer.parseInt(split[3]);
							}

							p.bidAuction(player, amount, secret);

						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-e") || split[1].equalsIgnoreCase("end")) {
						if(!p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "No Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(player.getName().equals(p.auctionStarter)) {
							p.endAuction();
						} else if (p.can(player, "end")) {
							p.endAuction();
						} else {
							return false;
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("start")) {
						int max = 0;
						int min = 0;
						int start = 0;
						int amount = 0;
						int itemID = 0;
						int interval = 0;
						String itemName = "";

						if (!p.can(player, "auction")) {
							return false;
						}

						if(p.auctionTimerRunning) {
							player.sendMessage(Colors.Rose + "Auction currently in progress! Use "+Colors.White+"/auction"+Colors.Rose+" to learn more!");
							return true;
						}

						if(split.length < 5) {
							player.sendMessage(Colors.Rose + "Usage: /auction start <time-seconds> <item> <amount> <start-bid>");
							player.sendMessage(Colors.Rose + "    Optional after start-bid: min-bid, max-bid");
							player.sendMessage(Colors.Rose + "Alt-Commands: -s");
							return true;
						}

						// 6
						if(split.length > 5) {
							interval = Integer.parseInt(split[2]);
							amount = Integer.parseInt(split[4]);
							start = Integer.parseInt(split[5]);

							if(interval < 11) {
								player.sendMessage(Colors.Rose + "Interval must be above 10 seconds!");
								return true;
							}

							try {
								itemID = Integer.parseInt(split[3]);
							} catch (NumberFormatException n) {
								itemID = etc.getDataSource().getItem(split[3]);
							}

							if(itemID < 0) {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}

							if (!Item.isValidItem(itemID)) {
								if(p.items.getKey(split[3]) != null) {
									itemID = Integer.parseInt(p.items.getKey(split[3]).toString());
								} else {
									player.sendMessage(Colors.Rose + "Invalid item!");
									return true;
								}
							}

							itemName = (String) p.items.get(cInt(itemID));
							itemName.replace("-", " ");

							if(amount < 1) {
								player.sendMessage(Colors.Rose + "Amount cannot be lower than 1!");
								return true;
							}
						}

						// 7
						if(split.length >= 7) {
							min = Integer.parseInt(split[8]);

							if(min < 3) {
								player.sendMessage(Colors.Rose + "Min cannot be lower than 2!");
								return true;
							}
						}

						// 9
						if(split.length >= 8) {
							max = Integer.parseInt(split[7]);

							if(max < 4) {
								player.sendMessage(Colors.Rose + "Max cannot be lower than 3!");
								return true;
							} else if(max < min) {
								player.sendMessage(Colors.Rose + "Max cannot be lower than minimum bid!");
								return true;
							}
						}

						if(debugging)
							log.info("[iConomy Debugging] ["+player+"] ["+interval+"] ["+itemID+"] ["+amount+"] ["+start+"] ["+min+"] ["+max+"] [#20361]");

						if(p.startAuction(player, interval, itemID, amount, start, min, max)) {
							player.sendMessage(Colors.Yellow + "Auction has begun.");
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " started a new auction.");
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Item: ["+Colors.LightGray + amount + Colors.Yellow + "] " + Colors.LightGray + itemName);
							p.broadcast(Colors.White +"["+ Colors.Gold +"Auction"+ Colors.White +"] " + Colors.Gold + player.getName() + Colors.Yellow + " Starting Bid: " + Colors.LightGray + start);
							return true;
						} else {
							return true;
						}
					}

					return true;
				}
			}

			/*
			 *  iConomy [Shop] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/lottery <i> - Displays amount per 1 item
			 *	/lottery <i> <a> - Displays amount per <a> items
			 *	/lottery -b|buy <i> <a> - Purchase the item
			 *	/lottery -s|sell <i> <a> - Sell an item
			 */
			if (split[0].equalsIgnoreCase("/lottery") && p.globalLottery) {
				if ((split.length < 2)) {
					if (!p.can(player, "lottery")) {
						return false;
					}

					// Do the lottery!
					p.lottery(player); return true;
				}
				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "lottery");
						return true;
					}
				}
			}

			/*
			 *  iConomy [Shop] [Basic 1.0]
			 *
			 *  @author: Nijikokun
			 *  @description: Creates a basic shop!
			 *
			 *  @commands:
			 *	/shop <i> - Displays amount per 1 item
			 *	/shop <i> <a> - Displays amount per <a> items
			 *	/shop -b|buy <i> <a> - Purchase the item
			 *	/shop -s|sell <i> <a> - Sell an item
			 */
			if (split[0].equalsIgnoreCase("/shop") && p.globalShop) {
				// Level 1
				if ((split.length < 2)) {
					player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
					player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
					player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
					return true;
				}

				// Level 2
				if ((split.length < 3)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						int itemID = 0;

						try {
							itemID = Integer.parseInt(split[1]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[1]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[1]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[1]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int bNA = p.itemNeedsAmount("buy", cInt(itemID));
							int sNA = p.itemNeedsAmount("sell", cInt(itemID));

							int buying = p.itemCost("buy", cInt(itemID), bNA, false);
							int selling = p.itemCost("sell", cInt(itemID), sNA, false);

							if (buying != 0) {
								if (bNA > 1) {
									player.sendMessage(" " + Colors.Green + "Must be " + Colors.Green + "bought" + Colors.LightGray + " in bundles of " + Colors.Green + bNA + Colors.LightGray + " for " + Colors.Green + buying + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.LightGray + "Can be " + Colors.Green + "bought" + Colors.LightGray + " for " + Colors.Green + buying + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Currently not for purchasing.");
							}

							if (selling != 0) {
								if (sNA > 1) {
									player.sendMessage(Colors.LightGray + "Must be " + Colors.Green + "sold" + Colors.LightGray + " in bundles of " + Colors.Green + sNA + Colors.LightGray + " for " + Colors.Green + selling + p.moneyName + ".");
								} else {
									player.sendMessage(Colors.LightGray + "Can be " + Colors.Green + "sold" + Colors.LightGray + " for " + Colors.Green + selling + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Currently cannot be sold.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: list, buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -l, -b, -s, ?");
							return true;
						}
					}
				}

				// Level 3
				if ((split.length < 4)) {
					if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else if(split[1].equalsIgnoreCase("-l") || split[1].equalsIgnoreCase("list")) {
						String type = split[2];

						if(type.equalsIgnoreCase("-b") || type.equalsIgnoreCase("buy")) {
							p.doBuyList(player, 1);
						} else if(type.equalsIgnoreCase("-s") || type.equalsIgnoreCase("sell")) {
							p.doSellList(player, 1);
						} else {
							player.sendMessage(Colors.Rose + "Invalid Usage: /shop list [buy|sell] <page>");
							player.sendMessage(Colors.Rose + "Alt-Usage: -l, -b|-s");
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("buy")) {
						int itemID = 0;

						if (!p.can(player, "buy")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int buying = p.itemNeedsAmount("buy", cInt(itemID));

							if (buying != 0) {
								p.doPurchase(player, itemID, 1);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for purchasing.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("sell")) {
						int itemID = 0;

						if (!p.can(player, "sell")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						if (itemID != 0) {
							int selling = p.itemNeedsAmount("sell", cInt(itemID));

							if (selling != 0) {
								p.doSell(player, itemID, 1);
							} else {
								player.sendMessage(Colors.Rose + "Currently cannot be sold.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						int itemID = 0;
						int amount = Integer.parseInt(split[2]);

						try {
							itemID = Integer.parseInt(split[1]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[1]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID) && amount == 0) {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						} else if(!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[1]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[1]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
								player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
								player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
								return true;
							}
						}

						if (itemID != 0) {
							int bNA = p.itemNeedsAmount("buy", cInt(itemID));
							int sNA = p.itemNeedsAmount("sell", cInt(itemID));

							int buying = p.itemCost("buy", cInt(itemID), amount, false);
							int selling = p.itemCost("sell", cInt(itemID), amount, false);
							int totalBuying = p.itemCost("buy", cInt(itemID), amount, true);
							int totalSelling = p.itemCost("sell", cInt(itemID), amount, true);

							if (buying != 0) {
								if (bNA > 1) {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " bundles will cost " + Colors.Green + totalBuying + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " will cost " + Colors.Green + totalBuying + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Invalid amount or not for purchasing!");
							}

							if (selling != 0) {
								if (sNA > 1) {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " bundles will sell for " + Colors.Green + totalSelling + p.moneyName + Colors.LightGray + ".");
								} else {
									player.sendMessage(Colors.Green + amount + Colors.LightGray + " can be sold for " + Colors.Green + totalSelling + p.moneyName + Colors.LightGray + ".");
								}
							} else {
								player.sendMessage(Colors.Rose + "Invalid Amount or not for selling!");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					}
				}

				// Level 4
				if ((split.length < 5)) {
					if (split[1].equalsIgnoreCase("-b") || split[1].equalsIgnoreCase("buy")) {
						int itemID = 0;

						if (!p.can(player, "buy")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						int amount = Integer.parseInt(split[3]);

						if (amount < 0 || amount == 0) {
							player.sendMessage(Colors.Rose + "Invalid amount!");
							return true;
						}

						if (itemID != 0) {
							int buying = p.itemNeedsAmount("buy", cInt(itemID));

							if (buying != 0) {
								p.doPurchase(player, itemID, amount);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for purchasing.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if(split[1].equalsIgnoreCase("-l") || split[1].equalsIgnoreCase("list")) {
						String type = split[2];

						int amount = Integer.parseInt(split[3]);

						if (amount < 2 || amount == 1) {
							amount = 1;
						}

						if(type.equalsIgnoreCase("-b") || type.equalsIgnoreCase("buy")) {
							p.doBuyList(player, amount);
						} else if(type.equalsIgnoreCase("-s") || type.equalsIgnoreCase("sell")) {
							p.doSellList(player, amount);
						} else {
							player.sendMessage(Colors.Rose + "Invalid Usage: /shop list [buy|sell] <page>");
							player.sendMessage(Colors.Rose + "Alt-Usage: -l, -b|-s");
						}

						return true;
					} else if (split[1].equalsIgnoreCase("-s") || split[1].equalsIgnoreCase("sell")) {
						int itemID = 0;

						if (!p.can(player, "sell")) {
							return false;
						}

						try {
							itemID = Integer.parseInt(split[2]);
						} catch (NumberFormatException n) {
							itemID = etc.getDataSource().getItem(split[2]);
						}

						if(itemID < 0) {
							player.sendMessage(Colors.Rose + "Invalid item!");
							return true;
						}

						if (!Item.isValidItem(itemID)) {
							if(p.items.getKey(split[2]) != null) {
								itemID = Integer.parseInt(p.items.getKey(split[2]).toString());
							} else {
								player.sendMessage(Colors.Rose + "Invalid item!");
								return true;
							}
						}

						int amount = Integer.parseInt(split[3]);

						if (amount < 0 || amount == 0) {
							player.sendMessage(Colors.Rose + "Invalid amount!");
							return true;
						}

						if (itemID != 0) {
							int selling = p.itemNeedsAmount("sell", cInt(itemID));

							if (selling != 0) {
								p.doSell(player, itemID, amount);
							} else {
								player.sendMessage(Colors.Rose + "Item currently not for selling.");
							}

							return true;
						} else {
							player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
							player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
							player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
							return true;
						}
					} else if (split[1].equalsIgnoreCase("?") || split[1].equalsIgnoreCase("help")) {
						p.halp(player, "shop");
						return true;
					} else {
						player.sendMessage(Colors.Rose + "Usage: /shop [command|item|itemID] [item] [amount]");
						player.sendMessage(Colors.Rose + "    Commands: buy, sell, help");
						player.sendMessage(Colors.Rose + "Alt-Commands: -b, -s, ?");
						return true;
					}
				}
			}
			return false;
		}
	}

	class ValueComparator implements Comparator {

	  Map base;
	  public ValueComparator(Map base) {
	      this.base = base;
	  }

	  public int compare(Object a, Object b) {
		int ax = Integer.parseInt((String)base.get(a));
		int bx = Integer.parseInt((String)base.get(b));

	    if(ax < bx) {
	      return 1;
	    } else if(ax == bx) {
	      return 0;
	    } else {
	      return -1;
	    }
	  }
	}
}
