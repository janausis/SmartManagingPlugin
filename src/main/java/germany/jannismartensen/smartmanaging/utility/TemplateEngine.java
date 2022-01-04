package germany.jannismartensen.smartmanaging.utility;

import freemarker.template.*;
import germany.jannismartensen.smartmanaging.SmartManaging;

import java.util.*;
import java.io.*;

import static germany.jannismartensen.smartmanaging.utility.Util.log;

public class TemplateEngine {

    final Configuration cfg;
    final SmartManaging plugin;

    public TemplateEngine(SmartManaging managing) {
        this.plugin = managing;

        /* Create and adjust the configuration singleton */
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        try {
            cfg.setDirectoryForTemplateLoading(new File(plugin.getDataFolder() + "/Templates"));
        } catch (IOException e) {
            log("Template folder could not be found! Creating new one...", 2);
            SmartManaging.copyResources("Templates", plugin, true);
            log("Created new template folder!");
            try {
                cfg.setDirectoryForTemplateLoading(new File(plugin.getDataFolder() + "/Templates"));
            } catch (IOException ex) {
                log(ex, 3);
                log("(TemplateEngine.TemplateEngine) Template folder still could not be created! Stopping server...", 3, true);
                plugin.stopServer();
            }
        }

        // Recommended settings for new projects:
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(true);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);

    }

    public String renderTemplate(String template, Map<String, String> data) {

        /* Get the template (uses cache internally) */
        Template temp;
        try {
            temp = cfg.getTemplate(template);
            Writer out = new StringWriter();
            temp.process(data, out);
            return out.toString();


        } catch (IOException e) {
            log(e, 3);
            log("(TemplateEngine.renderTemplate) Could not find or open the template: '" + template + "'", 3, true);

        } catch (TemplateException e) {
            log(e, 3);
            log("(TemplateEngine.renderTemplate) Error whilst rendering template: '" + template + "'", 3, true);
        }
        return "Error";

    }
}
