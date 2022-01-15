package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
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

public class PlayerSearchResults implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public PlayerSearchResults(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        if (!Util.loggedIn(he, connect)) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            return;
        }

        ManagingPlayer user = Util.getUser(connect, he, plugin);
        if (user == null) {
            return;
        }

        Map<String, String> map = new HashMap<>();
        map.put("ip", "'http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "'");

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);



        if (he.getRequestURI().getQuery() == null) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/players");
            return;
        }
        Map<String, String> params = Util.queryToMap(he.getRequestURI().getQuery());
        String query = params.getOrDefault("playername", "");
        if (query.equals("")) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/players");
            return;
        }

        ArrayList<String> suggestions = Connect.getTopSuggestions(connect, query, 0, params.getOrDefault("exactMatch", "false"));

        map = getData(map, suggestions);
        map.put("query", query);
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect));

        SmartManaging.copyResources("Templates/playersResult.html", plugin, false);
        String response = engine.renderTemplate("playersResult.html", map);

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    public Map<String, String> getData(Map<String, String> map, ArrayList<String> playerList) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("playerSearch.stats");

        StringBuilder playerString = new StringBuilder("[");
        for (String e : playerList) playerString.append("\"").append(e).append("\",");
        playerString.deleteCharAt(playerString.length() - 1);
        playerString.append("]");
        map.put("playerList", playerString.toString());

        ArrayList<ArrayList<ArrayList<String>>> data = new ArrayList<>();

        for (String player : playerList) {
            ArrayList<ArrayList<String>> playersList = new ArrayList<>();
            for (String stat : Objects.requireNonNull(section).getKeys(true)) {
                ArrayList<String> statList = new ArrayList<>();
                statList.add(stat + ": ");

                ManagingPlayer user = Connect.getPlayerByName(connect, player);

                int tmpScore = 0;
                for (String world: Util.getWorldList(plugin, "playerSearch")) {
                    try {
                        tmpScore += Integer.parseInt(Util.readStats(user, world, Objects.requireNonNull(section.getString(stat))));
                    } catch (NumberFormatException e) {
                        log(e, 3);
                        log("(PlayerSearchResults.getData) Could not format score into integer " + Util.readStats(user, world, Objects.requireNonNull(section.getString(stat))), 3, true);
                    } catch (NullPointerException r) {
                        log(r, 3);
                        log("(PlayerSearchResults.getData) You might be missing the exact definition of the stat " + stat + " for user " + Objects.requireNonNull(user).getName(), 3, true);
                    }
                }
                statList.add(String.valueOf(tmpScore));
                playersList.add(statList);

            }
            data.add(playersList);
        }

        if (data.isEmpty()) {
            map.put("dataList", "[]");
            return map;
        }

        String outString = Util.getStringFromArray3(data);
        map.put("dataList", outString);
        return map;
    }
}
