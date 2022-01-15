package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class Root implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public Root(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }
    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);
        template(he);
    }

    public void template (HttpExchange he) throws IOException {
        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        map.put("name", config.getString("servername"));
        map.put("announcement", config.getString("announcements.home"));
        map.put("playercount", String.valueOf(plugin.getServer().getOnlinePlayers().size()) + " / " + plugin.getServer().getMaxPlayers());
        map.put("accountNumber", String.valueOf(Connect.getPlayerCount(connect)));
        map.put("version", plugin.getServer().getBukkitVersion().split("-")[0]);
        map.put("ip", Util.getIpOrDomain(plugin));
        map.put("loggedin", String.valueOf(Util.loggedIn(he, connect)));
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect));

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        SmartManaging.copyResources("Templates/home.html", plugin, false);
        String response = engine.renderTemplate("home.html", map);

        he.sendResponseHeaders(200, response.length());

        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
