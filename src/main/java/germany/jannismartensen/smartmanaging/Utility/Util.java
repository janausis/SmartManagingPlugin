package germany.jannismartensen.smartmanaging.Utility;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;

public class Util {
    public static String PREFIX = "[SmartManaging] ";

    public static void log (String message) {
        Bukkit.getLogger().log(Level.INFO, PREFIX + message);
    }
    public static void log (String message, int status) {
        switch (status) {
            case 1 -> Bukkit.getLogger().log(Level.CONFIG, PREFIX + message);
            case 2 -> Bukkit.getLogger().log(Level.WARNING, PREFIX + message);
            case 3 -> Bukkit.getLogger().log(Level.SEVERE, PREFIX + message);
            default -> Bukkit.getLogger().log(Level.INFO, PREFIX + message);
        }

    }

    public static void registerUser (String m, Connection conn, CommandSender user, String password) {
        Connect.insertUser(m, conn, user, generateHash(password));
    }

    public static void deleteUser (Connection conn, CommandSender user, String password) {
        Connect.deleteUser(conn, user, generateHash(password));
    }

    public static void changeUserPassword (Connection conn, CommandSender user, String password, String oldPassword) {
        Connect.updatePassword(conn, user, generateHash(password), generateHash(oldPassword));
    }

    public static String generateHash(final String base) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static String readAllBytes(String filePath)
    {
        String content = "";

        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return content;
    }

    public static boolean loggedIn(HttpExchange he, Connection connect, SmartManaging plugin) {
        Headers reqHeaders = he.getRequestHeaders();
        List<String> cookies = null;
        try {
            cookies = reqHeaders.get("Cookie");
        } catch (Exception e) {
            log(e.getMessage(), 3);
        }

        if (cookies == null) {
            return false;
        }

        for (String c : cookies) {
            String identifier;

            String[] cookie = c.split("=");
            log(cookie[0]);
            if (cookie[0].equals("login")) {
                log(cookie[1]);
                identifier = cookie[1];
                return Connect.getPlayerFromCookie(connect, identifier) != null;
            }
        }
        return false;
    }

    public static String getIpOrDomain(SmartManaging plugin) {
        String ip;
        FileConfiguration config = plugin.getConfig();
        String domain = config.getString("domain");
        ip = plugin.getServer().getIp();
        if (domain != null) {
            if (!domain.isEmpty()) {
                ip = config.getString("domain");
            }
        }

        return ip;
    }
}
