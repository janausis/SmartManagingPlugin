package germany.jannismartensen.smartmanaging.Utility;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import germany.jannismartensen.smartmanaging.Endpoints.Cookie;
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
import java.util.Calendar;
import java.util.Date;
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
    public static void logAccess(HttpExchange he) {
        log(he.getRemoteAddress().toString().replace("/", "") + " accessed " + he.getRequestURI() + ": " + he.getRequestMethod());
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
        if (hasCookie(he)) {
            Headers reqHeaders = he.getRequestHeaders();
            List<String> cookies = null;
            try {
                cookies = reqHeaders.get("Cookie");
            } catch (Exception e) {
                log(e.getMessage(), 3);
            }

            for (String c : cookies) {
                String identifier;

                String[] cookie = c.split("=");
                if (cookie[0].equals("login")) {
                    identifier = cookie[1];
                    return Connect.getPlayerFromCookie(connect, identifier) != null;
                }
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

    public static Cookie invalidCookie() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -15);
        Date nextYear = cal.getTime();

        return new Cookie("login", "", nextYear, null, "192.168.1.25", "/", false, false, null);
    }

    public static boolean hasCookie(HttpExchange he) {
        Headers reqHeaders = he.getRequestHeaders();
        List<String> cookies = null;
        try {
            cookies = reqHeaders.get("Cookie");
        } catch (Exception e) {
            log(e.getMessage(), 3);
        }

        return cookies != null;
    }

    public static String getCookie(HttpExchange he) {
        Headers reqHeaders = he.getRequestHeaders();
        List<String> cookies = null;
        try {
            cookies = reqHeaders.get("Cookie");
        } catch (Exception e) {
            log(e.getMessage(), 3);
        }

        if (cookies == null) {
            return "";
        }

        for (String c : cookies) {
            String identifier;

            String[] cookie = c.split("=");
            if (cookie[0].equals("login")) {
                identifier = cookie[1];
                return identifier;
            }
        }

        return "";
    }

    public static Headers deleteInvalidCookies(boolean logged, HttpExchange he) {
        Headers headers = he.getResponseHeaders();
        if (!logged && Util.hasCookie(he)) {
            headers.add("Set-Cookie", Util.invalidCookie().toString());
        }
        return headers;
    }
}
