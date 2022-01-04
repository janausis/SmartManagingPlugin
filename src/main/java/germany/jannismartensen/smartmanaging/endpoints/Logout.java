package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;

public class Logout implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;
    String playerName = "";

    public Logout(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        Headers headers = he.getResponseHeaders();
        headers.add("Location", "http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");

        if (Util.loggedIn(he, connect)) {
            // Remove login cookie
            headers.add("Set-Cookie", Util.invalidCookie().toString());
        }

        String response = "";
        he.sendResponseHeaders(302, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
