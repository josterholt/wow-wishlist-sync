package wishlist.sync;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Limit {
	private AtomicLong _startTime = new AtomicLong(0);
	private Long _durationThreshold;
	private Integer _countThreshold;
	private AtomicInteger _currentCount = new AtomicInteger();
	
	public Limit(Long durationThreshold, Integer countThreshold) {
		_startTime.set(System.nanoTime());
		_durationThreshold = durationThreshold;
		_countThreshold = countThreshold;
	}
	
	public void _checkStartNewBatch() {	
		if(_getBatchDifference() > _durationThreshold) {
			System.out.println("Resetting");
			_startTime.set(System.nanoTime());
			_currentCount.set(0);
		}
	}
	
	private Long _checkBatchWait() {
		_checkStartNewBatch();
		if(_currentCount.get() >= _countThreshold && _getBatchDifference() < _durationThreshold) {
			return (_startTime.get() + _durationThreshold) - System.nanoTime();
		}

		return 0L;
	}
	
	private Long  _getBatchDifference() {
			return System.nanoTime() - _startTime.get();
	}

	public Long IncrementAndWait() {		
		 _currentCount.incrementAndGet();
		return _checkBatchWait();
	}
}