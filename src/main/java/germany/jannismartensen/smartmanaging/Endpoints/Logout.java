package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.Utility.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class Logout implements HttpHandler {

    TemplateEngine engine;
    SmartManaging plugin;
    Connection connect;
    String playerName = "";

    public Logout(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed '" + he.getRequestURI() + "': " + he.getRequestMethod());

        Headers headers = he.getResponseHeaders();
        headers.add("Location", "http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");

        if (Util.loggedIn(he, connect, plugin)) {
            // Remove login cookie

            // Make Cookie invalid by setting before current time
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -15);
            Date nextYear = cal.getTime();
            Cookie cookie = new Cookie("login", "", nextYear, null, "192.168.1.25", "/", false, false, null);
            headers.add("Set-Cookie", cookie.toString());
        }

        String response = "";
        he.sendResponseHeaders(302, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
