import javax.sound.sampled.*;

public class AudioOut
    implements AutoCloseable {

    private final SourceDataLine line;

    public AudioOut(AudioFormat format)
        throws LineUnavailableException
    {
        line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
    }

    public void write(byte[] data)
    {
        line.write(data, 0, data.length);
    }

    @Override
    public void close()
    {
        line.drain();
        line.stop();
        line.close();
    }
}
