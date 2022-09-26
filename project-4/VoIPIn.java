import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public final class VoIPIn {

    private final InetAddress group;
    private final int port;
    private final AudioFormat format;
    private boolean listening;

    public VoIPIn(InetAddress group, int port, AudioFormat format)
    {
        this.group = group;
        this.port = port;
        this.format = format;
    }

    public void open()
    {
        new Thread(() -> {
            try (AudioIn in = new AudioIn(format);
                    DatagramSocket socket = new DatagramSocket()) {
                byte[] data = new byte[(int) format.getSampleRate()];
                listening = true;
                while (listening) {
                    in.read(data);
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, group, port);
                    socket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close()
    {
        listening = false;
    }
}
