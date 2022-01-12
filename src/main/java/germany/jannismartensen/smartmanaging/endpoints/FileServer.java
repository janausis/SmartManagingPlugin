package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.Util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

import static germany.jannismartensen.smartmanaging.utility.Util.log;

public class FileServer implements HttpHandler {

    final SmartManaging plugin;
    final String filepath;
    boolean renders = false;

    public FileServer(SmartManaging m, String path) {
        this.plugin = m;
        this.filepath = path;
    }

    public FileServer(SmartManaging m, String path, boolean renders) {
        this.plugin = m;
        this.filepath = path;
        this.renders = renders;
    }

    public void handle(HttpExchange ex) throws IOException {
        Util.logAccess(ex);


        URI uri = ex.getRequestURI();
        String name = new File(uri.getPath()).getName();
        File path = new File(plugin.getDataFolder() + filepath, name);

        try {
            SmartManaging.copyResources(filepath.substring(1) + "/" + name, plugin, false);

        } catch (Exception e) {
            if (renders) {
                path = new File(plugin.getDataFolder() + "/renders/unknown.png");
            } else {
                log("The file " + name + " could not be found!",3);
            }
        }

        OutputStream out = ex.getResponseBody();

        if (path.exists()) {
            ex.sendResponseHeaders(200, path.length());
            out.write(Files.readAllBytes(path.toPath()));
        } else {
            log("File not found: " + path.getAbsolutePath(), 3);

            ex.sendResponseHeaders(404, 0);
            out.write("FILE NOT FOUND".getBytes());
        }

        out.close();
    }
}