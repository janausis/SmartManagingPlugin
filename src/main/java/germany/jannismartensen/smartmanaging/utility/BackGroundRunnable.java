package germany.jannismartensen.smartmanaging.utility;

import com.jogamp.opengl.GLCapabilities;
import germany.jannismartensen.smartmanaging.SmartManaging;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

import static germany.jannismartensen.smartmanaging.utility.Util.log;

public class BackGroundRunnable implements Runnable {

    private final SmartManaging plugin;
    private final boolean reload;

    public BackGroundRunnable(SmartManaging plugin, boolean reload) {
        this.plugin = plugin;
        this.reload = reload;
    }

    public void saveSide(ArrayList<String> data) throws IOException {
        File mainSide = new File(data.get(0));

        File saveFile = new File(JavaPlugin.getPlugin(SmartManaging.class).getDataFolder() + "/renders/" + data.get(3) +  ".png");
        ImageIO.write(CubeRenderer.resizeImage(ImageIO.read(mainSide), 256, 256), "png", saveFile);
    }

    public void run() {

        if (!plugin.getConfig().getBoolean("onlineInventory")) return;

        try {
            if (Util.isEmpty(Path.of(plugin.getDataFolder() + "/resources")) || reload) {
                Util.unzip(plugin.getDataFolder() + "/resources.zip", plugin.getDataFolder() + "/resources");
            }

            File renderFolder = new File(JavaPlugin.getPlugin(SmartManaging.class).getDataFolder() + "/renders/");
            if (!renderFolder.exists()) {
                renderFolder.mkdirs();
            }

            SmartManaging.copyResources("renders/unknown.png", plugin, false);

            if (!Util.isEmpty(Path.of(plugin.getDataFolder() + "/renders")) && !reload) {
                log("No rendering necessary! To force render from resources.zip, use /managing server resources reload", 1);
                return;
            }



            log("Starting up render...", 1);
            RenderLogger.start();

            ArrayList<ArrayList<String>> blockData = Util.getResourceBlockFaces(plugin);

            JFrame window = new JFrame("Starting Up");
            GLCapabilities caps = new GLCapabilities(null);
            caps.setHardwareAccelerated(true);
            window.setLocation(0, 0);
            window.setResizable(false);
            window.setVisible(true);
            window.setExtendedState(Frame.NORMAL);

            long total = 0;
            for (ArrayList<String> data : blockData) {
                // excludes glass blocks from alpha check
                if (!data.get(3).contains("glass")) {
                    try (InputStream in = new FileInputStream(data.get(2))) {
                        BufferedImage subImage = ImageIO.read(in);
                        if (CubeRenderer.hasMoreThanThirdAlpha(subImage)) {
                            RenderLogger.add(data.get(3), true, "Face", "0ms", 4);
                            saveSide(data);
                            continue;
                        }
                    }
                } else if (data.get(3).contains("pane")) {
                    saveSide(data);
                }

                window.setName(data.get(3));
                //                                                  Right        Top          Left         Name
                CubeRenderer panel = new CubeRenderer(caps, window, data.get(1), data.get(2), data.get(0), data.get(3), false);

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
                RenderLogger.add(data.get(3), true, "3D Render", panel.getRenderTime() + "ms", 1);

            }
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));

            RenderLogger.end(String.valueOf(blockData.size()),total/1000.0 + "s");

            log("Copying and resizing item icons from resource pack...", 1);
            long st = System.currentTimeMillis();

            for (String s : Util.listFiles(Util.getAssetsPath(plugin) + "minecraft/textures/item/", 1)) {
                File f = new File(Util.getAssetsPath(plugin) + "minecraft/textures/item/" + s);
                File dst = new File(plugin.getDataFolder() + "/renders/" + s);

                ImageIO.write(CubeRenderer.resizeImage(ImageIO.read(f), 256, 256), "png", dst);
            }
            log("Successfully copied and resized files in " + (System.currentTimeMillis() - st) + "ms!", 1);

        } catch (IOException e) {
            log(e, 3);
            log("(BackGroundRunnable.run) Please add a resource pack called resource.zip to the main folder of the Smart Managing Plugin if you want to use the online inventory");
        } catch (Exception e) {
            log(e, 3);
            log("(BackGroundRunnable.run) Unknown error whilst rendering");
        }
    }
}
