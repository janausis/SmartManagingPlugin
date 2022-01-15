package germany.jannismartensen.smartmanaging.endpoints;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import germany.jannismartensen.smartmanaging.SmartManaging;
import germany.jannismartensen.smartmanaging.utility.ManagingPlayer;
import germany.jannismartensen.smartmanaging.utility.TemplateEngine;
import germany.jannismartensen.smartmanaging.utility.Util;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import org.bukkit.Material;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static germany.jannismartensen.smartmanaging.utility.Util.*;

public class Inventory implements HttpHandler {

    final TemplateEngine engine;
    final SmartManaging plugin;
    final Connection connect;
    String playerName = "";

    public Inventory(TemplateEngine e, SmartManaging m, Connection c) {
        this.plugin = m;
        this.engine = e;
        this.connect = c;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Util.logAccess(he);


        if (!Util.loggedIn(he, connect) || !plugin.getConfig().getBoolean("onlineInventory")) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            return;
        }

        ManagingPlayer user = Util.getUser(connect, he, plugin);
        if (user == null) {
            redirect(plugin, he,"http://" + Util.getIpOrDomain(plugin) + ":" + SmartManaging.port + "/");
            return;
        }

        SmartManaging.createSourceFolder("resources");
        try {
            if (Util.isEmpty(Path.of(plugin.getDataFolder() + "/resources"))) {
                Util.unzip(plugin.getDataFolder() + "/resources.zip", plugin.getDataFolder() + "/resources");
            }
        } catch (IOException e) {
            log(e, 3);
            log("(Inventory.handle) Couldn't find a file called resources.zip, please add a resource pack if you want to use the web inventory", 3, true);
            Util.showErrorPage(he, engine, e, true, "Couldn't find a resource pack, please ask the staff to add one as resources.zip if you want to use the web inventory");
            return;

        }


        Map<String, String> map = new HashMap<>();
        map.put("playername", user.getName());
        Util.getNavbarRoutes(plugin, map, Util.loggedIn(he, connect));

        map = readNBT(map, user);

        Headers headers = Util.deleteInvalidCookies(Util.loggedIn(he, connect), he);

        SmartManaging.copyResources("Templates/Inventory.html", plugin, false);
        String response = engine.renderTemplate("Inventory.html", map);

        he.sendResponseHeaders(200, 0);
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }



    public Map<String,String> readNBT(Map<String,String> map, ManagingPlayer user) {
        try {
            HashMap<String, JSONObject> main = new HashMap<>();
            HashMap<String, JSONObject> data = new HashMap<>();


            NamedTag playerNBT = NBTUtil.read(Util.getInventoryWorldList(plugin) + "/playerdata/" + user.getUUID() + ".dat");
            // Convert fil into tag
            CompoundTag t = (CompoundTag) playerNBT.getTag();

            // read inventory from tag, get list and iterate over it
            ListTag<?> inventory = t.getListTag("Inventory");
            for (int i = 0; i < inventory.size(); i++) {
                HashMap<String, String> slotData = new HashMap<>();
                CompoundTag slot = (CompoundTag) inventory.get(i);
                slotData.put("count", String.valueOf(slot.getByte("Count")));
                if (slot.containsKey("tag")) {
                    slotData.put("tag", SNBTUtil.toSNBT(slot.getCompoundTag("tag")));
                } else {
                    slotData.put("tag", "");
                }
                String id = slot.getString("id");
                slotData.put("id", id);

                Material block = Material.getMaterial(id.toUpperCase().split(":")[1]);
                if (block == null) {
                    slotData.put("maxStack", "64");
                } else {
                    slotData.put("maxStack", String.valueOf(block.getMaxStackSize()));
                }
                data.put(String.valueOf(slot.getByte("Slot")), new JSONObject(slotData));
            }

            main.put("data", new JSONObject(data));
            map.put("data", new JSONObject(main).toJSONString());

        } catch (IOException e) {
            log(e, 3);
            log("(Inventory.readNBT) The nbt file for user " + user.getName() + " was not found!", 3, true);
        } catch (Exception e) {
            log(e, 3);
            log("(Inventory.readNBT) Unknown Error, check logs", 3, true);
        }
        return map;
    }
}
