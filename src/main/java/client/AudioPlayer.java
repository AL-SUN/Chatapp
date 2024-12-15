package client;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {
    public static void playAndDelete(String filePath) {
        new Thread(() -> {
            try {
                //System.out.printf("hello, path:%s\n", filePath);
                AudioInputStream as = AudioSystem.getAudioInputStream(new File(filePath));
                AudioFormat format = as.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
                sdl.open(format);
                sdl.start();
                int nBytesRead = 0;
                byte[] abData = new byte[512];
                while (nBytesRead != -1) {
                    nBytesRead = as.read(abData, 0, abData.length);
                    if (nBytesRead >= 0)
                        sdl.write(abData, 0, nBytesRead);
                }
                sdl.drain();
                sdl.close();
                as.close();
                File file = new File(filePath);
                file.delete();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }
}