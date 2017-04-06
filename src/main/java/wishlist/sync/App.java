package wishlist.sync;


import java.util.Date;
import java.util.concurrent.TimeUnit;

import wishlist.sync.ItemSync;


/**
 * Hello world!
 *
 */
public class App 
{
	
    public static void main( String[] args ) throws InterruptedException
    {
    	System.out.println(new Date());
    	SyncManager manager = new SyncManager();
    	manager.RunSync();
    	System.out.println(new Date());
    }
    

}
