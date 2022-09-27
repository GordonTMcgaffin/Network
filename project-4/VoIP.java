import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public final class VoIP {

    public static final int DEFAULT_VOIP_PORT = 9090;
    public static final int AUDIO_SAMPLE_RATE = 8000;
    public static final int AUDIO_SAMPLE_SIZE = 16;
    public static final int AUDIO_CHANNELS = 1;
    public static final boolean AUDIO_SIGNED = true;
    public static final boolean AUDIO_BIG_ENDIAN = false;
    public static final AudioFormat AUDIO_FORMAT =
        new AudioFormat(AUDIO_SAMPLE_RATE, AUDIO_SAMPLE_SIZE, AUDIO_CHANNELS,
                AUDIO_SIGNED, AUDIO_BIG_ENDIAN);

    private final VoIPIn in;
    private final VoIPOut out;

    public VoIP(InetAddress group, int port)
    {
        in = new VoIPIn(group, port, AUDIO_FORMAT);
        out = new VoIPOut(group, port, AUDIO_FORMAT);
    }

    public void close()
    {
        in.close();
        out.close();
    }
}
