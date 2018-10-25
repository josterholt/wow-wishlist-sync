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


/**
 * SynManager has three stages; init, execution, cleanup.
 * init - Initializes ItemSync static members, runs multiple instances of ItemSync through threads
 * execution - Runs callable instances
 * cleanup - Cleans up threads
 * 
 * @author Justin Osterholt
 */
public class SyncManager {
	RequestLimitManager limitManager;
	
	public void RunSync() {	
		/**
		 * INIT STAGE: Setup threading and sync environment/variables.
		 * numThreads - Number of sync threads to spin up in relation to number or CPU cores
		 * RequestLimitManager - Add rate limits so this app respects external endpoint limits
		 * ItemSync Static Variables - Set start ID and max record ID (we don't know the external max record ID)
		 */
		// Define the number of threads used in numThreads. Initialize threads based on CPU cores.
		Integer numThreads = Runtime.getRuntime().availableProcessors() + 1;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		System.out.println("Num Threads: " + numThreads.toString());	
		
		// Set rate limits so sync will pause and avoid errors
		RequestLimitManager.addLimit(new Limit(TimeUnit.SECONDS.toNanos(1), 100));
		RequestLimitManager.addLimit(new Limit(TimeUnit.HOURS.toNanos(1), 36000));

		// Initialize start time for request limits
		ItemSync.initialize();
		ItemSync.setMaxRecords(200000); // @todo Detect the end of valid IDs if possible

		// Add callable instance to list so they can be managed during execution phase
		ArrayList<Callable<ItemSync>> tasks = new ArrayList<Callable<ItemSync>>();
		for(int i = 0; i < numThreads; i++) {
			tasks.add(new ItemSync());
		}


		/**
		 * EXECUTION STAGE: Start all sync threads, loop until all threads are finished.
		 * Sleeps at end of check so we aren't hammering the CPU.
		 */
		try {
			// Start callables for every ItemSync instance
			List<Future<ItemSync>> futures = executor.invokeAll(tasks);

			// Main loop, checks state of thread (future)
			boolean loop = true;
			while(loop) {
				loop = false;
				for(Future<ItemSync> future : futures) {
					System.out.println(future.isDone());
					if(!future.isDone()) {
						loop = true;
					}
				}
				
				System.out.println("Waiting...");
				Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				

		/**
		 * CLEAN UP STAGE: Threads are done running, clean up
		 */
		System.out.println("Shutting down threads");
		executor.shutdown();
		while(!executor.isTerminated()) {
			
		}
		System.out.println("Finished");
	}	
}