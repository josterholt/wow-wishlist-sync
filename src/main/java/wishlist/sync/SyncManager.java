package wishlist.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncManager {
	RequestLimitManager limitManager;
	
	public void RunSync() {
		System.out.println("RunSync");
		
		// Thread initialization
		Integer numThreads = Runtime.getRuntime().availableProcessors() + 1;
		System.out.println("Num Threads: " + numThreads.toString());
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		// Initialize global vars
		AtomicInteger id = new AtomicInteger(0);
		RequestLimitManager.addLimit(new Limit(TimeUnit.SECONDS.toNanos(1), 100));
		RequestLimitManager.addLimit(new Limit(TimeUnit.HOURS.toNanos(1), 36000));
		
		
		// Initialize start time for request limits
		ItemSync.initialize();
		ItemSync.setStartId(0);
		ItemSync.setMaxRecords(200000); // Shouldn't need to do this
		
		for(int i = 0; i < numThreads; i++) {
			System.out.println("Adding thread");
			executor.execute(new ItemSync());
		}

		while(id.get() <= ItemSync.getMaxRecords()) {
			try {
				Thread.sleep(1000);
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