package germany.jannismartensen.smartmanaging.Utility;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import germany.jannismartensen.smartmanaging.Endpoints.Cookie;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.Utility.Database.Connect;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
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

    public static void redirect(SmartManaging plugin, HttpExchange he, String location) throws IOException {
        Headers headers = Util.deleteInvalidCookies(false, he);
        headers.add("Location", location);
        String response = "";
        he.sendResponseHeaders(302, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

    public  static Map<String, String> streamToMap(InputStream is) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;


        while ((line = br.readLine()) != null) {
            content.append(line);
            content.append("\n");
        }

        String[] values = content.toString().split("&");
        Map<String, String> map = new HashMap<>();
        for (String s : values) {
            String[] sl = s.split("=");
            map.put(sl[0], sl[1]);
        }

        return map;
    }

    public static String getStringFromArray(ArrayList<ArrayList<String>> valueList) {
        StringBuilder valueString = new StringBuilder("[");
        for (ArrayList<String> value : valueList) {
            valueString.append("[");
            for (String va : value) valueString.append('\"').append(va.replace(" ", "")).append("\",");
            valueString.deleteCharAt(valueString.length() - 1);
            valueString.append("],");
        }
        valueString.deleteCharAt(valueString.length() - 1);
        valueString.append("]");

        return valueString.toString();
    }

    public static String getPlayTime(ManagingPlayer user) {
        String playtime = "";
        String uuid = user.getUUID();

        JSONParser parser = new JSONParser();
        try {
            JSONObject main = (JSONObject) parser.parse(new FileReader("world/stats/" + uuid + ".json"));
            JSONObject stats = (JSONObject) main.get("stats");
            JSONObject custom = (JSONObject) stats.get("minecraft:custom");
            playtime = tickBeautifier(custom.get("minecraft:play_time").toString());

        } catch (IOException | ParseException e) {
            log(e.getMessage(), 3);
        }

        return playtime;
    }

    public static String tickBeautifier(String ticks) {
        int tick = 0;
        try {
            tick = Integer.parseInt(ticks);
            int seconds = tick/20;

            Date d = new Date(seconds * 1000L);
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return df.format(d);

        } catch (NumberFormatException e) {
            log(e.getMessage(), 3);
            log("(Util.tickBeautifier) Tick value wasn't convertible to integer!", 3);
            return ticks;
        }
    }
}
