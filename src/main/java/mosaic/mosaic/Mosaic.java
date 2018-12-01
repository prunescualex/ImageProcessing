package mosaic.mosaic;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alex.prunescu on 10/11/2018.
 */

public class Mosaic {

    private static final String TILES_DIR = "./src/main/resources/resize_output";
    private static final String INPUT_IMG = "./src/main/resources/r100-logo.png";
    private static final String OUTPUT_IMG = "./src/main/resources/output101.png";

    //60 60 12
    private static final int TILE_WIDTH = 100;
    private static final int TILE_HEIGHT = 100;
    //14
    private static final int TILE_SCALE = 4;
    private static final boolean IS_BW = false;
    private static final int THREADS = 4;

    private static void log(String msg){
        System.out.println(msg);
    }

    public static void main(String[] args) throws IOException, InterruptedException{


        long heapSize = Runtime.getRuntime().totalMemory();
        System.out.println("heapSize " + heapSize);

        long heapMaxSize = Runtime.getRuntime().maxMemory();
        System.out.println("heapMax " + heapMaxSize);

        long heapFreeSize = Runtime.getRuntime().freeMemory();
        System.out.println("heapFree " + heapFreeSize);

        long startTime = System.nanoTime();
        log("Reading tiles...");
        final Collection<Tile> tileImages = getImagesFromTiles(new File(TILES_DIR));

        log("Processing input image...");
        File inputImageFile = new File(INPUT_IMG);
        Collection<BufferedImagePart> inputImageParts = getImagesFromInput(inputImageFile);
        final Collection<BufferedImagePart> outputImageParts = Collections.synchronizedSet(new HashSet<BufferedImagePart>());

        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(THREADS);

        final AtomicInteger i = new AtomicInteger();
        final int partCount = inputImageParts.size();
        for (final BufferedImagePart inputImagePart : inputImageParts) {
            newFixedThreadPool.execute(new Runnable(){
                public void run() {
                    Tile bestFitTile = getBestFitTile(inputImagePart.image, tileImages);
                    log(String.format("Matching part %s of %s", i.incrementAndGet(), partCount));
                    outputImageParts.add(new BufferedImagePart(bestFitTile.image, inputImagePart.x, inputImagePart.y));
                }
            });
        }

        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(10000000, TimeUnit.SECONDS);

        log("Writing output image...");
        BufferedImage inputImage = ImageIO.read(inputImageFile);
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage output = makeOutputImage(width, height, outputImageParts);
        ImageIO.write(output, "png", new File(OUTPUT_IMG));
        log("FINISHED");
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println(totalTime);
        System.out.println(TimeUnit.NANOSECONDS.toSeconds(totalTime));
    }

    private static BufferedImage makeOutputImage(int width, int height, Collection<BufferedImagePart> parts){
        BufferedImage image = new BufferedImage(width * TILE_SCALE, height * TILE_SCALE, BufferedImage.TYPE_4BYTE_ABGR);

        for(BufferedImagePart part : parts){

            BufferedImage imagePart = image.getSubimage(part.x * TILE_SCALE, part.y * TILE_SCALE, TILE_WIDTH, TILE_HEIGHT);
            imagePart.setData(part.image.getData());

        }

        return image;
    }

    /**
     * Get fit of tile from input image
     * @param target
     * @param tiles
     * @return
     */
    private static Tile getBestFitTile(BufferedImage target, Collection<Tile> tiles) {
        Tile bestFit = null;
        int bestFitScore = -1;

        for(Tile tile : tiles){
            int score = getScore(target, tile);
            if (score > bestFitScore){
                bestFitScore = score;
                bestFit = tile;
            }
        }

        return bestFit;
    }


    /**
     * Get score
     * @param target
     * @param tile
     * @return
     */
    private static int getScore(BufferedImage target, Tile tile){
        assert target.getHeight() == Tile.SCALED_HEIGHT;
        assert target.getWidth() == Tile.SCALED_WIDTH;

        int total = 0;
        for(int x = 0; x<Tile.SCALED_WIDTH; x++){
            for(int y = 0; y<Tile.SCALED_HEIGHT; y++){
                int targetPixel = target.getRGB(x, y);
                Pixel candidatePixel = tile.pixels[x][y];
                int diff = getDiff(targetPixel, candidatePixel);
                int score;
                if (IS_BW){
                    score = 255 - diff;
                } else {
                    score = 255 * 3 - diff;
                }

                total += score;
            }
        }

        return total;
    }

