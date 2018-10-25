package wishlist.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import wishlist.sync.Limit;


/**
 * Manager class for limits. Allows multiple limits to be added and incremented/checked. 
 * 
 * @author Justin Osterholt
 *
 */
public class RequestLimitManager {
	private static List<Limit> limits = new ArrayList<Limit>();
	
	/**
	 * Add a limit instance to the manager
	 * 
	 * @param limit	Instance of limit
	 * @exception Throws IllegalArgumentException if limit is null
	 */
	public static void addLimit(Limit limit) {
		if (limit == null) {
			throw new IllegalArgumentException("limit can't be null");
		}
		
		limits.add(limit);
	}
	
	/*
	 * Loops through all limits, increments count and processes threshold and wait
	 * for each limit
	 * 
	 * @return Long Longest wait time among limits
	 */
	public static Long incrementAndCheckWait() {
		Long wait_duration = 0L;
		for(Limit limit : limits) {
			Long tmp_duration = limit.IncrementAndWait();
			if(tmp_duration > wait_duration) {
				wait_duration = tmp_duration;
			}
		}
		return wait_duration;
	}
}
