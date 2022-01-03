package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import germany.jannismartensen.smartmanaging.Utility.Database.GameModesDatabaseConnector;
import germany.jannismartensen.smartmanaging.Utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.Utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.Utility.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static germany.jannismartensen.smartmanaging.Utility.Util.redirect;

public class Profile implements HttpHandler {

    TemplateEngine engine;
    SmartManaging plugin;
    Connection connect;
    String playerName = "";

    public Profile(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        if (!Util.loggedIn(he, connect, plugin)) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            return;
        }

        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        ManagingPlayer user = Connect.getPlayerFromCookie(connect, Util.getCookie(he));
        if (user == null) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/logout");
            return;
        } else if (user.getUUID() == null || user.getName() == null || user.getCookie() == null || user.getPassword() == null) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/logout");
            return;
        }


        map.put("username", user.getName());
        map.put("announcement", config.getString("announcements.profile"));
        map.put("playtime", Util.getPlayTime(user));


        // Populate modeScores
        map = populateModes(map, user);
        map.put("loggedin", String.valueOf(Util.loggedIn(he, connect, plugin)));



        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect, plugin), he);

        SmartManaging.copyResources("Templates/profile.html", plugin, false);
        String response = engine.renderTemplate("profile.html", map);

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    public Map<String, String> populateModes(Map<String, String> map, ManagingPlayer user) {
        Map<String, String> outMap = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("modes");

        ArrayList<String> modes = new ArrayList<>();
        StringBuilder modeString = new StringBuilder("[");

        for (String entry : Objects.requireNonNull(section).getKeys(true)) if (!entry.contains(".")) modes.add(entry);
        for (String e : modes) modeString.append('\"').append(e.replace(" ", "")).append("\",");

        modeString.append("]");

        map.put("modes", modeString.toString());


        ArrayList<ArrayList<String>> scoreList = new ArrayList<>();
        ArrayList<ArrayList<String>> valueList = new ArrayList<>();


        for (String mode : modes) {
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
            String playerName = "";
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

        map.put("scoreList", Util.getStringFromArray(scoreList));
        map.put("valueList", Util.getStringFromArray(valueList));

        return map;
    }




}
