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

public class Admin implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public Admin(TemplateEngine e, SmartManaging m, Connection c) {
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
        if (user == null || !Connect.getManagerStatus(connect, user.getUUID())) {
            redirect(plugin, he, Util.root());
            return;
        }

        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();


        map.put("ip", "'http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "'");
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect), true);

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        // SmartManaging.copyResources("Templates/admin.html", plugin, false);
        // String response = engine.renderTemplate("admin.html", map);
        String response = "Yay, you have web admin rights!";

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
