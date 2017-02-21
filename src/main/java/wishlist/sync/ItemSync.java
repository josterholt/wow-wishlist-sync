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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import wishlist.sync.Item;

public class ItemSync implements Runnable {
	private AtomicInteger id;
	private Integer maxRecords;
	private static String contentType = "item";
	private String cacheFilePath;
	private static RequestLimitManager limitManager;
	
	public ItemSync(AtomicInteger id, Integer maxRecords, String cacheFilePath) {
		this.id = id;
		this.maxRecords = maxRecords;
		this.cacheFilePath = cacheFilePath;
	}
	   
    private String ReadAll(Reader rd) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	int cp;
    	while((cp = rd.read()) != -1) {
    		sb.append((char) cp);
    	}
    	return sb.toString();
    }
    
    private void _writeFile(Integer id, String content) throws IOException {
    	Path file = Paths.get(String.format(cacheFilePath, id));
		Files.write(file, Arrays.asList(content), Charset.forName("UTF-8"));
    }
    
    private void _checkLimit() {
		Long sleep_duration;    	
		sleep_duration = limitManager.incrementAndCheckWait();
		if(sleep_duration > 0) {
			try {
				System.out.println(Thread.currentThread().getName() + " Sleeping for " + TimeUnit.NANOSECONDS.toMillis(sleep_duration));
				Thread.sleep(TimeUnit.NANOSECONDS.toMillis(sleep_duration));
				System.out.println(Thread.currentThread().getName() + " woke up");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    private void SyncItem(Integer ItemId) {
    	System.out.println("Sync item " + ItemId);
    	return;
    	/*
    	String url = "https://us.api.battle.net/wow/" + contentType + "/" + ItemId.toString() + "?apikey=***REMOVED***";

    	InputStream is;
		try {
			is = new URL(url).openStream();
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	    	String json_string = ReadAll(rd);
	    	ObjectMapper mapper = new ObjectMapper();
	    	Item item = mapper.readValue(json_string,  Item.class);
	    	System.out.println(item.name);

	    	_writeFile(ItemId, json_string);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(FileNotFoundException e) {
			// Pass, should create empty file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    }

	public void run() {
		Integer current_id;
		boolean loop = true;
		
		while(loop) {
			 current_id = id.getAndIncrement();

			if(current_id > maxRecords) {
				loop = false;
			} else {
				_checkLimit();
				System.out.println(Thread.currentThread().getName() + ": Sync ID " + current_id.toString());
				SyncItem(current_id);
			}
		}
		
		//System.out.println("Syncing " + id.toString());
		System.out.println("Exiting thread " + Thread.currentThread().getName());
		return;
		/*

		*/
	}
}
