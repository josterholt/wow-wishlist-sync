package wishlist.sync;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.api.mysqla.result.Resultset;

import wishlist.sync.Item;

public class ItemSync implements Callable {
	private static boolean _isInitialized = false;
	private static AtomicInteger id = new AtomicInteger(0);
	private static Integer _maxRecords = 36000;
	private static String contentType = "item";
	private static RequestLimitManager limitManager;
	
	private static String _cacheFolder;
	private static String _cacheFilePathPattern;
	private static Calendar _cacheFileExpiration;
	
	private Connection conn = null;
	private String sql = "INSERT INTO items ("
			+ "name,"
			+ "description,"
			+ "summary,"
			+ "icon,"
			+ "wowId,"
			+ "stackable,"
			+ "itemBind,"
			+ "buyPrice,"
			+ "itemClass,"
			+ "itemSubClass,"
			+ "containerSlots,"
			+ "weaponinfoDamageMin,"
			+ "weaponinfoDamageMax,"
			+ "weaponinfoDamageExactMin,"
			+ "weaponinfoDamageExactMax,"
			+ "weaponinfoWeaponSpeed,"
			+ "weaponinfoDPS,"			
			+ "inventoryType,"
			+ "equippable,"
			+ "itemLevel,"
			+ "maxCount,"
			+ "maxDurability,"
			+ "minFactionId,"
			+ "minReputation,"
			+ "quality,"
			+ "sellPrice,"
			+ "requiredSkill,"
			+ "requiredLevel,"
			+ "requiredSkillRank,"
			+ "baseArmor,"
			+ "hasSockets,"
			+ "isAuctionable,"
			+ "armor,"
			+ "displayInfoId,"
			+ "nameDescription,"
			+ "nameDescriptionColor,"
			+ "upgradable,"
			+ "heroicTooltip,"
			+ "context,"
			+ "artifactId,"
			+ "created,"
			+ "updated"
			+ ") "
			+ "VALUES("
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"			
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"			
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"			
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"			
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "?,"
			+ "NOW(),"
			+ "NOW()"
			+ ")"
			+ "ON DUPLICATE KEY UPDATE "
			+ "name = ?, "
			+ "description = ?, "
			+ "summary = ?, "
			+ "icon = ?, "
			+ "stackable = ?,"
			+ "itemBind = ?,"
			+ "buyPrice = ?,"
			+ "itemClass = ?,"
			+ "itemSubClass = ?,"
			+ "containerSlots = ?,"
			+ "weaponinfoDamageMin = ?,"
			+ "weaponinfoDamageMax = ?,"
			+ "weaponinfoDamageExactMin = ?,"
			+ "weaponinfoDamageExactMax = ?,"
			+ "weaponinfoWeaponSpeed = ?,"
			+ "weaponinfoDPS = ?,"			
			+ "inventoryType = ?,"
			+ "equippable = ?,"
			+ "itemLevel = ?,"
			+ "maxCount = ?,"
			+ "maxDurability = ?,"
			+ "minFactionId = ?,"
			+ "minReputation = ?,"
			+ "quality = ?,"
			+ "sellPrice = ?,"
			+ "requiredSkill = ?,"
			+ "requiredLevel = ?,"
			+ "requiredSkillRank = ?,"
			+ "baseArmor = ?,"
			+ "hasSockets = ?,"
			+ "isAuctionable = ?,"
			+ "armor = ?,"
			+ "displayInfoId = ?,"
			+ "nameDescription = ?,"
			+ "nameDescriptionColor = ?,"
			+ "upgradable = ?,"
			+ "heroicTooltip = ?,"
			+ "context = ?,"
			+ "artifactId = ?,"
			+ "updated = NOW();";
	private PreparedStatement statement = null;
	
	private static Properties config;
	
