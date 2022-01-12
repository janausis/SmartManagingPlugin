package germany.jannismartensen.smartmanaging.utility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {
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

            return completions;
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("resources") && sender.hasPermission("managing.servermanager")) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            return completions;
        }
        return new ArrayList<>();
    }

}
