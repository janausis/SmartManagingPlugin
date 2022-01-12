package germany.jannismartensen.smartmanaging.utility;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.endpoints.Cookie;
import germany.jannismartensen.smartmanaging.utility.database.Connect;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Util {
    public static final String PREFIX = "[SmartManaging] ";
    public static final String PREFIXINFO =    "[SmartManaging/INFO]    ";
    public static final String PREFIXDEBUG =   "[SmartManaging/DEBUG]   ";
    public static final String PREFIXWARNING = "[SmartManaging/WARNING] ";
    public static final String PREFIXERROR =   "[SmartManaging/ERROR]   ";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public static final String[] logTypes = {"file", "console", "both"};

    public static void log (String message) {
        log(message, 0);
    }

    public static void log (String message, int status) {
        SmartManaging plugin = JavaPlugin.getPlugin(SmartManaging.class);

        String logType = getLogStatus(plugin, "logLocation");
        if (logType.equals("file")) {
            logToFile(message, status, plugin, "");
        } else if (logType.equals("both")) {
            logToConsole(message, status, true);
            logToFile(message, status, plugin, "");
        } else {
            logToConsole(message, status, true);
        }
    }

    public static void log (Exception e, int status) {
        SmartManaging plugin = JavaPlugin.getPlugin(SmartManaging.class);

        String logType = getLogStatus(plugin, "logLocation");
        if (logType.equals("file")) {
            logToFile(e.getMessage(), status, plugin, "");
            logToFile("\n\n" + ExceptionUtils.getStackTrace(e), status, plugin, "");
        } else if (logType.equals("both")) {
            logToFile(e.getMessage(), status, plugin, "");
            logToFile("\n\n" + ExceptionUtils.getStackTrace(e), status, plugin, "");

            logToConsole(e.getMessage(), status, true);
            logToConsole("\n\n" + ExceptionUtils.getStackTrace(e), status);
        } else {
            logToConsole(e.getMessage(), status, true);
            logToConsole("\n\n" + ExceptionUtils.getStackTrace(e), status);
        }
    }

    public static void log (String message, int status, boolean forceConsole) {
        SmartManaging plugin = JavaPlugin.getPlugin(SmartManaging.class);

        if (forceConsole && getLogStatus(plugin, "logLocation").equals("file")) {
            logToConsole(message, status, true);
        }
        log(message, status);
    }

    public static void logToConsole(String message, int status) {
        switch (status) {
            case 2 -> Bukkit.getLogger().log(Level.WARNING, PREFIX + message);
            case 3 -> Bukkit.getLogger().log(Level.SEVERE, PREFIX + message);
            default -> Bukkit.getLogger().log(Level.INFO, PREFIX + message);
        }
    }

    public static void logToConsole(String message, int status, boolean color) {
        if (color) {
            switch (status) {
                case 1 -> Bukkit.getLogger().log(Level.INFO, PREFIX + ANSI_GREEN + message + ANSI_RESET);
                case 2 -> Bukkit.getLogger().log(Level.WARNING, PREFIX + ANSI_YELLOW + message + ANSI_RESET);
                case 3 -> Bukkit.getLogger().log(Level.SEVERE, PREFIX + ANSI_RED + message + ANSI_RESET);
                case 4 -> Bukkit.getLogger().log(Level.INFO, PREFIX + ANSI_YELLOW + message + ANSI_RESET);
                default -> Bukkit.getLogger().log(Level.INFO, PREFIX + message);
            }
        } else {
            logToConsole(message, status);
        }
    }

    public static void logToFile(String message, int status, SmartManaging plugin, String FileName) {
        String ogName = FileName;
        String msg;
        switch (status) {
            case 1 -> msg = PREFIXDEBUG + message;
            case 2 -> msg = PREFIXWARNING + message;
            case 3 -> msg = PREFIXERROR + message;
            default -> msg = PREFIXINFO + message;
        }
        if (!FileName.equals("")) {
            FileName = "/" + FileName + "/";
        }

        String timeStamp = new SimpleDateFormat("[HH:mm:ss]").format(new Date());
        String fileStamp = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        msg = timeStamp + " " + msg + "\n";
        File logFolder = new File(plugin.getDataFolder() + "/logs" + FileName);
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        try {
            BufferedWriter output;
            if (ogName.equals("")) {
                output = new BufferedWriter(new FileWriter(plugin.getDataFolder() + "/logs/" + FileName + fileStamp + ".txt", true));
            } else {
                output = new BufferedWriter(new FileWriter(plugin.getDataFolder() + "/logs/" + FileName + ogName + "-" +fileStamp + ".txt", true));
            }

            output.append(msg);
            output.close();

        } catch (IOException e) {
            logToConsole("(Util.logToFile) The logger is unable to open the log file, logging to console instead.", 3);
            if (getLogStatus(plugin, "logLocation").equals("file")) {
                logToConsole(message, status);
            }
        }
    }

    public static String root() {
        return "http://" + Util.getIpOrDomain(JavaPlugin.getPlugin(SmartManaging.class)) + ":" + SmartManaging.port + "/";
    }

    public static void logAccess(HttpExchange he) {
        SmartManaging plugin = JavaPlugin.getPlugin(SmartManaging.class);
        FileConfiguration config = plugin.getConfig();

        String message = he.getRemoteAddress().toString().replace("/", "") + " accessed " + he.getRequestURI() + ": " + he.getRequestMethod();
        int status = 0;

        if (config.contains("logAccess") && Objects.equals(config.getString("logAccess"), "true")) {
            String logType = getLogStatus(plugin, "accessLogLocation");
            if (logType.equals("file")) {
                logToFile(message, status, plugin, "access");
            } else if (logType.equals("both")) {
                logToFile(message, status, plugin, "access");
                logToConsole(message, status);
            } else {
                logToConsole(message, status);
            }
        }
    }

    public static String getLogStatus(SmartManaging plugin, String logType) {
        FileConfiguration config = plugin.getConfig();
        String logTo = config.getString(logType);
        if (logTo == null || Arrays.stream(logTypes).noneMatch(logTo::equals)) {
            logTo = "console";
        }
        return logTo;
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
        try {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static boolean loggedIn(HttpExchange he, Connection connect) {
        if (hasCookie(he)) {
            Headers reqHeaders = he.getRequestHeaders();
            List<String> cookies;
            try {
                cookies = reqHeaders.get("Cookie");
            } catch (Exception e) {
                log(e, 3);
                log("(Util.loggedIn) Could not find Cookie in headers", 2);
                return false;
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
        List<String> cookies;
        try {
            cookies = reqHeaders.get("Cookie");
        } catch (Exception e) {
            log("(Util.hasCookie) Could not find Cookie in headers, logging user out", 2);
            return false;
        }

        return cookies != null;
    }

    public static String getCookie(HttpExchange he) {
        Headers reqHeaders = he.getRequestHeaders();
        List<String> cookies;
        try {
            cookies = reqHeaders.get("Cookie");
        } catch (Exception e) {
            log("(Util.getCookie) Could not find Cookie in headers, logging user out", 2);
            return "";
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
        Headers headers = he.getResponseHeaders();
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

    public static String getStringFromArray1(ArrayList<String> valueList) {
        StringBuilder valueString = new StringBuilder("[");

        for (String va : valueList) valueString.append('\"').append(va).append("\",");
        valueString.deleteCharAt(valueString.length() - 1);
        valueString.append("]");

        return valueString.toString();
    }

    public static String getStringFromArray(ArrayList<ArrayList<String>> valueList) {
        StringBuilder valueString = new StringBuilder("[");
        for (ArrayList<String> value : valueList) {
            valueString.append("[");
            for (String va : value) valueString.append('\"').append(va).append("\",");
            valueString.deleteCharAt(valueString.length() - 1);
            valueString.append("],");
        }
        valueString.deleteCharAt(valueString.length() - 1);
        valueString.append("]");

        return valueString.toString();
    }

    public static String getStringFromArray3(ArrayList<ArrayList<ArrayList<String>>> valueList) {
        try {
        StringBuilder valueString = new StringBuilder("[");
        for (ArrayList<ArrayList<String>> value : valueList) {
            valueString.append("[");

            for (ArrayList<String> subvalue : value) {
                valueString.append("[");
                for (String va : subvalue) valueString.append('\"').append(va).append("\",");
                valueString.deleteCharAt(valueString.length() - 1);
                valueString.append("],");
            }
            valueString.deleteCharAt(valueString.length() - 1);
            valueString.append("],");
        }
        valueString.deleteCharAt(valueString.length() - 1);
        valueString.append("]");

        return valueString.toString();
        } catch (Exception e) {
            log(e, 3);
            log("(Util.getStringFromArray3) There was an error converting an array to string!", 3);
            return "";
        }
    }

    public static ArrayList<String> getWorldList(SmartManaging plugin) {

        FileConfiguration config = plugin.getConfig();
        String l = Objects.requireNonNull(config.getString("worldName")).replace(" ","");

        return new ArrayList<>(Arrays.asList(l.split(",")));
    }

    public static String getInventoryWorldList(SmartManaging plugin) {
        return Objects.requireNonNull(plugin.getConfig().getString("inventoryWorld")).replace(" ","");
    }

    public static ArrayList<String> getWorldList(SmartManaging plugin, String path) {

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(path);
        String l = Objects.requireNonNull(Objects.requireNonNull(section).getString("worldName"));

        ArrayList<String> out = new ArrayList<>();
        for (String s : l.split(",")) {
            out.add(s.trim());
        }
        return out;
    }

    public static String getPlayTime(SmartManaging plugin, ManagingPlayer user) {

        int tick = 0;
        for (String world : Objects.requireNonNull(getWorldList(plugin))) {
            try {
                tick += Integer.parseInt(readStats(user, world, "minecraft:custom;minecraft:play_time"));
            } catch (NumberFormatException e) {
                log(e, 3);
                log("(Util.getPlayTime) Tick value wasn't convertible to integer!", 3);
                return "";
            }
        }

        return tickBeautifier(String.valueOf(tick));
    }

    // @Param stat: type;stat
    public static String readStats(ManagingPlayer user, String worldName, String stat) {
        if (stat.equals("minecraft:custom;minecraft:total_kills")) {
            try {
                String player = readStats(user, worldName, "minecraft:custom;minecraft:player_kills");
                String mob = readStats(user, worldName, "minecraft:custom;minecraft:mob_kills");
                return String.valueOf(Integer.parseInt(player) + Integer.parseInt(mob));

            } catch (NumberFormatException e) {
                log(e, 3);
                log("(Util.readStats) Kill stat value wasn't convertible to integer!", 3);
                return "";
            }
        }

        String statOut = "";
        String uuid = user.getUUID();
        String type = stat.split(";")[0].trim();
        String st = stat.split(";")[1].trim();


        JSONParser parser = new JSONParser();
        try {
            JSONObject main = (JSONObject) parser.parse(new FileReader(worldName + "/stats/" + uuid + ".json"));
            JSONObject stats = (JSONObject) main.get("stats");
            if (!stats.containsKey(type)) {
                return "0";
            }
            JSONObject custom = (JSONObject) stats.get(type);
            if (!custom.containsKey(st)) {
                return "0";
            }

            statOut = custom.get(st).toString();

        } catch (IOException | ParseException e) {
            log(e, 3);
            log("(Util.readStats) Could not parse JSON, returning empty string", 3);
        } catch (Exception a) {
            log(a, 3);
            log("(Util.readStats) There was an unusual error, please have a look into the logs.", 3, true);
        }

        return statOut;
    }

    public static String tickBeautifier(String ticks) {
        int tick;
        try {
            tick = Integer.parseInt(ticks);
            int seconds = tick/20;
            int days = (int) Math.floor(seconds/60.0/60.0/24.0);

            Date d = new Date(seconds * 1000L);
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            if (days != 0) {
                return days + ":" + df.format(d);
            } else {
                return df.format(d);
            }

        } catch (NumberFormatException e) {
            log(e, 3);
            log("(Util.tickBeautifier) Tick value wasn't convertible to integer!", 3);
            return ticks;
        }
    }

    public static void zipLog(SmartManaging plugin, String logFolder) {
        File f = new File(plugin.getDataFolder() + "/logs/" + logFolder);

        FilenameFilter textFilter = (dir, name) -> name.toLowerCase().endsWith(".txt");
        String identifier;
        if (logFolder.equals("")) {
            identifier = "";
        } else {
            identifier = logFolder.replace("/", "") + "-";
        }

        File[] files = f.listFiles(textFilter);
        if (files != null) {
            String fileStamp = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
            for (File file : files) {
                if (file.isFile() && !file.getName().equals(identifier + fileStamp + ".txt")) {
                    Util.zipLog(logFolder + file.getName(), plugin);
                }
            }
        }
    }

    public static void zipLog(String filename, SmartManaging plugin) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(plugin.getDataFolder() + "/logs/" + filename.substring(0, filename.length()-4) + ".zip");

            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(plugin.getDataFolder() + "/logs/" + filename);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();

            fileToZip.delete();

        } catch (FileNotFoundException e) {
            log(e, 3);
            log("(Util.zipLog) Could not find the file " + plugin.getDataFolder() + "/logs/" + filename + ".zip", 3);
        } catch (IOException e) {
            log(e, 3);
            log("(Util.zipLog) There was an error whilst compressing the file " + plugin.getDataFolder() + "/logs/" + filename + ".zip", 3);
        }

    }

    public static ManagingPlayer getUser(Connection connect, HttpExchange he, SmartManaging plugin) {
        ManagingPlayer user = Connect.getPlayerFromCookie(connect, Util.getCookie(he));
        try {
            if (user == null) {
                redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/logout");
            } else if (user.getUUID() == null || user.getName() == null || user.getCookie() == null || user.getPassword() == null) {
                redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/logout");
            }
        } catch (IOException e) {
            log(e, 3);
            log("(Util.getUser) Unable to redirect user to logout", 3);
        }

        return user;
    }

    public static void unzip(String zip, String path) throws IOException {
        File destDir = new File(path);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }

        return false;
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static Set<String> listFolders(String dir, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static Set<String> listFiles(String dir, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public static String getAssetsPath(SmartManaging plugin) throws IOException {
        String assetsPath = plugin.getDataFolder() + "/resources/";
        assetsPath = assetsPath.replace("\\", "/");
        // get main folder

        for (String s : listFolders(assetsPath, 10)) {
            if (s.endsWith("assets")) {
                s = s.replace("\\", "/");
                assetsPath += s.split(assetsPath)[s.split(assetsPath).length-1] + "/";
            }
        }
        if (assetsPath.endsWith("resources/")) {
            log("(Util.getResourceBlockFaces) Could not find assets folder in resources folder");
            throw new IOException();
        }

        return assetsPath;
    }

    public static ArrayList<ArrayList<String>> getResourceBlockFaces(SmartManaging plugin) throws IOException {
        ArrayList<ArrayList<String>> out = new ArrayList<>();
        ArrayList<String> blockList = new ArrayList<>();

        String blockPath = getAssetsPath(plugin) + "minecraft/textures/block/";

        Set<String> items = listFiles(getAssetsPath(plugin) + "minecraft/textures/item/", 1);
        for (String file : listFiles(blockPath, 1)) {
            if (!file.endsWith(".png")) continue;

            // Left, Right, Topside
            ArrayList<String> subList = new ArrayList<>();

            String[] spl = file.split("_");
            String block;
            String[] invisibleSides = {"bottom", "back", "end", "left"};
            String[] sides = {"top", "right", "front", "side"};

            if (spl.length > 0 && Arrays.asList(invisibleSides).contains(spl[spl.length - 1].replace(".png", ""))) {
                continue;
            }
            if (spl.length > 0 && Arrays.asList(sides).contains(spl[spl.length - 1].replace(".png", ""))) {
                 block = file.replace("_" + spl[spl.length - 1], "");
            } else {
                block = file.replace(".png", "");
            }

            if (items.contains(block + ".png") || items.contains(file) || blockList.contains(block)) {
                continue;
            }

            blockList.add(block);


            File main = new File(blockPath + block.replace("_sticky", "") + ".png");
            File end = new File(blockPath + block + "_end.png");
            File front = new File(blockPath + block + "_front.png");
            File right = new File(blockPath + block + "_right.png");
            File side = new File(blockPath + block + "_side.png");
            File top = new File(blockPath + block.replace("cut_", "").replace("chiseled_", "") + "_top.png");

            if (front.exists()) { subList.add(front.getPath()); }
            else if (side.exists()) { subList.add(side.getPath()); }
            else if (main.exists()) { subList.add(main.getPath()); }
            else if (end.exists()) { subList.add(end.getPath()); }
            else { subList.add(blockPath + file); }

            if (right.exists()) { subList.add(right.getPath()); }
            else if (side.exists()) { subList.add(side.getPath()); }
            else if (main.exists()) { subList.add(main.getPath()); }
            else if (end.exists()) { subList.add(end.getPath()); }
            else { subList.add(blockPath + file); }

            if (top.exists()) { subList.add(top.getPath()); }
            else if (end.exists()) { subList.add(end.getPath()); }
            else if (main.exists()) { subList.add(main.getPath()); }
            else { subList.add(blockPath + file); }

            if (!front.exists() && !right.exists() && !side.exists() && !top.exists() && !file.replace(".png", "").equals(block)) {
                block = file.replace(".png", "");
            }

            subList.add(block);
            out.add(subList);

        }

        return out;
    }

    public static void showErrorPage(HttpExchange he, TemplateEngine engine, Exception e, boolean loggedIn, String errorText) throws IOException {
        SmartManaging plugin = JavaPlugin.getPlugin(SmartManaging.class);
        Map<String, String> map = new HashMap<>();
        map.put("errorText", errorText);
        map.put("stackTrace", "\n\n" + ExceptionUtils.getStackTrace(e).replace("at ", "<br>at "));
        map.put("loggedin", String.valueOf(loggedIn));

        SmartManaging.copyResources("Templates/error.html", plugin, false);
        String response = engine.renderTemplate("error.html", map);

        he.sendResponseHeaders(500, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
