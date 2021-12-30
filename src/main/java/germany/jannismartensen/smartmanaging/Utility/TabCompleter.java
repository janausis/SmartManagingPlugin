package germany.jannismartensen.smartmanaging.Utility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("register");
            completions.add("changepassword");
            completions.add("unregister");
            completions.add("server");

            return completions;
        }
        if(args.length == 2 && args[0].equalsIgnoreCase("server")) {
            List<String> completions = new ArrayList<>();
            completions.add("start");
            completions.add("stop");

            return completions;
        }
        return new ArrayList<>();
    }

}
