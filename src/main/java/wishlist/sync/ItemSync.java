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
	
	public ItemSync() {
		if(!_isInitialized) {
			_cacheFolder = System.getProperty("user.dir") + "\\cache\\";
			File cacheFolder = new File(_cacheFolder);		
			_cacheFilePathPattern = cacheFolder + "\\%1$s.json";
			
			initializeCacheFolder(cacheFolder);
			
			_cacheFileExpiration = Calendar.getInstance();
			_cacheFileExpiration.add(Calendar.DATE, 7);
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
				_checkLimit();

		    	File cache_file = new File(String.format(_cacheFilePathPattern, current_id));
		    	if(cache_file.exists() && _cacheFileExpiration.getTimeInMillis() > cache_file.lastModified()) {
		    		System.out.println(Thread.currentThread().getName() + ": " + current_id + " file is cached");
		    	} else {
					System.out.println(Thread.currentThread().getName() + ": Sync ID " + current_id.toString());
					SyncItem(current_id);
		    	}
			}
		}

		System.out.println("Exiting thread " + Thread.currentThread().getName());
		return;
	}
	
	private void insertRecord() {
		/*
		try {
			Connection conn = null;
			conn = DriverManager.getConnection("jdbc:mysql://ostwebdev.com:3306/ostwebde_wishlist?user=tmpuser&password=tmppass&useJDBCCompliantTimezoneShift=true&serverTimezone=PST");
			PreparedStatement statement = conn.prepareStatement("SELECT * FROM test");
			ResultSet results = statement.executeQuery();
			results.next();

			System.out.println(results.getInt(1));
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
}
