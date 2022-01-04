package germany.jannismartensen.smartmanaging;

import com.sun.net.httpserver.HttpServer;
import germany.jannismartensen.smartmanaging.Endpoints.*;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import germany.jannismartensen.smartmanaging.Utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.Utility.TabCompleter;
import germany.jannismartensen.smartmanaging.Utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.Utility.TestDataGenerator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.Objects;

import static germany.jannismartensen.smartmanaging.Utility.Util.*;

public class SmartManaging extends JavaPlugin {

    HttpServer server;
    TemplateEngine engine;
    Connection Database;
    public boolean serverRunning = false;
    public static int port = 9000;


    @Override
    public void onEnable() {
        createSourceFolder("images");
        createSourceFolder("style");
        createSourceFolder("Templates");
        this.saveDefaultConfig();

        Database = Connect.connect(this);
        engine = new TemplateEngine(this);
        Objects.requireNonNull(getCommand("managing")).setTabCompleter(new TabCompleter());

        FileConfiguration config = getConfig();
        if (config.contains("port")) {
            port = config.getInt("port");
        }

        //startServer();
    }

    @Override
    public void onDisable() {
        stopServer();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender,
                             Command command,
                             @NonNull String label,
                             @NonNull String[] args) {
        if (command.getName().equalsIgnoreCase("managing")) {

            if (args != null) {
                if (args[0].equalsIgnoreCase("server")) {
                    if (args[1].equalsIgnoreCase("start")) {
                        startServer(sender);

                    } else if (args[1].equalsIgnoreCase("stop")) {
                        stopServer(sender);

                    } else if (args[1].equalsIgnoreCase("generateTestData")) {
                        Player p = sender.getServer().getPlayer(sender.getName());

                        assert p != null;
                        TestDataGenerator.generate(this, new ManagingPlayer( p.getName(), p.getUniqueId().toString(), null, null));

                    } else if (args[1].equalsIgnoreCase("deleteTestData")) {
                        new File(getDataFolder(), "testdata.db").delete();
                    } else {
                        sender.sendMessage("Unknown action");
                    }
                }

                else if (args[0].equalsIgnoreCase("register")) {
                    if (Connect.userExists(Database, sender.getName())) {sender.sendMessage("You are already registered!"); return true;}
                    if (args.length <= 1) { sender.sendMessage("Please provide a password!"); return true;}
                    registerUser(getServer().getIp(), Database, sender, args[1]);

                } else if (args[0].equalsIgnoreCase("unregister")) {
                    if (!Connect.userExists(Database, sender.getName())) {sender.sendMessage("You are not registered!"); return true;}
                    if (args.length <= 1) { sender.sendMessage("Please provide a password!"); return true;}
                    deleteUser(Database, sender, args[1]);

                } else if (args[0].equalsIgnoreCase("changepassword")) {
                    if (!Connect.userExists(Database, sender.getName())) {sender.sendMessage("You are not registered!"); return true;}
                    if (args.length <= 1) {
                        sender.sendMessage("Please provide the old password!");
                        return true;
                    }
                    if (args.length <= 2) {
                        sender.sendMessage("Please provide the new password as well!");
                        return true;
                    }
                    changeUserPassword(Database, sender, args[2], args[1]);
                }




                else {
                    sender.sendMessage("Unknown action");
                }


            } else {
                sender.sendMessage("syntax: managing <action>");
            }


            return true;
        }
        return false;
    }

    public void start () throws IOException {
        serverRunning = true;

        int port = 9000;
        server = HttpServer.create(new InetSocketAddress(port), 5);
        server.createContext("/", new Root(engine, this, Database));
        server.createContext("/login", new Login(engine, this, Database));
        server.createContext("/logout", new Logout(engine, this, Database));
        server.createContext("/profile", new Profile(engine, this, Database));
        server.createContext("/favicon.ico", new ServeFile(this, "favicon.ico"));
        server.createContext("/robots.txt", new ServeFile(this, "robots.txt"));
        server.createContext("/style", new FileServer(this, "/style"));
        server.createContext("/images", new FileServer(this, "/images"));
        server.createContext("/images/modes", new FileServer(this, "/images/modes"));
        server.setExecutor(null);
        server.start();

    }

    public void startServer (){
        if (serverRunning) return;
        try {
            start();
            log("Server started at port " + port, 0, true);
        } catch (IOException e) {
            log(e, 3);
            log("Server could not start at port " + port, 3, true);
        }
    }

    public void startServer (CommandSender sender) {
        if (serverRunning) return;
        try {
            start();
            log("Server started at port " + port, 0, true);
            if (!sender.getName().equals("CONSOLE")) sender.sendMessage("Server started at port " + port);
        } catch (IOException e) {
            log(e, 3);
            log("Server could not start at port " + port, 3, true);
            if (!sender.getName().equals("CONSOLE")) sender.sendMessage("Server could not start at port " + port);
        }
    }

    public void stopServer () {
        if (serverRunning) {
            serverRunning = false;
            server.stop(0);
            log("Server stopped at port " + port, 0, true);
        }
    }

    public void stopServer (CommandSender sender) {
        if (serverRunning) {
            serverRunning = false;
            server.stop(0);
            log("Server stopped at port " + port, 0, true);
            if (!sender.getName().equals("CONSOLE")) sender.sendMessage("Server stopped at port " + port);
        }
    }

    public static void copyResources(String path, SmartManaging m, boolean folder) {
        File file = new File(m.getDataFolder() + "/" + path);

        if (!file.exists()) {
            if (!folder) {

                m.saveResource(String.valueOf(path), true);
            } else {
                file.mkdirs();
            }
        }
    }

    public void createSourceFolder (String folder) {
        File a = new File(getDataFolder() + "/" + folder);
        if (!a.exists()) {
            log("Created source folder '" + folder + "'");
            a.mkdirs();
        }
    }

}
