package wishlist.sync;

import java.io.BufferedReader;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import wishlist.sync.Item;

public class ItemSync implements Runnable {
	private Integer id;
	private static String contentType = "item";
	private static String filePath = "C:\\Users\\Justin\\Misc Code\\WoW Bucket List\\other\\data_cache_java\\%1$s.json";
	
	public ItemSync(Integer id) {
		this.id = id;
	}
	   
    private static String ReadAll(Reader rd) throws IOException {
    	StringBuilder sb = new StringBuilder();
    	int cp;
    	while((cp = rd.read()) != -1) {
    		sb.append((char) cp);
    	}
    	return sb.toString();
    }
    
    private static void WriteFile(Integer id, String content) throws IOException {
    	Path file = Paths.get(String.format(filePath, id.toString()));
		Files.write(file, Arrays.asList(content), Charset.forName("UTF-8"));
    }

	public void run() {
		
		System.out.println("Syncing " + id.toString());

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
	}
}
