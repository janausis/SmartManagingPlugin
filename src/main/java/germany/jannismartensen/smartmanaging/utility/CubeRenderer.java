package germany.jannismartensen.smartmanaging.utility;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.jogamp.opengl.GL.*;

public class CubeRenderer extends GLJPanel implements GLEventListener {


    private final boolean close;
    private boolean done = false;
    private final long inst1;
    private long inst2;
    private final int width = 256;
    private final int height = 256;
    private File outFile;
    private final String outName;
    JFrame window;
    private final float rotateX;
    private final float rotateY;
    private final float rotateZ;   // rotation amounts about axes
    private int texture;
    private int texture2;
    private int texture3;
    private final String t1name;
    private final String t2name;
    private final String t3name;

    // Correct orientation -45.0, 150.0, 90.0
    public CubeRenderer(GLCapabilities capabilities, JFrame window, String t1, String t2, String t3, String outName, boolean close) {
        super(capabilities);
        inst1 = System.currentTimeMillis();
        setPreferredSize( new Dimension(width,height) );
        addGLEventListener(this);
        rotateX = -45.0f;
        rotateY = 150.0f;
        rotateZ = 90.0f;
        this.window = window;
        this.outName = outName;
        this.close = close;
        this.t1name = t1;
        this.t2name = t2;
        this.t3name = t3;
    }

    // ----------------- define some drawing methods for this program ----------

    private void square(GL2 gl) {
        gl.glTranslatef(0,0,0.5f);    // Move square 0.5 units forward.
        gl.glNormal3f(0,0,1);        // Normal vector to square (this is actually the default).


        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.5f);
        gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.5f);
        gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, 0.5f);
        gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, 0.5f);
        gl.glEnd();
    }

    private void cube(GL2 gl) {

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
        gl.glPushMatrix();
        gl.glRotatef(180,1,0,0); // rotate square to right face
        square(gl);
        gl.glPopMatrix();

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture2);
        gl.glPushMatrix();
        gl.glRotatef(-90,0,1,0); // rotate square to top face
        square(gl);
        gl.glPopMatrix();

        gl.glBindTexture(GL2.GL_TEXTURE_2D, texture3);
        gl.glPushMatrix();
        gl.glRotatef(-90,1,0,0); // rotate square to left face
        square(gl);
        gl.glPopMatrix();
    }


    // ---------------  Methods of the GLEventListener interface -----------

    public void display(GLAutoDrawable drawable) {
        // called when the panel needs to be drawn

        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL4bc.GL_BLEND);
        gl.glBlendFunc( GL4bc.GL_SRC_ALPHA, GL4bc.GL_ONE_MINUS_SRC_ALPHA );
        gl.glEnable(GL_ALPHA);
        gl.glColorMask(true, true, true, true);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);


        gl.glMatrixMode(GL2.GL_PROJECTION);  // Set up the projection.
        gl.glLoadIdentity();
        gl.glOrtho(-1,1,-1,1,-2,2);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glLoadIdentity();             // Set up modelview transform.
        gl.glRotatef(rotateZ,0,0,1);
        gl.glRotatef(rotateY,0,1,0);
        gl.glRotatef(rotateX,1,0,0);
        gl.glScaled(0.5, 0.5, 0.5);

        cube(gl);


        try {
            BufferedImage screenshot = makeScreenshot(gl, width, height);
            ImageIO.write(screenshot, "png", new File("./"  + outName +  ".png"));
            outFile = new File("./" + outName + ".png");
            inst2 = System.currentTimeMillis();
            done = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            done = true;
        }
        if (close)
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));

    }

    public File getOutFile() {
        return outFile;
    }

    public long getRenderTime() {
        return inst2 - inst1;
    }

    public boolean getDone() {
        return done;
    }

    public void init(GLAutoDrawable drawable) {
        // called when the panel is created
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glEnable(GL_MULTISAMPLE);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL4bc.GL_BLEND);
        gl.glBlendFunc( GL4bc.GL_SRC_ALPHA, GL4bc.GL_ONE_MINUS_SRC_ALPHA );
        gl.glEnable(GL_ALPHA);
        gl.glColorMask(true, true, true, true);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);


        try{

            try (InputStream in = getClass().getResourceAsStream("/"+t1name+".png")) {
                assert in != null;

                BufferedImage subImage = rotateImage(resizeImage(ImageIO.read(in), width, height), -90);
                File rotatedImageFile = new File("./"+t1name+"_rotated.png");
                ImageIO.write(subImage, "png", rotatedImageFile);
                Texture t = TextureIO.newTexture(rotatedImageFile, true);
                texture= t.getTextureObject(gl);
                rotatedImageFile.delete();

            }

            try (InputStream in = getClass().getResourceAsStream("/"+t2name+".png")) {
                assert in != null;

                BufferedImage subImage = rotateImage(resizeImage(ImageIO.read(in), width, height), 0);
                File rotatedImageFile = new File("./"+t2name+"_rotated.png");
                ImageIO.write(subImage, "png", rotatedImageFile);
                Texture t = TextureIO.newTexture(rotatedImageFile, true);
                texture2 = t.getTextureObject(gl);
                rotatedImageFile.delete();

            }

            try (InputStream in = getClass().getResourceAsStream("/"+t3name+".png")) {
                assert in != null;

                BufferedImage subImage = rotateImage(resizeImage(ImageIO.read(in), width, height), -90);
                File rotatedImageFile = new File("./"+t3name+"_rotated.png");
                ImageIO.write(subImage, "png", rotatedImageFile);
                Texture t = TextureIO.newTexture(rotatedImageFile, true);
                texture3 = t.getTextureObject(gl);
                rotatedImageFile.delete();

            }


        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void dispose(GLAutoDrawable drawable) {
        // called when the panel is being disposed
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // called when user resizes the window
    }

    public static BufferedImage rotateImage(BufferedImage imageToRotate, int val) {
        int widthOfImage = imageToRotate.getWidth();
        int heightOfImage = imageToRotate.getHeight();
        int typeOfImage = imageToRotate.getType();

        BufferedImage newImageFromBuffer = new BufferedImage(widthOfImage, heightOfImage, typeOfImage);

        Graphics2D graphics2D = newImageFromBuffer.createGraphics();

        graphics2D.rotate(Math.toRadians(val), widthOfImage / 2.0f, heightOfImage / 2.0f);
        graphics2D.drawImage(imageToRotate, null, 0, 0);

        return newImageFromBuffer;
    }

    BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        if (targetHeight == originalImage.getHeight() && targetWidth == originalImage.getWidth()) {
            return originalImage;
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public BufferedImage makeScreenshot(GL2 gl, int width, int height) {
        BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = screenshot.getGraphics();

        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        // BufferUtils.createByteBuffer(width * height * 3);

        gl.glReadPixels(0, 0, width, height, GL_RGBA, GL_BYTE, buffer);


        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                // The color are the three consecutive bytes, it's like referencing
                // to the next consecutive array elements, so we got red, green, blue..
                // red, green, blue, and so on..
                int r = buffer.get()*2;
                int g = buffer.get()*2;
                int b = buffer.get()*2;
                int a = buffer.get()*2;


                if (r == 0 && g == 0 & b == 0) a = 0;

                graphics.setColor(new Color(r, g, b, a));
                graphics.drawRect(w,height - h, 1, 1); // height - h is for flipping the image
            }
        }

        return screenshot;
    }

}