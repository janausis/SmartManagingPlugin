package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import germany.jannismartensen.smartmanaging.Utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.Utility.Util;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

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
            Headers headers = Util.deleteInvalidCookies(false, he);
            headers.add("Location", "http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            String response = "";
            he.sendResponseHeaders(302, 0);
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        String username = Connect.getPlayerFromCookie(connect, Util.getCookie(he));

        map.put("username", username);
        map.put("announcement", config.getString("announcement"));
        map.put("playtime", "NOT FOUND!");

        // Generate modes list
        if (config.contains("modes")) {
            String s = "['" + config.getString("modes").replace(" ", "").replace(",", "','") + "']";
            map.put("modes", s);
        } else {
            map.put("modes", "[]");
        }

        map.put("loggedin", String.valueOf(Util.loggedIn(he, connect, plugin)));

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect, plugin), he);

        SmartManaging.copyResources("Templates/profile.html", plugin, false);
        String response = engine.renderTemplate("profile.html", map);

        he.sendResponseHeaders(302, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
