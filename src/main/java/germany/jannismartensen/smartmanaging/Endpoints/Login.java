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

public class Login implements HttpHandler {

    TemplateEngine engine;
    SmartManaging plugin;
    Connection connect;

    public Login(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed '" + he.getRequestURI() + "': " + he.getRequestMethod());


        if (he.getRequestMethod().equals("POST")) {
            Map<String, String> map = EndpointUtil.streamToMap(he.getRequestBody());
            if (!map.containsKey("playername")) {
                template(he, "Please enter your ingame name!");
                return;
            }

            if (!map.containsKey("password")) {
                template(he, "Please enter a password!");
                return;
            }

            if (Connect.correctPassword(connect, map.get("playername"), Util.generateHash(map.get("password").trim()))) {
                template(he, "Valid password");
            } else {
                template(he, "Invalid password");
            }


        } else {
            if (he.getRequestURI().getQuery() != null) {
                Map<String, String> params = EndpointUtil.queryToMap(he.getRequestURI().getQuery());
                template(he, params.getOrDefault("msg", ""));
            } else {
                template(he, "");
            }
        }
    }

    public void template (HttpExchange he, String message) throws IOException {
        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();


        map.put("name", config.getString("servername"));
        map.put("message", message);

        String domain = config.getString("domain");
        map.put("ip", plugin.getServer().getIp());
        if (domain != null) {
            if (!domain.isEmpty()) {
                map.put("ip", config.getString("domain"));
            }
        }



        SmartManaging.copyResources("Templates/login.html", plugin, false);
        String response = engine.renderTemplate("login.html", map);

        if (he.getRequestMethod().equals("POST")) {
            Headers headers = he.getResponseHeaders();
            headers.add("Location", "http://" + map.get("ip") + ":" + SmartManaging.port + "/?msg=" + message);
            he.sendResponseHeaders(302, response.length());
        } else {
            he.sendResponseHeaders(200, response.length());
        }


        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
