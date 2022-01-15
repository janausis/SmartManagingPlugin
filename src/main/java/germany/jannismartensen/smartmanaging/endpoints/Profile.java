package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import germany.jannismartensen.smartmanaging.utility.database.GameModesDatabaseConnector;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static germany.jannismartensen.smartmanaging.utility.Util.log;
import static germany.jannismartensen.smartmanaging.utility.Util.redirect;

public class Profile implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;
    String playerName = "";

    public Profile(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        if (!Util.loggedIn(he, connect)) {
            redirect(plugin, he, Util.root());
            return;
        }

        ManagingPlayer user = Util.getUser(connect, he, plugin);
        if (user == null) {
            redirect(plugin, he, Util.root());
            return;
        }

        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        map.put("username", user.getName());
        map.put("announcement", config.getString("announcements.profile"));
        map.put("playtime", Util.getPlayTime(plugin, user));
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect));


        // Populate modeScores
        map = populateModes(map, user);

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        SmartManaging.copyResources("Templates/profile.html", plugin, false);
        String response = engine.renderTemplate("profile.html", map);

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    public Map<String, String> populateModes(Map<String, String> map, ManagingPlayer user) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("modes");

        ArrayList<String> modes = new ArrayList<>();
        StringBuilder modeString = new StringBuilder("[");

        // Check if minecraft stats should be displayed
        ConfigurationSection stats = config.getConfigurationSection("stats");
        for (String entry : Objects.requireNonNull(stats).getKeys(true)) if (!entry.contains(".")) modes.add(entry);

        // Add modes
        for (String entry : Objects.requireNonNull(section).getKeys(true)) if (!entry.contains(".")) modes.add(entry);
        for (String e : modes) modeString.append('\"').append(e).append("\",");

        modeString.append("]");

        map.put("modes", modeString.toString());


        // Init arrays
        ArrayList<ArrayList<String>> scoreList = new ArrayList<>();
        ArrayList<ArrayList<String>> valueList = new ArrayList<>();

        // Set values for each mode
        for (String mode : modes) {
            // Special for minecraft stats
            if (stats.contains(mode)) {
                ArrayList<ArrayList<String>> out = readStats(user, mode);
                scoreList.add(out.get(1));
                valueList.add(out.get(0));
                continue;
            }

            ArrayList<String> tmpScoreList = new ArrayList<>();
            ArrayList<String> tmpValueList = new ArrayList<>();

            ConfigurationSection modeSection = config.getConfigurationSection("modes." + mode);

            // Get Database info
            String dbPath = Objects.requireNonNull(modeSection).getString("DatabaseFile");
            String tableName = Objects.requireNonNull(modeSection).getString("TableName");
            String playerStoredAs = Objects.requireNonNull(modeSection).getString("StoredAs");
            String playerStoreName = Objects.requireNonNull(modeSection).getString("StoredInColumn");

            ConfigurationSection valuesModeSection = config.getConfigurationSection("modes." + mode + ".values");

            ArrayList<String> values = new ArrayList<>(Objects.requireNonNull(valuesModeSection).getKeys(true));
            values.add(0, playerStoreName);



            Connection conn = GameModesDatabaseConnector.connect(plugin, dbPath);
            String playerName;
            if (Objects.equals(playerStoredAs, "uuid")) {
                playerName = user.getUUID();
            } else {
                playerName = user.getName();
            }

            for (int i = 1; i < values.size(); i++) {
                String value = values.get(i);

                if (!value.equals(playerStoreName)) {
                    String out = GameModesDatabaseConnector.getPlayerStat(Objects.requireNonNull(conn), tableName, playerName, value, playerStoreName);
                    tmpValueList.add(out);
                    tmpScoreList.add(value + ": ");
                }
            }

            scoreList.add(tmpScoreList);
            valueList.add(tmpValueList);
        }

        map.put("scoreList", Util.getStringFromArray2(scoreList));
        map.put("valueList", Util.getStringFromArray2(valueList));

        return map;
    }

    public ArrayList<ArrayList<String>> readStats(ManagingPlayer user, String mode) {


        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("stats." + mode + ".stats");

        ArrayList<String> scores = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        ArrayList<String> stats = new ArrayList<>();
        for (String entry : Objects.requireNonNull(section).getKeys(true)) if (!entry.contains(".")) stats.add(entry);

        for (String stat : stats) {

            int tmpScore = 0;
            for (String world: Util.getWorldList(plugin, "stats." + mode)) {
                try {
                    tmpScore += Integer.parseInt(Util.readStats(user, world, Objects.requireNonNull(section.getString(stat))));

                } catch (NumberFormatException e) {
                    log(e, 3);
                    log("(Profile.readStats) Could not format score into integer " + Util.readStats(user, world, Objects.requireNonNull(section.getString(stat))), 3, true);
                } catch (NullPointerException r) {
                    log(r, 3);
                    log("(Profile.readStats) You might be missing the exact definition of the stat " + stat + " for mode " + mode + " in your config", 3, true);
                }
            }

            if (Objects.requireNonNull(section.getString(stat)).contains("time")) {
                scores.add(Util.tickBeautifier(String.valueOf(tmpScore)));
            } else if (Objects.requireNonNull(section.getString(stat)).contains("cm")) {
                scores.add(String.valueOf((double)tmpScore/100.0));
            } else {
                scores.add(String.valueOf(tmpScore));
            }

            values.add(stat + ": ");
        }


        ArrayList<ArrayList<String>> out = new ArrayList<>();
        out.add(scores);
        out.add(values);

        return out;
    }
}
