package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import static germany.jannismartensen.smartmanaging.utility.Util.redirect;


// Get 5 Player suggestions
public class PlayerSearch implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public PlayerSearch(TemplateEngine e, SmartManaging m, Connection c) {
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

        Map<String, String> params = Util.queryToMap(he.getRequestURI().getQuery());

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);
        ArrayList<String> suggestions = Connect.getTopSuggestions(connect, params.getOrDefault("search", ""), 5, "false");

        JSONArray jsArray = new JSONArray();
        jsArray.addAll(suggestions);

        JSONObject data = new JSONObject();
        data.put("data", jsArray);
        String response = data.toJSONString();

        he.sendResponseHeaders(200, response.length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }


}
