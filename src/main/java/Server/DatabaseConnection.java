package Server;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import static Utils.ResourceLoader.loadProperties;
import static Utils.ResourceLoader.loadResource;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseConnection {

    boolean isMySQL = true; // false if using SQLite
    private static final String DATABASE_SQL_FILE_PATH = "./db/database.sql"; // Change path if necessary

    public Connection connect() throws SQLException {
        // TODO: change to your MySQL database settings in config.properties
        Properties properties = loadProperties();
        if(isMySQL){
            String DB_URL = properties.getProperty("db.url");
            String USER = properties.getProperty("db.user");
            String PASS = properties.getProperty("db.password");
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } else {
            String url = properties.getProperty("sqlite.url");
            return DriverManager.getConnection(url);
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

    public void initDatabase() {
        // read the SQL file
        StringBuilder sqlBuilder = new StringBuilder();

        try (InputStream inputStream = loadResource(DATABASE_SQL_FILE_PATH); // read file from resources folder
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--") && !line.trim().isEmpty()) { // ignore comments and empty lines
                    if(!isMySQL) { // change dialect in SQLite
                        if (line.contains("CREATE DATABASE")||line.contains("USE")) continue; // skip creating database
                        if (line.contains("IGNORE")) {
                            line = line.replace("IGNORE", "OR IGNORE");
                        }
                        if(line.contains("AUTO_INCREMENT")) {
                            line = line.replace("AUTO_INCREMENT", "AUTOINCREMENT");
                        }
                    }
                    sqlBuilder.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read SQL file: "+ DATABASE_SQL_FILE_PATH + e.getMessage());
        }

        // execute the SQL commands
        try (Connection conn = connect()) {
            conn.setAutoCommit(false); // enable transactions

            String[] sqlStatements = sqlBuilder.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String sql : sqlStatements) {
//                    System.out.println(sql);
                    String trimmedSql = sql.trim();
                    if (!trimmedSql.isEmpty()) {
//                        System.out.println("Executing: " + trimmedSql);
                        stmt.execute(trimmedSql);
                    }
                }
                conn.commit();
                System.out.println("Database is initialized successfully.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("SQL error: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true); // reset to default mode
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws SQLException {
        // NOTE: reset the database by deleting the database directly and running this main method
        // In SQLite, delete the file in the project directory  (e.g., "db/database.db")
        // In MySQL, drop the database in MySQL server
        DatabaseConnection db = new DatabaseConnection();
        db.initDatabase();  // this will create database automatically, no need to create manually

        // test: show all tables in the database
        /*Connection conn = db.connect();
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        while (tables.next()) {
            System.out.println(tables.getString("TABLE_NAME"));
        }
        conn.close();*/

        /*//test createUser
        db.createUser("test", "test");
        boolean res= db.verifyUser("test", "test");
        System.out.println(res);*/
    }
}