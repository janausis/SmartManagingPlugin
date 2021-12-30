package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class ServeFile implements HttpHandler {

    SmartManaging plugin;
    String filepath;

    public ServeFile(SmartManaging m, String path) {
        this.plugin = m;
        this.filepath = path;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed '" + he.getRequestURI() + "': " + he.getRequestMethod());

        SmartManaging.copyResources(filepath, plugin, false);

        File file = new File(plugin.getDataFolder() + "/" + filepath);
        he.sendResponseHeaders(200, file.length());

        OutputStream outputStream = he.getResponseBody();
        Files.copy(file.toPath(), outputStream);
        outputStream.close();

    }
}