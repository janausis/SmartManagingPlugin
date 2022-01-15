package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static germany.jannismartensen.smartmanaging.utility.Util.redirect;

public class Players implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public Players(TemplateEngine e, SmartManaging m, Connection c) {
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
            redirect(plugin, he, Util.root());
            return;
        }

        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();

        map.put("ip", "'http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "'");
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect), Connect.getManagerStatus(connect, user.getUUID()));

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        SmartManaging.copyResources("Templates/players.html", plugin, false);
        String response = engine.renderTemplate("players.html", map);

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
