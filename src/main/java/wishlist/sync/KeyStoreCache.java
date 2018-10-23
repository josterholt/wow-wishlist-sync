package wishlist.sync;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;


public class KeyStoreCache {
	private static Connection _conn = null;
	private static String _cacheFolder = null;
	private static int _count = 0;
	private static Long _globalTimeElapsedMili = 0L;
	private static Long _globalQueryTimeElapsed = 0L;
	private static Long _globalTimeMili;
	private PreparedStatement _statement;
	private PreparedStatement _updateStatement;			
	
	private static final ReentrantLock dbLock = new ReentrantLock(true);

	KeyStoreCache(String cacheFolder) {
		_cacheFolder = cacheFolder;
		_globalTimeMili = System.currentTimeMillis();
		
		if(_conn == null) {		
			try {
				_openConnection();
				Statement statement = _conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS cache (id integer primary key, value varchar(255), modified integer, created integer)");
				_conn.close();				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	protected DBLockClosable lockDB() {
		dbLock.lock();
		return new DBLockClosable();
	}

	protected class DBLockClosable implements AutoCloseable {
		@Override
		public void close() {
			dbLock.unlock();
		}
	}	
	
	private boolean _openConnection() {
		try {	
			_conn = DriverManager.getConnection("jdbc:sqlite:" + _cacheFolder + "cache.db");
		

			if(_conn == null) {
				return false;
			}
			
			if(_conn.isClosed()) {
				return false;
			}	
			_statement = _conn.prepareStatement("SELECT id, value FROM cache WHERE id = ? AND modified IS NOT NULL AND modified > ?");
			_updateStatement = _conn.prepareStatement("INSERT OR REPLACE INTO cache (id, value, modified, created) VALUES(?, ?, (SELECT strftime('%s', 'now')), COALESCE((SELECT created FROM cache WHERE id = ?), (SELECT strftime('%s', 'now'))))");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private boolean _checkAndReconnect() {
		System.out.println(Thread.currentThread().getName() + " reconnecting if closed");
		try {
			if(_conn.isClosed()) {
				//System.out.println("Connection is closed, reconnecting");
				_openConnection();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public void put(Integer id, String content) {
		long startTime = System.currentTimeMillis();
		System.out.println(Thread.currentThread().getName() + " Locking DB in PUT");
		try(DBLockClosable dblc = lockDB()) {
		//try {
			_checkAndReconnect();
			_updateStatement.clearParameters();
			_updateStatement.setInt(1, id);
			_updateStatement.setString(2,  content);
			_updateStatement.setInt(3, id);
			_updateStatement.executeUpdate();	
			System.out.println("Set Elapsed: " + (System.currentTimeMillis() - startTime));
			_conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.print("Not closing connection for some reason (1)");
		}
	}
	
	public String get(Integer id) {
		//System.out.println("Getting something");
		Long start_time = System.currentTimeMillis();
		System.out.println(Thread.currentThread().getName() + " Locking DB in GET");
		try(DBLockClosable dblc = lockDB()) {
			_checkAndReconnect();
			
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -7);
			long cache_expiration = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
			//System.out.println(cache_expiration);
			
			_statement.clearParameters();
			_statement.setInt(1,  id);
			_statement.setLong(2, cache_expiration);
			
			ResultSet results = _statement.executeQuery();

			String value = null;
			if(results.next()) {
		    	value = results.getString("value");				 				
			}
			_conn.close();
			
			long _queryTimeElapsedMili = System.currentTimeMillis() - start_time;
			_globalQueryTimeElapsed += _queryTimeElapsedMili;
			long globalTimeElapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - _globalTimeMili);
	    	_count++;
	    	if(globalTimeElapsed >= 30) {
	    		System.out.println("KEY AVERAGE: " + (_count / _globalQueryTimeElapsed) + "(" + _count + " / " + _globalQueryTimeElapsed + ")");
	    		_count = 0;
	    		_globalTimeMili = System.currentTimeMillis();
	    		_globalQueryTimeElapsed = 0L;
	    	}
	    	
			return value;
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.print("Not closing connection for some reason (2)");
		}

		//System.out.println("Get Elapsed (out): " + (System.currentTimeMillis() - startTime));
		System.out.println("Fail out - return null");
		_globalTimeElapsedMili += System.currentTimeMillis() - start_time;
		
		return null;
	}
	
	public void close() {
		try {
			_conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
