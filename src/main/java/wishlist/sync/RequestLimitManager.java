package wishlist.sync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RequestLimitManager {
	public static AtomicLong _startTime = new AtomicLong(0);
	public static Long _batchDurationThreshold = TimeUnit.SECONDS.toNanos(1);
	public static Integer _batchNumItemThreshold = 100;
	public static AtomicInteger _numItems = new AtomicInteger(0);
	
	public static void _checkStartNewBatch() {
		if(_getBatchDifference() > _batchDurationThreshold) {
			System.out.println("Resetting");
			_startTime.set(System.nanoTime());
			_numItems.set(0);
		}
	}
	
	private static Long _checkBatchWait() {
		_checkStartNewBatch();
		if(_numItems.get() >= _batchNumItemThreshold && _getBatchDifference() < _batchDurationThreshold) {
			return (_startTime.get() + _batchDurationThreshold) - System.nanoTime();
		}

		return 0L;
	}
	
	private static Long  _getBatchDifference() {
			return System.nanoTime() - _startTime.get();
	}
	
	public static Long IncrementAndCheckWait() {
		 _numItems.incrementAndGet();
		return _checkBatchWait();
	}
}
