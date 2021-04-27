package chatT;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBConnect {
	
	public PreparedStatement psmt;
	public Connection con;
	public ResultSet rs;
	
	public DBConnect() {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			String url = "jdbc:oracle:thin://@localhost:1521:orcl";
			String userID = "kosmo";
			String userPW = "1234";
			con = DriverManager.getConnection(url, userID, userPW);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			if(con != null) con.close();
			if(psmt != null) psmt.close();
			if(rs != null) rs.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
