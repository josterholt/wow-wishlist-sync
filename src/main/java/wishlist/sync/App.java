package wishlist.sync;


import java.util.Date;
import java.util.concurrent.TimeUnit;

import wishlist.sync.ItemSync;


/**
 * Entry point for sync script
 *
 */
public class App 
{
	
    public static void main( String[] args ) throws InterruptedException
    {
    	System.out.println(new Date()); // Start timestamp
    	SyncManager manager = new SyncManager();
    	manager.RunSync();
    	System.out.println(new Date()); // End timestamp
    }
    

}
