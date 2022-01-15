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

public class ServerMap implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public ServerMap(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);

        Headers headers = he.getResponseHeaders();
        String d = plugin.getConfig().getString("dynmap");
        if (d == null) {
            headers.add("Location", Util.root());
        } else {
            headers.add("Location", d);
        }

        String response = "";
        he.sendResponseHeaders(302, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }
}
