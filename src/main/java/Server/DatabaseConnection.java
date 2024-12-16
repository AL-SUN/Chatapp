package Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Date;
import static Utils.ResourceLoader.loadProperties;
import static Utils.ResourceLoader.loadResource;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseConnection {

    boolean isMySQL = true; // false if using SQLite
    private final String DB_URL;
    private String USER;
    private String PASS;
    private final String SQL_FILEPATH; // Change path in config.properties

    private final String FILE_PATH;
    private final String TMP_PATH;


    public DatabaseConnection() {
        // TODO: change to your MySQL database settings in config.properties
        Properties properties = loadProperties();
        if(isMySQL){
            DB_URL = properties.getProperty("db.url");
            USER = properties.getProperty("db.user");
            PASS = properties.getProperty("db.password");

        } else {
            DB_URL = properties.getProperty("sqlite.url");
        }
        SQL_FILEPATH = properties.getProperty("db.sql.file");
        FILE_PATH = properties.getProperty("server.filedir");
        TMP_PATH = properties.getProperty("server.tmpdir");
    }

    public Connection connect() throws SQLException {
        if(isMySQL){
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } else {
            return DriverManager.getConnection(DB_URL);
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

    public int saveMsg(String username, String message) {
        int msgId = -1;
        try (Connection conn = connect()) {
            String sql = "INSERT INTO messages (room_id, sender, message) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, 1); // room_id is 1 for default chat room
                pstmt.setString(2, username);
                pstmt.setString(3, message);
                // time is stored as current timestamp by default
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    msgId = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to save message: " + e.getMessage());
        }
        return msgId;
    }

    /**
     * Save the file to the server and update the file path in the database
     * @param messageId the message ID to associate with the file
     * @param filename the name of the file saved temporarily
     * @param fileType File or Audio
     */
    public void saveFile(int messageId, String filename, String fileType) {
        try (Connection conn = connect()) {
            // 1.insert a new record to get attachment_id
            String sql = "INSERT INTO Attachments (file_name, message_id, file_path, file_type) VALUES (?, ?, '', ?)";
            int attachmentId = -1;
            String fileBaseName=  filename.split("_")[2]; // remove username and timestamp

            try (PreparedStatement pstmt = conn.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1,fileBaseName);
                pstmt.setInt(2, messageId);
                pstmt.setString(3, fileType);
                pstmt.executeUpdate();

                // get the generated attachment_id
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    attachmentId = rs.getInt(1);
                }
            }
            if (attachmentId == -1) {
                System.err.println("Failed to insert attachment record.");
                return;
            }

            // 2. save a new file name using attachment_id
            String serverStoragePath = FILE_PATH + LocalDate.now() + "/"; // e.g., files/2021-09-01/
            String extension = fileBaseName.substring(fileBaseName.lastIndexOf(".")); // (e.g., .png)
            String newFileName = "attachment_" + attachmentId + extension; // e.g. attachment_1001.png
            Path targetPath = Paths.get(serverStoragePath + newFileName);
            Path sourcePath = Paths.get(TMP_PATH + filename);
            try {
                Files.createDirectories(Paths.get(serverStoragePath));
                Files.copy(sourcePath, targetPath);
            } catch (IOException e) {
                System.err.println("Failed to copy file: " + e.getMessage());
                return;
            }

            // 3. update the file path in the database
            String updateSql = "UPDATE Attachments SET file_path = ? WHERE attachment_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, serverStoragePath + newFileName);
                pstmt.setInt(2, attachmentId);
                pstmt.executeUpdate();
                System.out.println("File uploaded and path updated successfully: " + targetPath);
            } catch (SQLException e) {
                System.err.println("Failed to update file path: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Failed to save file: " + e.getMessage());
        }
    }

    public void initDatabase() {
        // read the SQL file
        StringBuilder sqlBuilder = new StringBuilder();

        try (InputStream inputStream = loadResource(SQL_FILEPATH); // read file from resources folder
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
            System.err.println("Failed to read SQL file: "+ SQL_FILEPATH + e.getMessage());
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
        // NOTE: reset the database by clearing the database and running this main method
        // In SQLite, delete the database in the project directory directly (e.g., "db/database.db"), it will be recreated automatically
        // In MySQL, clear the database, it needs database exist already with the name "chatapp", can't delete
        DatabaseConnection db = new DatabaseConnection();
        db.initDatabase();

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