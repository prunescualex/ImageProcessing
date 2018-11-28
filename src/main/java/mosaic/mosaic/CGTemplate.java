package mosaic.mosaic;

/**
 * Created by alex.prunescu on 28/11/2018.
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;

/** Custom Drawing Code Template */
// A Swing application extends javax.swing.JFrame
public class CGTemplate extends JFrame {
    // Define constants
    public static final int CANVAS_WIDTH  = 16000;
    public static final int CANVAS_HEIGHT = 12000;

    // Declare an instance of the drawing canvas,
    // which is an inner class called DrawCanvas extending javax.swing.JPanel.
    private static DrawCanvas canvas;

    // Constructor to set up the GUI components and event handlers
    public CGTemplate() {
        canvas = new DrawCanvas();    // Construct the drawing canvas
        canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

        // Set the Drawing JPanel as the JFrame's content-pane
        Container cp = getContentPane();
        cp.add(canvas);
        // or "setContentPane(canvas);"

        setDefaultCloseOperation(EXIT_ON_CLOSE);   // Handle the CLOSE button
        pack();              // Either pack() the components; or setSize()
        setTitle("......");  // "super" JFrame sets the title
        setVisible(true);    // "super" JFrame show
    }

    /**
     * Define inner class DrawCanvas, which is a JPanel used for custom drawing.
     */
    private class DrawCanvas extends JPanel {
        // Override paintComponent to perform your own painting
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);     // paint parent's background
            setBackground(Color.black);
            // set opaque to false - background not drawn
            setOpaque(false);

            // Drawing primitive shapes

            g.drawLine(30, 40, 100, 200);

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            MediaTracker tracker = new MediaTracker(this);
            Image image = toolkit.getImage("./src/main/resources/resize_output/output1.jpeg");
            tracker.addImage(image, 0);
            try {
                tracker.waitForAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ImageObserver observer = null;
            g.drawImage(image,50,50,observer );
            g.drawImage(image,50,150,observer );
            g.drawImage(image,50,250,observer );
            g.drawImage(image,50,350,observer );
            g.drawImage(image,50,450,observer );

        }


    }

    private static void saveImage(){
        BufferedImage imagebuf=null;;
        try {
            imagebuf = new Robot().createScreenCapture(canvas.bounds());
        } catch (AWTException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Graphics2D graphics2D = imagebuf.createGraphics();
        canvas.paint(graphics2D);
        try {
            ImageIO.write(imagebuf,"jpeg", new File("./src/main/resources/save1.png"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("error");
        }
    }

    // The entry main method
    public static void main(String[] args) {
        // Run the GUI codes on the Event-Dispatching thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CGTemplate(); // Let the constructor do the job
                saveImage();
            }
        });
    }
}