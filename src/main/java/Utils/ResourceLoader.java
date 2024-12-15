package Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ResourceLoader {
    public static BufferedImage loadImage(String path) {
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream != null) {
                return ImageIO.read(inputStream);
            } else {
                System.err.println("Not found Image ：" + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); // return a transparent image if failed
    }
}