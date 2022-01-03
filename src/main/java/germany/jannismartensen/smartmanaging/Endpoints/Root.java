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

public class Root implements HttpHandler {

    TemplateEngine engine;
    SmartManaging plugin;
    Connection connect;

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
        map.put("playercount", String.valueOf(plugin.getServer().getOnlinePlayers().size()));
        map.put("accountNumber", String.valueOf(Connect.getPlayerCount(connect)));
        map.put("version", plugin.getServer().getBukkitVersion());
        map.put("ip", Util.getIpOrDomain(plugin));
        map.put("loggedin", String.valueOf(Util.loggedIn(he, connect, plugin)));

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect, plugin), he);

        SmartManaging.copyResources("Templates/home.html", plugin, false);
        String response = engine.renderTemplate("home.html", map);

        he.sendResponseHeaders(200, response.length());

        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
