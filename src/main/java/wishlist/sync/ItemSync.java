package wishlist.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.api.mysqla.result.Resultset;

import wishlist.sync.Item;

public class ItemSync implements Runnable {
	private static boolean _isInitialized = false;
	private static AtomicInteger id = new AtomicInteger(0);
	private static Integer _maxRecords = 36000;
	private static String contentType = "item";
	private static RequestLimitManager limitManager;
	
	private static String _cacheFolder;
	private static String _cacheFilePathPattern;
	private static Calendar _cacheFileExpiration;
	
	private Connection conn = null;
	private String sql = "INSERT INTO items_new (name, description, summary, icon, wowId, requiredSkillRank, itemLevel, sellPrice, created, updated) VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE name = ?, description = ?, summary = ?, icon = ?, requiredSkillRank = ?, itemLevel = ?, sellPrice = ?, updated = NOW();";
	private PreparedStatement statement = null;
	
	
	public ItemSync() {
		if(!_isInitialized) {
			_cacheFolder = System.getProperty("user.dir") + "\\cache\\";
			File cacheFolder = new File(_cacheFolder);		
			_cacheFilePathPattern = cacheFolder + "\\%1$s.json";
			
			initializeCacheFolder(cacheFolder);
			
			_cacheFileExpiration = Calendar.getInstance();
			_cacheFileExpiration.add(Calendar.DATE, 7);
		}
		
		try {
			conn = DriverManager.getConnection("jdbc:mysql://ubuntu:3306/wishlist?user=dbuser&password=dbuser&useJDBCCompliantTimezoneShift=true&serverTimezone=PST");
			statement = conn.prepareStatement(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static void setStartId(Integer StartId) {
		id.set(StartId);
	}
	
	private void initializeCacheFolder(File cacheFolder) {
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
    
    private void _writeFile(Integer id, String content) {
    	Path file = Paths.get(String.format(_cacheFilePathPattern, id));
    	try {
    		Files.write(file, Arrays.asList(content), Charset.forName("UTF-8"));	
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
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

    	InputStream is;
		try {
			is = new URL(url).openStream();
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	    	String json_string = _readAll(rd);
	    	ObjectMapper mapper = new ObjectMapper();
	    	Item item = mapper.readValue(json_string,  Item.class);
	    	System.out.println(item.name);

	    	_writeFile(ItemId, json_string);
			insertRecord(item);	    	
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(FileNotFoundException e) {
			// Pass, should create empty file
			_writeFile(ItemId, "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public void run() {
		Integer current_id;
		boolean loop = true;
		
		while(loop) {
			 current_id = id.getAndIncrement();

			if(current_id > _maxRecords) {
				loop = false;
			} else {
		    	File cache_file = new File(String.format(_cacheFilePathPattern, current_id));
		    	if(cache_file.exists() && _cacheFileExpiration.getTimeInMillis() > cache_file.lastModified()) {
		    		System.out.println(Thread.currentThread().getName() + ": " + current_id + " file is cached");
		    		
			    	try {
				    	ObjectMapper mapper = new ObjectMapper();			    		
						Item item = mapper.readValue(cache_file, Item.class);
						insertRecord(item);
					} catch (JsonParseException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (JsonMappingException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    		
		    	} else {
					_checkLimit();		    		
					System.out.println(Thread.currentThread().getName() + ": Sync ID " + current_id.toString());
					SyncItem(current_id);
		    	}
			}
		}

		System.out.println("Exiting thread " + Thread.currentThread().getName());
		return;
	}
	
	private void insertRecord(Item item) {
		try {
			statement.setString(1,  item.name);
			statement.setString(2, item.description);
			statement.setString(3, "");
			statement.setString(4, item.icon);
			statement.setInt(5, item.id);
			statement.setInt(6, item.requiredSkillRank);
			statement.setInt(7, item.itemLevel);
			statement.setInt(8, item.sellPrice);
			statement.setString(9, item.name);
			statement.setString(10,  item.description);
			statement.setString(11,  "");
			statement.setString(12,  item.icon);
			statement.setInt(13, item.requiredSkillRank);
			statement.setInt(14, item.itemLevel);
			statement.setInt(15,  item.sellPrice);
			
			
			int num_updated = statement.executeUpdate();
			if(num_updated == 0) {
				System.out.println("Record not 	updated");
			} else {
				System.out.println("Record updated");
			}

			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
