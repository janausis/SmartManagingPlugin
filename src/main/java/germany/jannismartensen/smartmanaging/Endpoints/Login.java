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
import java.sql.SQLException;
import java.util.*;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class Login implements HttpHandler {

    TemplateEngine engine;
    SmartManaging plugin;
    Connection connect;
    String playerName = "";

    public Login(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed '" + he.getRequestURI() + "': " + he.getRequestMethod());

        if (Util.loggedIn(he, connect, plugin)) {
            Headers headers = he.getResponseHeaders();
            headers.add("Location", "http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            String response = "";
            he.sendResponseHeaders(302, 0);
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        if (he.getRequestMethod().equals("POST")) {
            Map<String, String> map = EndpointUtil.streamToMap(he.getRequestBody());
            if (!map.containsKey("playername")) {
                template(he, "Please enter your ingame name!", false);
                return;
            } else {
                playerName = map.get("playername");
            }

            if (!map.containsKey("password")) {
                template(he, "Please enter a password!", false);
                return;
            }

            boolean remember = map.containsKey("remember");

            if (Connect.correctPassword(connect, map.get("playername"), Util.generateHash(map.get("password").trim()))) {
                template(he, "", remember);
            } else {
                template(he, "Invalid password", remember);
            }


        } else {
            if (he.getRequestURI().getQuery() != null) {
                Map<String, String> params = EndpointUtil.queryToMap(he.getRequestURI().getQuery());
                template(he, params.getOrDefault("msg", ""), false);
            } else {
                template(he, "", false);
            }
        }
    }

    public void template (HttpExchange he, String message, boolean remember) throws IOException {
        Map<String, String> map = new HashMap<>();
        FileConfiguration config = plugin.getConfig();


        map.put("name", config.getString("servername"));
        map.put("message", message);

        map.put("ip", Util.getIpOrDomain(plugin));

        SmartManaging.copyResources("Templates/login.html", plugin, false);
        String response = engine.renderTemplate("login.html", map);

        if (he.getRequestMethod().equals("POST")) {
            if (!message.equals("")) {
                Headers headers = he.getResponseHeaders();
                headers.add("Location", "http://" + map.get("ip") + ":" + SmartManaging.port + "/login/?msg=" + message);
                he.sendResponseHeaders(302, response.length());
            } else {
                // Send Login Cookie
                UUID uuid = UUID.randomUUID();
                UUID uuid2 = UUID.randomUUID();
                String cookieString = uuid + "-" + uuid2;

                // Make Cookie valid for one year
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, 1);
                Date nextYear = cal.getTime();

                Cookie cookie;
                if (remember) {
                    cookie = new Cookie("login", cookieString, nextYear, null, "192.168.1.25", "/", false, false, null);
                } else {
                    cookie = new Cookie("login", cookieString, null, null, "192.168.1.25", "/", false, false, null);
                }
                try {
                    Connect.insertCookie(connect, playerName, cookieString);
                } catch (SQLException e) {
                    log(e.getMessage(), 3);

                    message = "There has been an error, try again later!";
                    Headers headers = he.getResponseHeaders();
                    headers.add("Location", "http://" + map.get("ip") + ":" + SmartManaging.port + "/login/?msg=" + message);
                    he.sendResponseHeaders(302, response.length());
                }

                Headers headers = he.getResponseHeaders();
                headers.add("Set-Cookie", cookie.toString());
                headers.add("Location", "http://" + map.get("ip") + ":" + SmartManaging.port + "/");
                he.sendResponseHeaders(302, response.length());
            }
        } else {
            he.sendResponseHeaders(200, response.length());
        }


        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
