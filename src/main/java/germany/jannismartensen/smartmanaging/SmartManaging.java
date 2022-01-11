package germany.jannismartensen.smartmanaging;

import com.jogamp.opengl.GLCapabilities;
import com.sun.net.httpserver.HttpServer;
import germany.jannismartensen.smartmanaging.endpoints.*;
import germany.jannismartensen.smartmanaging.utility.*;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.Objects;
import java.util.UUID;

import static germany.jannismartensen.smartmanaging.utility.Util.*;

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
        createSourceFolder("scripts");
        createSourceFolder("Templates");
        this.saveDefaultConfig();

        // Convert old log file to zip
        zipLog(this, "");
        zipLog(this, "access/");

        Objects.requireNonNull(getCommand("managing")).setTabCompleter(new TabCompleter());
        if (!Util.getLogStatus(this, "logLocation").equals("console")) {
            logToFile("Activating Plugin...", 0, this, "");
        }
        setPort();

        Database = Connect.connect(this);
        engine = new TemplateEngine(this);

        startServer();
    }

    @Override
    public void onDisable() {
        stopServer();

        if (!Util.getLogStatus(this, "logLocation").equals("console")) {
            logToFile("Deactivating Plugin... \n\n\n", 0, this, "");
        }
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender,
                             Command command,
                             @NonNull String label,
                             @NonNull String[] args) {
        if (command.getName().equalsIgnoreCase("managing")) {
            if (sender.hasPermission("managing.serveruser") || sender.hasPermission("managing.servermanager")) {

                if (args != null) {
                    if (args[0].equalsIgnoreCase("server")) {
                        if (!sender.hasPermission("managing.servermanager")) {
                            sender.sendMessage(ChatColor.RED + "You do not have the permission managing.servermanager to use this command!" + ChatColor.RESET);
                            return true;
                        }


                        if (args[1].equalsIgnoreCase("start")) {
                            startServer(sender);

                        } else if (args[1].equalsIgnoreCase("stop")) {
                            stopServer(sender);

                        } else if (args[1].equalsIgnoreCase("generateTestData")) {
                            if (args.length <= 3) {
                                TestDataGenerator.generate(this, new ManagingPlayer(args[2], UUID.randomUUID().toString(), null, null));
                            } else {
                                Player p = sender.getServer().getPlayer(sender.getName());

                                assert p != null;
                                TestDataGenerator.generate(this, new ManagingPlayer(p.getName(), p.getUniqueId().toString(), null, null));
                            }

                        } else if (args[1].equalsIgnoreCase("reload")) {
                            reloadConfig();
                            log("Reloaded SmartManaging config!", 1, true);
                            if (!sender.getName().equals("CONSOLE"))
                                sender.sendMessage(ChatColor.GREEN + "Reloaded SmartManaging config!");

                            if (serverRunning) {
                                stopServer(sender);
                                setPort();
                                startServer(sender);
                            } else {
                                setPort();
                            }

                        } else {
                            sender.sendMessage("Unknown action");
                        }
                    } else if (args[0].equalsIgnoreCase("register")) {
                        if (Connect.userExists(Database, sender.getName())) {
                            sender.sendMessage(ChatColor.RED + "You are already registered!");
                            return true;
                        }
                        if (args.length <= 1) {
                            sender.sendMessage(ChatColor.RED + "Please provide a password!");
                            return true;
                        }
                        registerUser(getServer().getIp(), Database, sender, args[1]);

                    } else if (args[0].equalsIgnoreCase("unregister")) {
                        if (!Connect.userExists(Database, sender.getName())) {
                            sender.sendMessage(ChatColor.RED + "You are not registered!");
                            return true;
                        }
                        if (args.length <= 1) {
                            sender.sendMessage(ChatColor.RED + "Please provide a password!");
                            return true;
                        }
                        deleteUser(Database, sender, args[1]);

                    } else if (args[0].equalsIgnoreCase("changepassword")) {
                        if (!Connect.userExists(Database, sender.getName())) {
                            sender.sendMessage(ChatColor.RED + "You are not registered!");
                            return true;
                        }
                        if (args.length <= 1) {
                            sender.sendMessage(ChatColor.RED + "Please provide the old password!");
                            return true;
                        }
                        if (args.length <= 2) {
                            sender.sendMessage(ChatColor.RED + "Please provide the new password as well!");
                            return true;
                        }
                        changeUserPassword(Database, sender, args[2], args[1]);
                    } else {
                        sender.sendMessage("Unknown action");
                    }


                } else {
                    sender.sendMessage(ChatColor.RED + "syntax: managing <action>");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have the permission managing.serveruser to use this command!" + ChatColor.RESET);
            }

            return true;
        }
        return false;
    }

    public void start () throws IOException {

        //
        serverRunning = true;


        server = HttpServer.create(new InetSocketAddress(port), 5);
        // Routes
        server.createContext("/", new Root(engine, this, Database));
        server.createContext("/login", new Login(engine, this, Database));
        server.createContext("/logout", new Logout(engine, this, Database));
        server.createContext("/profile", new Profile(engine, this, Database));
        server.createContext("/players", new Players(engine, this, Database));
        server.createContext("/players/search", new PlayerSearch(engine, this, Database));
        server.createContext("/players/results", new PlayerSearchResults(engine, this, Database));
        server.createContext("/players/random", new PlayerSearchRandom(engine, this, Database));
        server.createContext("/inventory", new Inventory(engine, this, Database));

        // Static Files
        server.createContext("/favicon.ico", new ServeFile(this, "favicon.ico"));
        server.createContext("/robots.txt", new ServeFile(this, "robots.txt"));
        server.createContext("/style", new FileServer(this, "/style"));
        server.createContext("/images", new FileServer(this, "/images"));
        server.createContext("/scripts", new FileServer(this, "/scripts"));
        server.createContext("/images/modes", new FileServer(this, "/images/modes"));
        server.setExecutor(null);
        server.start();

    }

    public void setPort () {
        FileConfiguration config = getConfig();
        if (config.contains("port")) {
            port = config.getInt("port");
        }
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
            if (!sender.getName().equals("CONSOLE")) sender.sendMessage(ChatColor.RED + "Server could not start at port " + port);
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

    public static void createSourceFolder(String folder) {
        File a = new File(JavaPlugin.getPlugin(SmartManaging.class).getDataFolder() + "/" + folder);
        if (!a.exists()) {
            log("Created source folder '" + folder + "'");
            a.mkdirs();
        }
    }

    public void renderItems() {
        JFrame window = new JFrame("SmartManagingRenderer");
        GLCapabilities caps = new GLCapabilities(null);
        caps.setHardwareAccelerated(true);
        window.setLocation(0, 0);
        window.setResizable(false);
        window.setVisible(true);

        long total = 0;
        int tests = 10;
        for (int i = 0; i < tests; i++) {
            CubeRenderer panel = new CubeRenderer(caps, window, "grass_block_side", "grass_block_top", "grass_block_side", "render", i==tests-1);

            window.setContentPane(panel);
            window.pack();
            panel.requestFocusInWindow();

            while (!panel.getDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            total += panel.getRenderTime();
            System.out.println( i+1 + ": " + panel.getRenderTime() + "ms");
        }
        System.out.println("Total: " + total/1000.0 + "s");
    }

}