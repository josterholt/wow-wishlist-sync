package wishlist.sync;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SyncManager {
	private Long _startTime;
	private Long _batchDurationThreshold = TimeUnit.SECONDS.toNanos(1);
	private Integer _batchNumItemThreshold = 100;
	private Integer _numItems = 0;
		
	public void RunSync() {
		
		File cacheFolder = new File(System.getProperty("user.dir") + "\\cache\\");
		if(!cacheFolder.exists()) {
			if(!cacheFolder.mkdirs()) {
				System.out.println("Unable to create cache folder");
			}
		}
		
		String cacheFilePathPattern = cacheFolder + "\\%1$s.json";
		
		System.out.println("RunSync");
		_startTime = System.nanoTime();
		
		Integer numThreads = Runtime.getRuntime().availableProcessors() + 1;
		System.out.println("Num Threads: " + numThreads.toString());
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		Integer id = 0;
		Integer maxId = 5000;
		while(id <= maxId) {

			//System.out.println(String.format(cacheFilePathPattern, id.toString()));
			executor.execute(new ItemSync(id, String.format(cacheFilePathPattern, id.toString())));
			id++;
			IncrementAndCheckWait();
		}
		executor.shutdown();
		while(!executor.isTerminated()) {
			
		}
		System.out.println("Finished");
	}
	
	private void _checkStartNewBatch() {
		if(_getBatchDifference() > _batchDurationThreshold) {
			System.out.println("Resetting");
			_startTime = System.nanoTime();
			_numItems = 0;
		}
	}
	
	private void _checkBatchWait() {
		_checkStartNewBatch();
		if(_numItems >= _batchNumItemThreshold && _getBatchDifference() < _batchDurationThreshold) {
			System.out.println("Sleeping...");
			try {
				Long diff = (_startTime + _batchDurationThreshold) - System.nanoTime();
				//System.out.println(TimeUnit.NANOSECONDS.toMillis(diff));
				if(diff > 0) {
					Long sleepCheck = System.nanoTime();
					Thread.sleep(TimeUnit.NANOSECONDS.toMillis(diff));
					
					Long sleepDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sleepCheck);
					System.out.println("Slept for: " + sleepDuration);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private Long  _getBatchDifference() {
			return System.nanoTime() - _startTime;
	}
	
	public void IncrementAndCheckWait() {
		_numItems++;
		_checkBatchWait();
	}
	
}