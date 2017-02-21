package wishlist.sync;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SyncManager {
	RequestLimitManager limitManager;
	
	public void RunSync() {
		System.out.println(System.getProperty("user.dir") + "\\cache\\");
		File cacheFolder = new File(System.getProperty("user.dir") + "\\cache\\");
		if(!cacheFolder.exists()) {
			if(!cacheFolder.mkdirs()) {
				System.out.println("Unable to create cache folder");
			}
		}
		
		String cacheFilePathPattern = cacheFolder + "\\%1$s.json";
		
		System.out.println("RunSync");
		
		// Thread initialization
		Integer numThreads = Runtime.getRuntime().availableProcessors() + 1;
		System.out.println("Num Threads: " + numThreads.toString());
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		// Initialize global vars
		AtomicInteger id = new AtomicInteger(0);
		RequestLimitManager.addLimit(new Limit(TimeUnit.SECONDS.toNanos(1), 100));
		RequestLimitManager.addLimit(new Limit(TimeUnit.HOURS.toNanos(1), 300));
		
		// Initialize start time for request limits		
		Integer maxId = 36000;
		for(int i = 0; i < numThreads; i++) {
			System.out.println("Adding thread");
			executor.execute(new ItemSync(id, maxId, cacheFilePathPattern));
		}

		while(id.get() <= maxId) {
			try {
				Thread.sleep(1000000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("Shutting down threads");
		executor.shutdown();
		while(!executor.isTerminated()) {
			
		}
		System.out.println("Finished");
	}	
}