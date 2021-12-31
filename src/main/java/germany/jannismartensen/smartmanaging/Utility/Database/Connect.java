package germany.jannismartensen.smartmanaging.Utility.Database;

import germany.jannismartensen.smartmanaging.SmartManaging;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.sql.*;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class Connect {
    public static Connection connect(SmartManaging plugin) {


        Connection conn;
        try {
            // db parameters
            File databaseFile = new File(plugin.getDataFolder(), "logindata.db");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!databaseFile.exists()) {
                databaseFile.createNewFile();
            }

            String url = String.format("jdbc:sqlite:%s", databaseFile);
            // create a connection to the database
            conn = DriverManager.getConnection(url);


            // SQL statement for creating a new table
            String sql = """
                    CREATE TABLE IF NOT EXISTS player (
                    name text NOT NULL UNIQUE,
                    password text NOT NULL,
                    cookie text UNIQUE );""";

            try {
                Statement stmt = conn.createStatement();
                // create a new table
                stmt.execute(sql);
            } catch (SQLException e) {
                log(e.getMessage(), 3);
            }


            log("Connection to Database successful.");

            return conn;

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void insertUser(String server, Connection conn, CommandSender user, String password) {
        String sql = "INSERT INTO player(name, password) VALUES(?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            if (server.isEmpty()) {
                server = "<Server IP>";
            }

            user.sendMessage("Welcome! Lookup your profile under " + server + ":" + SmartManaging.port);

        } catch (SQLException e) {
            user.sendMessage("You could not be registered! Please try again later.");
            log(e.getMessage(), 3);
        }
    }

    public static void insertCookie(Connection conn, String username, String cookieString) throws SQLException {
        String sql = "UPDATE player SET cookie = ? WHERE name = ?";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, cookieString);
        pstmt.setString(2, username);
        pstmt.executeUpdate();
    }

    public static String getPlayerFromCookie(Connection conn, String cookie) {
        String sql = "SELECT name FROM player WHERE cookie = ?";
        String name = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, cookie);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Only expecting a single result

                if (rs.next()) {
                    name = rs.getString(1);
                    // Get data from the current row and use it
                }

            } catch (SQLException ex) {
                log(ex.getMessage(), 3);
            }

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return name;
    }

    public static boolean userExists(Connection conn, String name) {
        String sql = "SELECT COUNT(*) FROM player WHERE name = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Only expecting a single result
                if (rs.next()) {
                    return (rs.getInt(1) != 0);
                }
            } catch (SQLException ex) {
                log(ex.getMessage(), 3);
            }

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return false;
    }
    public static boolean correctPassword(Connection conn, String name, String password) {
        String sql = "SELECT COUNT(*) FROM player WHERE name = ? and password = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Only expecting a single result
                if (rs.next()) {
                    return (rs.getInt(1) != 0);
                }
            } catch (SQLException ex) {
                log(ex.getMessage(), 3);
            }

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return false;
    }


    public static int getPlayerCount(Connection conn) {
        String sql = "SELECT COUNT(*) FROM player";
        int count = 0;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Only expecting a single result

                while (rs.next()) {
                    ++count;
                    // Get data from the current row and use it
                }

            } catch (SQLException ex) {
                log(ex.getMessage(), 3);
            }

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return count;
    }


    public static void updatePassword(Connection conn, CommandSender user, String password, String oldPassword) {
        String sql = "UPDATE player SET password = ? WHERE name = ? AND password = ?";

        if (!correctPassword(conn, user.getName(), oldPassword)) {
            user.sendMessage("Old password was incorrect!");
            return;
        }

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, password);
            pstmt.setString(2, user.getName());
            pstmt.setString(3, oldPassword);
            pstmt.executeUpdate();

            user.sendMessage("Your password has been updated!");

        } catch (SQLException e) {
            user.sendMessage("Your password could not be updated! Please try again later.");
            log(e.getMessage(), 3);
        }
    }
    public static void deleteUser(Connection conn, CommandSender user, String password) {
        String sql = "DELETE FROM player WHERE name = ? AND password = ?";

        if (!correctPassword(conn, user.getName(), password)) {
            user.sendMessage("Password was incorrect!");
            return;
        }

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            user.sendMessage("Your account has been deleted!");
            user.sendMessage("Goodbye " + user.getName());

        } catch (SQLException e) {
            user.sendMessage("Your account could not be deleted! Please try again later.");
            log(e.getMessage(), 3);
        }
    }
}