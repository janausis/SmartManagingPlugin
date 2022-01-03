package germany.jannismartensen.smartmanaging.Utility.Database;

import germany.jannismartensen.smartmanaging.SmartManaging;

import java.io.File;
import java.sql.*;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class GameModesDatabaseConnector {
    public static Connection connect(SmartManaging plugin, String dbName) {

        Connection conn;
        try {
            // db parameters
            File databaseFile = new File(plugin.getDataFolder(), dbName);
            String url = String.format("jdbc:sqlite:%s", databaseFile);
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            return conn;

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return null;
    }

    public static String getPlayerStat(Connection conn, String tableName, String player, String statName, String storedAs) {
        String sql = "SELECT "+ statName + " FROM " + tableName +" WHERE " + storedAs + " = ?";
        String out = "";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, player);

            try (ResultSet rs = pstmt.executeQuery()) {
               if (rs.next()) {
                    out = rs.getString(1);
                }

            } catch (SQLException ex) {
                log(ex.getMessage(), 3);
            }

        } catch (SQLException e) {
            log(e.getMessage(), 3);
        }
        return out;
    }




}