package mosaic.mosaic;

/**
 * Created by alex.prunescu on 01/12/2018.
 */
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Convertor {


    public static void listFilesForFolder(final File folder) throws IOException {
        int i=0;
        String outputPath = "./src/main/resources/resize_output/output";
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {

                // listFilesForFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
                BufferedImage bufferedImage = ImageIO.read(fileEntry);

                // create a blank, RGB, same width and height, and a white background
                BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                        bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);

                // write to jpeg file
                ImageIO.write(newBufferedImage, "jpg", new File(outputPath + String.valueOf(i) + ".jpg"));

                i++;
                System.out.println("Done");
            }
        }
    }

    public static void main(String[] args) {

        try {
            listFilesForFolder(new File("./src/main/resources/forced_tiles"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}