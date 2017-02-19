package wishlist.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SyncManager {
	private Long _startTime;
	private Long _batchDurationThreshold = 1000L;
	private Integer _batchNumItemThreshold = 2;
	private Integer _numItems = 0;
		
	public void RunSync() {
		System.out.println("RunSync");
		_startTime = System.nanoTime();
		ExecutorService executor = Executors.newFixedThreadPool(5);
		Integer id = 0;
		Integer maxId = 100;
		while(id <= maxId) {
			System.out.println("Syncing " + id.toString());
			executor.execute(new ItemSync(id));
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
		//System.out.println("Items: " + _numItems.toString() + " vs " + _batchNumItemThreshold.toString());
		//System.out.println("Time: " + _getBatchDifference().toString() + " vs " + _batchDurationThreshold.toString());
		if(_numItems > _batchNumItemThreshold && _getBatchDifference() < _batchDurationThreshold) {
			System.out.println("Sleeping...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private Long  _getBatchDifference() {
	
		System.out.println(_startTime);
		System.out.println(System.nanoTime());
		System.out.println(System.nanoTime() - _startTime);

		System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - _startTime));
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - _startTime);
	}
	
	public void IncrementAndCheckWait() {
		_numItems++;
		_checkBatchWait();
	}
	
}