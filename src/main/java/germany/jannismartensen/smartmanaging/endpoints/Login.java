package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.Cookie;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static germany.jannismartensen.smartmanaging.utility.Util.log;
import static germany.jannismartensen.smartmanaging.utility.Util.redirect;

public class Login implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;
    String playerName = "";

    public Login(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        if (Util.loggedIn(he, connect)) {
            redirect(plugin, he, Util.root());
            return;
        }

        if (he.getRequestMethod().equals("POST")) {
            Map<String, String> map = Util.streamToMap(he.getRequestBody());
            if (!map.containsKey("playername")) {
                template(he, "Please enter your ingame name!", false, "");
                return;
            } else {
                playerName = map.get("playername");
            }

            if (!map.containsKey("password")) {
                template(he, "Please enter a password!", false, map.get("playername"));
                return;
            }

            boolean remember = map.containsKey("remember");

            if (Connect.correctPassword(connect, map.get("playername"), Util.generateHash(map.get("password").trim()))) {
                template(he, "", remember, map.get("playername"));
            } else {
                template(he, "Invalid password", remember, map.get("playername"));
            }


        } else {
            if (he.getRequestURI().getQuery() != null) {
                Map<String, String> params = Util.queryToMap(he.getRequestURI().getQuery());
                template(he, params.getOrDefault("msg", ""), false, "");
            } else {
                template(he, "", false, "");
            }
        }
    }

    public void template (HttpExchange he, String message, boolean remember, String playerName) throws IOException {
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
                // Make Cookie valid for one year
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.YEAR, 1);
                Date nextYear = cal.getTime();


                int t = Math.round(Instant.now().getEpochSecond());

                Cookie cookie;
                ManagingPlayer existingCookie = Connect.getPlayerByName(connect, playerName);
                if (existingCookie == null || !Connect.isCookieValid(connect, existingCookie.getCookie())) {
                    // Send Login Cookie
                    UUID uuid = UUID.randomUUID();
                    UUID uuid2 = UUID.randomUUID();
                    String cookieString = uuid + "-" + uuid2;

                    if (remember) {
                        // Add one year worth of seconds
                        t += 31536000;
                        cookie = new Cookie("login", cookieString, nextYear, null, map.get("ip"), "/", false, false, null);
                    } else {
                        cookie = new Cookie("login", cookieString, null, null, map.get("ip"), "/", false, false, null);
                    }
                    try {
                        Connect.insertCookie(connect, playerName, cookieString, t);
                    } catch (SQLException e) {
                        sqlException(e, message, he, response, map);
                        return;
                    }
                } else {
                    if (remember) {
                        // Add one year worth of seconds
                        t += 31536000;
                        cookie = new Cookie("login", existingCookie.getCookie(), nextYear, null, map.get("ip"), "/", false, false, null);
                    } else {
                        cookie = new Cookie("login", existingCookie.getCookie(), null, null, map.get("ip"), "/", false, false, null);
                    }
                    try {
                        Connect.insertCookie(connect, playerName, existingCookie.getCookie(), t);
                    } catch (SQLException e) {
                        sqlException(e, message, he, response, map);
                        return;
                    }
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

    public void sqlException(Exception e, String message, HttpExchange he, String response, Map<String, String> map) throws IOException {
        log(e, 3);
        log("(Login.template) Could not insert cookie into database", 3, true);

        message = "There has been an error, try again later!";
        Headers headers = he.getResponseHeaders();
        headers.add("Location", "http://" + map.get("ip") + ":" + SmartManaging.port + "/login/?msg=" + message);
        he.sendResponseHeaders(302, response.length());
    }
}
