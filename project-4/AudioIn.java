import javax.sound.sampled.*;
import static javax.sound.sampled.DataLine.Info;

public final class AudioIn
    implements AutoCloseable {

    private final AudioFormat format;
    private final TargetDataLine line;

    public AudioIn(AudioFormat format)
        throws LineUnavailableException
    {
        this.format = format;
        Info info = new Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
    }

    public void read(byte[] data)
    {
        line.read(data, 0, data.length);
    }

    public AudioFormat getFormat()
    {
        return format;
    }

    @Override
    public void close()
    {
        line.drain();
        line.stop();
        line.close();
    }
}
