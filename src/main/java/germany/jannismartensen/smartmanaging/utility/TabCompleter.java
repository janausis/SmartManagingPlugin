package germany.jannismartensen.smartmanaging.utility;

import germany.jannismartensen.smartmanaging.utility.database.Connect;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    Connection connect;
    public TabCompleter(Connection conn) {
        this.connect = conn;
    }


    @Override
    public List<String> onTabComplete (@NonNull CommandSender sender,@NonNull Command cmd,@NonNull String label, String[] args) {
        if(args.length == 1 && (sender.hasPermission("managing.servermanager") || sender.hasPermission("managing.serveruser"))) {
            List<String> completions = new ArrayList<>();
            completions.add("register");
            completions.add("changepassword");
            completions.add("unregister");

            if (sender.hasPermission("managing.servermanager")) {
                completions.add("server");
            }

            return completions;
        }
        if(args.length == 2 && args[0].equalsIgnoreCase("server") && sender.hasPermission("managing.servermanager")) {
            List<String> completions = new ArrayList<>();
            completions.add("start");
            completions.add("stop");
            completions.add("reload");
            completions.add("resources");
            completions.add("generateTestData");
            completions.add("webAdmin");

            return completions;
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("generateTestData") && sender.hasPermission("managing.servermanager")) {
            return new ArrayList<>(Connect.getTopSuggestions(connect, "", 0, String.valueOf(false)));
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("resources") && sender.hasPermission("managing.servermanager")) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            return completions;
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("webAdmin") && sender.hasPermission("managing.servermanager")) {
            List<String> completions = new ArrayList<>();
            completions.add("add");
            completions.add("remove");
            return completions;
        }

        if(args.length == 4 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("webAdmin") && sender.hasPermission("managing.servermanager")) {
            if (args[2].equalsIgnoreCase("add")) return new ArrayList<>(Connect.getAllManagers(connect, false));
            if (args[2].equalsIgnoreCase("remove")) return new ArrayList<>(Connect.getAllManagers(connect, true));
        }

        return new ArrayList<>();
    }

}