	private KeyStoreCache cache;
	
	
	public ItemSync() {
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + config.getProperty("db_host", "localhost") + ":" + config.getProperty("db_port", "3306") + "/" + config.getProperty("db_name", "") + "?user=" + config.getProperty("db_user", "") + "&password=" + config.getProperty("db_password", "") + "&useJDBCCompliantTimezoneShift=true&serverTimezone=PST");
			statement = conn.prepareStatement(sql);
		} catch (SQLException e) {
			System.out.println("Unable to connect");
		}
		
		cache = new KeyStoreCache(_cacheFolder);
	}
	
	@Override
	public void finalize() {
		cache.close();
	}
	
	private static String promptWithQuestion(String question, String default_answer) {
		String full_prompt;
		if(default_answer != "" && default_answer != null) {
			full_prompt = question + "(" + default_answer + "):";
		} else {
			full_prompt = question + ":";
		}
		
		System.out.println(full_prompt);
		
		Scanner scannerName = new Scanner(System.in);
		scannerName.useDelimiter(System.getProperty("line.separator"));
		return scannerName.next();
	}
	
	private static void setConfigFromPrompt(String key, String question, String default_answer) {
		String answer = promptWithQuestion(question, default_answer);
			
		if(answer == "" || answer == null) {
			config.setProperty(key, default_answer);
		} else {
			config.setProperty(key, answer);
		}
	}
	
	public static void initialize() {
		System.out.println(System.getProperty("user.home"));
		File propertyFile = new File(System.getProperty("user.home") + "\\.wowsync");
		config = new Properties();
		
		if(propertyFile.exists()) {
			try(InputStream input = new FileInputStream(propertyFile)) {
				config.load(input);
				
				System.out.println(config.getProperty("cacheDirectory"));
				
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		} else {
			try(OutputStream output = new FileOutputStream(propertyFile)) {
				setConfigFromPrompt("cacheDirectory", "Enter cache folder", config.getProperty("cacheDirectory", ""));
				setConfigFromPrompt("db_host", "Enter database host", config.getProperty("cacheDirectory", ""));
				
				setConfigFromPrompt("db_port", "Enter database port", config.getProperty("cacheDirectory", ""));
				setConfigFromPrompt("db_name", "Enter database name", config.getProperty("cacheDirectory", ""));
				setConfigFromPrompt("db_user", "Enter database user", config.getProperty("cacheDirectory", ""));
				setConfigFromPrompt("db_password", "Enter database password", config.getProperty("cacheDirectory", ""));
				
				if(config.getProperty("cacheDirectory") == "") {
					System.out.println("Invalid folder specified");
					System.exit(-1);
				}

				config.store(output, null);
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}

		_cacheFolder = config.getProperty("cacheDirectory");
		File cacheFolder = new File(_cacheFolder);		
		_cacheFilePathPattern = cacheFolder + "\\%1$s.json";
		
		initializeCacheFolder(cacheFolder);
		
		_cacheFileExpiration = Calendar.getInstance();
		_cacheFileExpiration.add(Calendar.DATE, 7);

	}
	
	public static void setStartId(Integer StartId) {
		id.set(StartId);
	}
	
	private static void initializeCacheFolder(File cacheFolder) {
		if(!cacheFolder.exists()) {
			if(!cacheFolder.mkdirs()) {
				System.out.println("Unable to create cache folder");
			}
		}
	}
	
	public static void setMaxRecords(Integer maxRecords) {
		_maxRecords = maxRecords;
	}

	public static Integer getMaxRecords() {
		return _maxRecords;
	}
	   
    private String _readAll(Reader rd) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	int cp;
    	while((cp = rd.read()) != -1) {
    		sb.append((char) cp);
    	}
    	return sb.toString();
    }
    
    private void _writeCache(Integer id, String content) {
		cache.put(id, content);
		/*
    	Path file = Paths.get(String.format(_cacheFilePathPattern, id));
    	try {
    		Files.write(file, Arrays.asList(content), Charset.forName("UTF-8"));	
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	*/
    }
    
    private void _checkLimit() {
		Long sleep_duration;    	
		sleep_duration = limitManager.incrementAndCheckWait();
		if(sleep_duration > 0) {
			try {
				System.out.println(Thread.currentThread().getName() + " Sleeping for " + TimeUnit.NANOSECONDS.toMillis(sleep_duration));
				Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleep_duration));
				//System.out.println(Thread.currentThread().getName() + " woke up");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    private void SyncItem(Integer ItemId) {
    	String url = "https://us.api.battle.net/wow/" + contentType + "/" + ItemId.toString() + "?apikey=***REMOVED***";

		try {
			/**
			 * Grab JSON from URL
			 */
	    	InputStream is;
			System.out.println(Thread.currentThread().getName() + ": Opening API URL");
			long startTime = System.currentTimeMillis();
			is = new URL(url).openStream();
			//new URL(url).openConnection().getContent()

			System.out.println(Thread.currentThread().getName() + ": Reading contents");
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	    	String json_string = _readAll(rd);
	    	System.out.println(Thread.currentThread().getName() + ": Content read");
	    	
	    	/**
	    	 * Store JSON in cache
	    	 */
	    	startTime = System.currentTimeMillis();
	    	_writeCache(ItemId, json_string);
	    	System.out.println(Thread.currentThread().getName() + ": Finished writing to cache: " + (System.currentTimeMillis() - startTime));
	    	
	    	/**
	    	 * Deserialize JSON into object
	    	 */
	    	ObjectMapper mapper = new ObjectMapper();
	    	Item item = mapper.readValue(json_string,  Item.class);
	    	System.out.println(Thread.currentThread().getName() + ": Finished API URL. " + (System.currentTimeMillis() - startTime));	    	
	    	System.out.println(Thread.currentThread().getName() + ": Writing to cache");
	    	
	    	/**
	    	 * Insert record into main database
	    	 */
	    	System.out.println(Thread.currentThread().getName() + ": Inserting record into database");
			insertRecord(item);
			System.out.println(Thread.currentThread().getName() + ": Record inserted into database");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(FileNotFoundException e) {
			// Pass, should create empty file
			_writeCache(ItemId, "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Entry point for Callable
     * Runs through IDs and adds to main database
     * If ID does not exist in cache, a fetch is made against Blizzard API, content is parsed and upserted into main database
     * If ID exists in cache, cached content is parsed and upserted into main database
     */
	public Boolean call() {
		Integer current_id;
		boolean loop = true;
		
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		long globalStartTime = System.currentTimeMillis();
		int numRecordsProcessed = 0;
		int numMisses = 0;
		
		while(loop) {
			 current_id = id.getAndIncrement();

			if(current_id > _maxRecords) {
				System.out.println("Max records hit (" + _maxRecords + ")");
				loop = false;
			} else {
				System.out.println(Thread.currentThread().getName() + ": Processing " + current_id);
				String cache_content = cache.get(current_id);
				if(cache_content != null) {
		    		//System.out.println(Thread.currentThread().getName() + ": " + current_id + " file is cached");
		    		//System.out.println(Thread.currentThread().getName() + ": cache file " + current_id);
		    		long startTime = System.currentTimeMillis();
			    	try {
			    		//System.out.println(cache_content.toString());
				    	ObjectMapper mapper = new ObjectMapper();
				    	String json_content = cache_content.toString();
				    	if(!json_content.isEmpty()) {
							Item item = mapper.readValue(json_content, Item.class);
							insertRecord(item);
				    	}
				    	numRecordsProcessed++;
				    	long timeElapsedInMili = (System.currentTimeMillis() - globalStartTime);
				    	long globalTimeElapsed = TimeUnit.MILLISECONDS.toSeconds(timeElapsedInMili);

				    	if(globalTimeElapsed >= 60) {
				    		//System.out.println("AVERAGE: " + (numRecordsProcessed / globalTimeElapsed) + "(" + numRecordsProcessed + "/" + numMisses + ")");
				    		//System.out.println("Item Num: " + item.id);
				    		globalStartTime = System.currentTimeMillis();
				    		numRecordsProcessed = 0;
				    		numMisses = 0;
				    	}						
					} catch (JsonParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						numMisses++;
					} catch (JsonMappingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						numMisses++;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						numMisses++;
					} catch(Exception e) {
						e.printStackTrace();
					}
			    	//System.out.println(Thread.currentThread().getName() + ": end cache file parsing: " + (System.currentTimeMillis() - startTime));
			    	//if(numRecordsProcessed % 100 == 0) {
			    	//}
		    		
		    	} else {
					_checkLimit();		    		
					System.out.println(Thread.currentThread().getName() + ": Sync ID " + current_id.toString());
					SyncItem(current_id);
		    	}
			}
			
		}
		
		try {
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Exiting thread " + Thread.currentThread().getName());
		return true;
	}
	
	private void insertRecord(Item item) {
		try {
			// Insert
			statement.setString(1,  item.name);
			statement.setString(2, item.description);
			statement.setString(3, "");
			statement.setString(4, item.icon);
			statement.setInt(5, item.id);
			statement.setBoolean(6,  item.stackable);
			statement.setBoolean(7, item.itemBind);
			statement.setInt(8, item.buyPrice);
			statement.setInt(9, item.itemClass);
			statement.setInt(10,  item.itemSubClass);
			statement.setInt(11,  item.containerSlots);



//			statement.setInt(12,  item.weaponInfo.damage.min);
//			statement.setInt(13, item.weaponInfo.damage.max);
//			statement.setInt(14, item.weaponInfo.damage.exactMin);
//			statement.setInt(15, item.weaponInfo.damage.exactMax);
//			statement.setInt(16, item.weaponInfo.weaponSpeed);
//			statement.setFloat(17, item.weaponInfo.dps);
			statement.setInt(12,  0);
			statement.setInt(13, 0);
			statement.setInt(14, 0);
			statement.setInt(15, 0);
			statement.setInt(16, 0);
			statement.setFloat(17, 0);
			
			

			
			statement.setInt(18, item.inventoryType);
			statement.setBoolean(19,  item.equippable);
			statement.setInt(20, item.itemLevel);
			statement.setInt(21, item.maxCount);
			statement.setInt(22, item.maxDurability);
			statement.setInt(23, item.minFactionId);
			statement.setInt(24,  item.minReputation);
			statement.setInt(25, item.quality);
			statement.setInt(26, item.sellPrice);
			statement.setInt(27,  item.requiredSkill);
			statement.setInt(28,  item.requiredLevel);
			statement.setInt(29, item.requiredSkillRank);
			statement.setInt(30, item.baseArmor);
			statement.setBoolean(31, item.hasSockets);
			statement.setBoolean(32,  item.isAuctionable);
			statement.setInt(33,  item.armor);
			statement.setInt(34,  item.displayInfoId);
			statement.setString(35, item.nameDescription);
			statement.setString(36,  item.nameDescriptionColor);
			statement.setBoolean(37,  item.upgradable);
			statement.setBoolean(38, item.heroicTooltip);
			statement.setString(39,  item.context);
			statement.setInt(40, item.artifactId);
			
			// Update
			statement.setString(41,  item.name);
			statement.setString(42, item.description);
			statement.setString(43, "");
			statement.setString(44, item.icon);
			statement.setBoolean(45,  item.stackable);
			statement.setBoolean(46, item.itemBind);
			statement.setInt(47, item.buyPrice);
			statement.setInt(48, item.itemClass);
			statement.setInt(49,  item.itemSubClass);
			statement.setInt(50,  item.containerSlots);
			
			
//			statement.setInt(51,  item.weaponInfo.damage.min);
//			statement.setInt(52, item.weaponInfo.damage.max);
//			statement.setInt(53, item.weaponInfo.damage.exactMin);
//			statement.setInt(54, item.weaponInfo.damage.exactMax);
//			statement.setInt(55, item.weaponInfo.weaponSpeed);
//			statement.setFloat(56, item.weaponInfo.dps);
			statement.setInt(51, 0);
			statement.setInt(52, 0);
			statement.setInt(53, 0);
			statement.setInt(54, 0);
			statement.setInt(55, 0);
			statement.setFloat(56, 0);			
			
			
			
			statement.setInt(57, item.inventoryType);
			statement.setBoolean(58,  item.equippable);
			statement.setInt(59, item.itemLevel);
			statement.setInt(60, item.maxCount);
			statement.setInt(61, item.maxDurability);
			statement.setInt(62, item.minFactionId);
			statement.setInt(63,  item.minReputation);
			statement.setInt(64, item.quality);
			statement.setInt(65, item.sellPrice);
			statement.setInt(66,  item.requiredSkill);
			statement.setInt(67,  item.requiredLevel);
			statement.setInt(68, item.requiredSkillRank);
			statement.setInt(69, item.baseArmor);
			statement.setBoolean(70, item.hasSockets);
			statement.setBoolean(71,  item.isAuctionable);
			statement.setInt(72,  item.armor);
			statement.setInt(73,  item.displayInfoId);
			statement.setString(74, item.nameDescription);
			statement.setString(75,  item.nameDescriptionColor);
			statement.setBoolean(76,  item.upgradable);
			statement.setBoolean(77, item.heroicTooltip);
			statement.setString(78,  item.context);
			statement.setInt(79, item.artifactId);

			
			int num_updated = statement.executeUpdate();
			if(num_updated == 0) {
				System.out.println("Record not 	updated");
			} else {
				//System.out.println("Record updated");
			}
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
