package wishlist.sync;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Represents progress against a rate limit (ex. number of URL requests within a given time).
 * Uses the term batch to refer to the time since construction or since last reset.
 * 
 * @author Justin Osterholt
 *
 */
class Limit {
	private AtomicLong _startTime = new AtomicLong(0);
	private Long _durationThreshold;
	private Integer _countThreshold;
	private AtomicInteger _currentCount = new AtomicInteger();
	
	/**
	 * Initializes rate limit constraints
	 * 
	 * @param durationThreshold	Amount of time before the count against the rate limit is cleared 
	 * @param countThreshold	Number of requests that may occur within the duration threshold
	 */
	public Limit(Long durationThreshold, Integer countThreshold) {
		_startTime.set(System.nanoTime());
		_durationThreshold = durationThreshold;
		_countThreshold = countThreshold;
	}
	
	/**
	 * Increment _currentCount by one and returns the amount of time to wait. Will return zero if 
	 * rate threshold has not been exceeded, otherwise returns time remaining before rate reset.
	 * 
	 * @return Long	Amount of time to wait
	 */
	public Long IncrementAndWait() {
		 // @todo get is called again right below, might be able to clean this up
		_currentCount.incrementAndGet(); 

		// Reset count if _durationThreshold has been exceed
		_checkStartNewBatch();
		
		// Return time remaining if the rate limit has been passed
		if(_currentCount.get() >= _countThreshold && _getBatchDifference() < _durationThreshold) {
			return (_startTime.get() + _durationThreshold) - System.nanoTime();
		}

		// Count threshold not exceeded, no wait
		return 0L;

	}	
	
	/*
	 * @return Long Duration since beginning of current batch
	 */
	private Long  _getBatchDifference() {
			return System.nanoTime() - _startTime.get();
	}

	/*
	 * Resets _startTime and _currentCount if rate limit duration has passed
	 * Example. Will reset after one minute if rate limit is 300 records per minute
	 * 
	 * @return void
	 */
	private void _checkStartNewBatch() {	
		if(_getBatchDifference() > _durationThreshold) {
			System.out.println("Resetting");
			_startTime.set(System.nanoTime());
			_currentCount.set(0);
		}
	}
}