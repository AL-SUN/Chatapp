package Server;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

import org.mindrot.jbcrypt.BCrypt;

public class MySQLConnection {

    public Connection connect() throws SQLException {
        Properties properties = new Properties();
        try {
            // TODO: change to your MySQL database settings in config.properties
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            String DB_URL = properties.getProperty("db.url");
            String USER = properties.getProperty("db.user");
            String PASS = properties.getProperty("db.password");
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createUser(String username, String password) throws SQLException {
        try(Connection conn = connect()) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();
            }
        }
    }

    public boolean verifyUser(String username, String password) {
        try (Connection conn = connect()) {
            String sql = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        return BCrypt.checkpw(password, hashedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;  //should not reach here
    }

    public boolean saveMsg(String username, String message) {
        try (Connection conn = connect()) {
            String sql = "INSERT INTO messages (room_id, sender, message) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, 1); // room_id is 1 for default chat room
                pstmt.setString(2, username);
                pstmt.setString(3, message);
                // time is stored as current timestamp by default
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws SQLException {
        MySQLConnection mySQLConnection = new MySQLConnection();
        mySQLConnection.createUser("test", "test");
        boolean res=mySQLConnection.verifyUser("test", "test");
        System.out.println(res);
    }
}