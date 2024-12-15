package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class AudioRecord {

    private static final float RATE = 44100f;

    private static final AudioFormat.Encoding ENCODING = AudioFormat.Encoding.PCM_SIGNED;

    private static final int SAMPLE_SIZE = 16;

    private static final boolean BIG_ENDIAN = false;

    private static final int CHANNELS = 2;

    public static boolean running = false;

    public static void save(String path) throws IOException, LineUnavailableException {
        running = true;
        File file = new File(path);
        if (file.isDirectory()) {
            if (!file.exists()) file.mkdirs();
            file.createNewFile();
        }

        AudioFormat audioFormat = new AudioFormat(ENCODING, RATE, SAMPLE_SIZE, CHANNELS, (SAMPLE_SIZE / 8) * CHANNELS,
                RATE, BIG_ENDIAN);
        TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
        targetDataLine.open();
        targetDataLine.start();
        byte[] b = new byte[256];
        FileOutputStream os = new FileOutputStream(file);
        while (targetDataLine.read(b, 0, b.length) > 0 && running) os.write(b);
        os.close();
        targetDataLine.close();
    }
}