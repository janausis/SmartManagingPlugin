package germany.jannismartensen.smartmanaging.utility;
// Use OpenGL and JOGL to draw a simple cube
// with each face being a different color.  Rotations
// can be applied with the arrow keys, the page up
// key and the page down key.  The home key will set
// all rotations to 0.

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.*;
import java.awt.*;

import static com.jogamp.opengl.GL.GL_MULTISAMPLE;

public class CubeRenderer extends GLJPanel implements GLEventListener {

    public static void main(String[] args) {
        JFrame window = new JFrame("JOGL Scene");
        GLCapabilities caps = new GLCapabilities(null);
        CubeRenderer panel = new CubeRenderer(caps);
        window.setContentPane(panel);
        window.pack();
        window.setLocation(50,50);
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
        panel.requestFocusInWindow();
    }

    private final float rotateX;
    private final float rotateY;
    private final float rotateZ;   // rotation amounts about axes
    private int texture;

    // Correct orientation -45.0, 150.0, 90.0
    public CubeRenderer(GLCapabilities capabilities) {
        super(capabilities);
        setPreferredSize( new Dimension(500,500) );
        addGLEventListener(this);
        rotateX = -45.0f;
        rotateY = 150.0f;
        rotateZ = 90.0f;
    }

    // ----------------- define some drawing methods for this program ----------

    private void square(GL2 gl, float r, float g, float b) {
        gl.glColor3f(r,g,b);         // The color for the square.
        gl.glTranslatef(0,0,0.5f);    // Move square 0.5 units forward.
        gl.glNormal3f(0,0,1);        // Normal vector to square (this is actually the default).
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glVertex2f(-0.5f,-0.5f);    // Draw the square (before the
        gl.glVertex2f(0.5f,-0.5f);     //   the translation is applied)
        gl.glVertex2f(0.5f,0.5f);      //   on the xy-plane, with its
        gl.glVertex2f(-0.5f,0.5f);     //   at (0,0,0).
        gl.glEnd();
    }

    private void cube(GL2 gl) {


        gl.glPushMatrix();
        gl.glRotatef(180,0,1,0); // rotate square to back face
        square(gl,0,1,1);        // back face is cyan
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glRotatef(-90,0,1,0); // rotate square to left face
        square(gl,0,1,0);        // left face is green
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glRotatef(-90,1,0,0); // rotate square to top face
        square(gl,0,0,1);        // top face is blue
        gl.glPopMatrix();
    }

    // ---------------  Methods of the GLEventListener interface -----------

    public void display(GLAutoDrawable drawable) {
        // called when the panel needs to be drawn

        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0,0,0,0);
        gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

        gl.glMatrixMode(GL2.GL_PROJECTION);  // Set up the projection.
        gl.glLoadIdentity();
        gl.glOrtho(-1,1,-1,1,-2,2);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glLoadIdentity();             // Set up modelview transform.
        gl.glRotatef(rotateZ,0,0,1);
        gl.glRotatef(rotateY,0,1,0);
        gl.glRotatef(rotateX,1,0,0);

        cube(gl);

    }

    public void init(GLAutoDrawable drawable) {
        // called when the panel is created
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.8F, 0.8F, 0.8F, 1.0F);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glEnable(GL_MULTISAMPLE);
    }

    public void dispose(GLAutoDrawable drawable) {
        // called when the panel is being disposed
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // called when user resizes the window
    }

}