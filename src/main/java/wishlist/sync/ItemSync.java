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
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import wishlist.sync.Item;

public class ItemSync implements Runnable {
	private AtomicInteger id;
	private Integer maxRecords;
	private static String contentType = "item";
	private String cacheFilePath;
	
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
    
    private void WriteFile(AtomicInteger id, String content) throws IOException {
    	Path file = Paths.get(cacheFilePath);
		Files.write(file, Arrays.asList(content), Charset.forName("UTF-8"));
    }

	public void run() {
		Integer current_id = id.incrementAndGet();
		while(maxRecords < current_id) {
			System.out.println(current_id);
			try {
				Thread.currentThread().sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//System.out.println("Syncing " + id.toString());
		
		return;
		/*
    	String url = "https://us.api.battle.net/wow/" + contentType + "/" + id.toString() + "?apikey=***REMOVED***";

    	InputStream is;
		try {
			is = new URL(url).openStream();
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	    	String json_string = ReadAll(rd);
	    	ObjectMapper mapper = new ObjectMapper();
	    	Item item = mapper.readValue(json_string,  Item.class);
	    	System.out.println(item.name);

	    	WriteFile(id, json_string);
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
}
