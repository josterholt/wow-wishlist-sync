package wishlist.sync;


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
    	SyncManager manager = new SyncManager();
    	manager.RunSync();
    }
    

}
