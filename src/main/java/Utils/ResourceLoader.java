package Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Properties;

public class ResourceLoader {
    public static BufferedImage loadImage(String path) {
        try (InputStream inputStream = loadResource(path)) {
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

    // A simple method to load a resource file
    public static InputStream loadResource(String path) {
        return ResourceLoader.class.getClassLoader().getResourceAsStream(path);
    }

    public static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = loadResource("config.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
                return properties;
            } else {
                System.err.println("Not found config.properties in resources");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}