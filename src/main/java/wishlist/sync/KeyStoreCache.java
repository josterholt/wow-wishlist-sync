package wishlist.sync;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class KeyStoreCache {
	private static Connection conn = null;
	
	KeyStoreCache(String cacheFolder) {
		if(conn == null) {		
			try {
				conn = DriverManager.getConnection("jdbc:sqlite:" + cacheFolder + "cache.db");
				Statement statement = conn.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS cache (id integer primary key, value varchar(255), modified integer, created integer)");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void put(Integer id, String content) {
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement("INSERT OR REPLACE INTO cache (id, value, modified, created) VALUES(?, ?, (SELECT strftime('%s', 'now')), COALESCE((SELECT created FROM cache WHERE id = ?), (SELECT strftime('%s', 'now'))))");
			statement.setInt(1, id);
			statement.setString(2,  content);
			statement.setInt(3, id);
			statement.executeUpdate();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String get(Integer id) {
		PreparedStatement statement;
		try {
			statement = conn.prepareStatement("SELECT id, value FROM cache WHERE id = ? AND modified IS NOT NULL");
			statement.setInt(1,  id);
			ResultSet results = statement.executeQuery();
			if(results.next()) {
				return results.getString("value");				
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return null;
	}
	
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
