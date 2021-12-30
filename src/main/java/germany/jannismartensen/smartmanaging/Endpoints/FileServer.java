package germany.jannismartensen.smartmanaging.Endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

import static germany.jannismartensen.smartmanaging.Utility.Util.log;

public class FileServer implements HttpHandler {

    SmartManaging plugin;
    String filepath;

    public FileServer(SmartManaging m, String path) {
        this.plugin = m;
        this.filepath = path;
    }

    public void handle(HttpExchange ex) throws IOException {
        log(ex.getRemoteAddress().toString().replace("/", "") + " accessed '" + ex.getRequestURI() + "': " + ex.getRequestMethod());


        URI uri = ex.getRequestURI();
        String name = new File(uri.getPath()).getName();
        File path = new File(plugin.getDataFolder() + filepath, name);

        try {
            SmartManaging.copyResources(filepath.substring(1) + "/" + name, plugin, false);

        } catch (Exception e) {
            e.printStackTrace();
        }

        OutputStream out = ex.getResponseBody();

        if (path.exists()) {
            ex.sendResponseHeaders(200, path.length());
            out.write(Files.readAllBytes(path.toPath()));
        } else {
            System.err.println("File not found: " + path.getAbsolutePath());

            ex.sendResponseHeaders(404, 0);
            out.write("FILE NOT FOUND".getBytes());
        }

        out.close();
    }
}