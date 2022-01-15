package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import germany.jannismartensen.smartmanaging.utility.database.Connect;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;

import static germany.jannismartensen.smartmanaging.utility.Util.log;
import static germany.jannismartensen.smartmanaging.utility.Util.redirect;

public class PlayerSearchRandom implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;

    public PlayerSearchRandom(TemplateEngine e, SmartManaging m, Connection c) {
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
        if (user == null) {
            redirect(plugin, he, Util.root());
            return;
        }

        try {
            Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);
            ArrayList<String> allPlayers = Connect.getTopSuggestions(connect, "", 0, "false");
            int index = (int)(Math.random() * ((allPlayers.size() - 1) + 1));

            redirect(plugin, he, Util.root() + "players/results?playername=" + allPlayers.get(index) + "&exactMatch=true");
        } catch (Exception e) {
            log(e, 3);
            log("PlayerSearchRandom.handle) There was an unexpected error whilst getting random player");
            redirect(plugin, he, Util.root() + "players");
        }
    }
}
