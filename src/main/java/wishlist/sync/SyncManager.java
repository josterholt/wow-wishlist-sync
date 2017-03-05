package wishlist.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
		
		ArrayList<Callable<ItemSync>> tasks = new ArrayList<Callable<ItemSync>>();
		for(int i = 0; i < numThreads; i++) {
			System.out.println("Adding thread");
			//executor.execute(new ItemSync());
			tasks.add(new ItemSync());
		}
				
		try {
			List<Future<ItemSync>> futures = executor.invokeAll(tasks);

			boolean loop = true;
			while(loop) {
				loop = false;
				for(Future<ItemSync> future : futures) {
					System.out.println(future.isDone());
					if(!future.isDone()) {
						loop = true;
					}
				}
				
				Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
			
		System.out.println("Shutting down threads");
		executor.shutdown();
		while(!executor.isTerminated()) {
			
		}
		System.out.println("Finished");
	}	
}