package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import germany.jannismartensen.smartmanaging.Utility.TemplateEngine;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

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
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed '" + he.getRequestURI() + "': " + he.getRequestMethod());

        template(he);
    }

    public void template (HttpExchange he) throws IOException {
        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();


        map.put("name", config.getString("servername"));
        map.put("announcement", config.getString("announcement"));
        map.put("playercount", String.valueOf(plugin.getServer().getOnlinePlayers().size()));
        map.put("accountNumber", String.valueOf(Connect.getPlayerCount(connect)));
        map.put("version", plugin.getServer().getBukkitVersion());

        String domain = config.getString("domain");
        map.put("ip", plugin.getServer().getIp());
        if (domain != null) {
            if (!domain.isEmpty()) {
                map.put("ip", config.getString("domain"));
            }
        }


        SmartManaging.copyResources("Templates/home.html", plugin, false);
        String response = engine.renderTemplate("home.html", map);

        he.sendResponseHeaders(200, response.length());


        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
