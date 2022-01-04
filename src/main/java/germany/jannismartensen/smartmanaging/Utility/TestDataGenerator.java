package germany.jannismartensen.smartmanaging.Utility;

import germany.jannismartensen.smartmanaging.SmartManaging;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class TestDataGenerator {

    public static void generate(SmartManaging plugin, ManagingPlayer player) {

        Connection conn;
        try {



            FileConfiguration config = plugin.getConfig();
            ConfigurationSection section = config.getConfigurationSection("modes");

            ArrayList<String> modes = new ArrayList<>();

            for (String entry : Objects.requireNonNull(section).getKeys(true)) if (!entry.contains(".")) modes.add(entry);


            // Iterate over all modes
            for (String mode : modes) {
                StringBuilder sql = new StringBuilder();

                // Read the need info from the config
                ConfigurationSection modeSection = config.getConfigurationSection("modes." + mode);

                // Get Database info
                String dbPath = Objects.requireNonNull(modeSection).getString("DatabaseFile");
                String tableName = Objects.requireNonNull(modeSection).getString("TableName");
                String playerStoredAs = Objects.requireNonNull(modeSection).getString("StoredAs");
                String playerStoreName = Objects.requireNonNull(modeSection).getString("StoredInColumn");

                ConfigurationSection valuesModeSection = config.getConfigurationSection("modes." + mode + ".values");

                ArrayList<String> values = new ArrayList<>(Objects.requireNonNull(valuesModeSection).getKeys(true));
                values.add(0, playerStoreName);

                // Create a table for each mode
                sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
                for (String value : values) {
                    if (value.equals(values.get(values.size()-1))) {
                        sql.append("'").append(value).append("' TEXT");
                    } else {
                        sql.append("'").append(value).append("' TEXT, ");
                    }
                }
                sql.append(")");




                // db parameters
                File databaseFile = new File(plugin.getDataFolder(), Objects.requireNonNull(dbPath));
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                if (!databaseFile.exists()) {
                    databaseFile.createNewFile();
                } else {
                    File databaseOldFile = new File(plugin.getDataFolder(), dbPath);
                    databaseOldFile.delete();
                    databaseFile.createNewFile();
                }

                String url = String.format("jdbc:sqlite:%s", databaseFile);
                // create a connection to the database
                conn = DriverManager.getConnection(url);


                try {
                    Statement stmt = conn.createStatement();
                    // create a new table
                    stmt.execute(sql.toString());
                } catch (SQLException e) {
                    log(e, 3);
                    log("(TestDataGenerator.generate) SqlException whilst creating test database " + databaseFile.getPath(), 3, true);
                }

                StringBuilder columns = new StringBuilder();
                StringBuilder marks = new StringBuilder();

                for (String value : values) {
                    if (value.equals(values.get(values.size()-1))) {
                        columns.append(value);
                        marks.append("?");
                    } else {
                        columns.append(value).append(", ");
                        marks.append("?").append(", ");
                    }
                }

                String playerName;
                if (Objects.equals(playerStoredAs, "uuid")) {
                    playerName = player.getUUID();
                } else {
                    playerName = player.getName();
                }

                String create = "INSERT INTO " + tableName + "(" + columns + ") VALUES(" + marks + ")";

                try {
                    int max = 1000000;
                    int min = 0;
                    PreparedStatement pstmt = conn.prepareStatement(create);
                    for (int i = 0; i < values.size(); i++) {
                        if (values.get(i).equals(playerStoreName)) {
                            pstmt.setString(i + 1, playerName);
                        } else {
                            pstmt.setString(i + 1, String.valueOf((int) (Math.random() * (max - min + 1) + min)));
                        }
                    }

                    pstmt.executeUpdate();



                } catch (SQLException e) {
                    log(e, 3);
                    log("(TestDataGenerator.generate) SqlException inserting into test database " + databaseFile.getPath(), 3, true);
                }

                conn.close();
            }

            log("Creation of Database successful");

        } catch (SQLException e) {
            log(e, 3);
            log("(TestDataGenerator.generate) SqlException whilst handling test databases", 3, true);
        } catch (IOException e) {
            log(e, 3);
            log("(TestDataGenerator.generate) Could not open a test database file", 3, true);
        }
    }

}