    private static int getDiff(int target, Pixel candidate){
        if (IS_BW){
            return Math.abs(getRed(target) - candidate.r);
        } else {
            return Math.abs(getAlpha(target)-candidate.a)+
                    Math.abs(getRed(target) - candidate.r) +
                    Math.abs(getGreen(target) - candidate.g) +
                    Math.abs(getBlue(target) - candidate.b);
        }
    }

    private static int getAlpha(int pixel){
        return ( pixel >>> 32 ) & 0xff;
    }

    private static int getRed(int pixel){
        return  (pixel >>> 16) & 0xff;
    }

    private static int getGreen(int pixel){
        return  (pixel >>> 8) & 0xff;
    }

    private static int getBlue(int pixel){
        return  pixel & 0xff;
    }

    /**
     * Read tiles
     * @param tilesDir
     * @return
     * @throws IOException
     */
    private static Collection<Tile> getImagesFromTiles(File tilesDir) throws IOException{
        Collection<Tile> tileImages = Collections.synchronizedSet(new HashSet<Tile>());
        File[] files = tilesDir.listFiles();
        for(File file : files){
            BufferedImage img = ImageIO.read(file);
            if (img != null){
                tileImages.add(new Tile(img));
            } else {
                System.err.println("null image for file " + file.getName() + file.exists());
            }
        }
        return tileImages;
    }

    /**
     * Split input file in scaled down images
     * @param inputImgFile
     * @return
     * @throws IOException
     */
    private static Collection<BufferedImagePart> getImagesFromInput(File inputImgFile) throws IOException{
        Collection<BufferedImagePart> parts = new HashSet<BufferedImagePart>();

        BufferedImage inputImage = ImageIO.read(inputImgFile);
        int totalHeight = inputImage.getHeight();
        int totalWidth = inputImage.getWidth();

        int x=0, y=0, w=Tile.SCALED_WIDTH, h=Tile.SCALED_HEIGHT;
        while(x+w <= totalWidth){
            while(y+h <= totalHeight){
                BufferedImage inputImagePart = inputImage.getSubimage(x, y, w, h);
                parts.add(new BufferedImagePart(inputImagePart, x, y));
                y+=h;
            }
            y=0;
            x+= w;
        }

        return parts;
    }

    /**
     * Repr of tile
     */
    public static class Tile {
        public static int SCALED_WIDTH = TILE_WIDTH / TILE_SCALE;
        public static int SCALED_HEIGHT = TILE_HEIGHT / TILE_SCALE;
        public Pixel[][] pixels = new Pixel[SCALED_WIDTH][SCALED_HEIGHT];
        public BufferedImage image;

        public Tile(BufferedImage image) {
            this.image = image;
            calcPixels();
        }

        private void calcPixels(){
            for(int x=0; x<SCALED_WIDTH; x++){
                for(int y=0; y<SCALED_HEIGHT; y++){
                    pixels[x][y] = calcPixel(x * TILE_SCALE, y * TILE_SCALE, TILE_SCALE, TILE_SCALE);
                }
            }
        }

        private Pixel calcPixel(int x, int y, int w, int h){
            int aTotal = 0, redTotal = 0, greenTotal = 0, blueTotal = 0;

            for(int i=0; i<w; i++){
                for(int j=0; j<h; j++){
                    /*
                    int rgb = image.getRGB(x+i, y+j);
                    aTotal   += getAlpha(rgb);
                    redTotal   += getRed(rgb);
                    greenTotal += getGreen(rgb);
                    blueTotal  += getBlue(rgb);*/

                    Color mycolor = new Color(image.getRGB(x+i, y+j));
                    aTotal += mycolor.getAlpha();
                    redTotal += mycolor.getRed();
                    greenTotal += mycolor.getGreen();
                    blueTotal += mycolor.getBlue();
                }
            }
            int count = w*h;
            return new Pixel(aTotal/count,redTotal/count, greenTotal/count, blueTotal/count);
        }
    }

    public static class BufferedImagePart{
        public BufferedImagePart(BufferedImage image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }

        public BufferedImage image;
        public int x;
        public int y;
    }

    public static class Pixel{
        public int a,r,g,b;

        public Pixel(int a,int r, int g, int b) {
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
        }
        @Override
        public String toString() {
            return a + "."+ r + "." + g + "." + b;
        }
    }
}