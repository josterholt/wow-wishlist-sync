package wishlist.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import wishlist.sync.Limit;


public class RequestLimitManager {
	private static List<Limit> limits = new ArrayList<Limit>();
	
	public static void addLimit(Limit limit) {
		limits.add(limit);
	}
	
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
