import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import static java.lang.System.err;

public final class VoIPOut {

    private boolean listening;

    public VoIPOut(InetAddress group, int port, AudioFormat format)
    {
        new Thread(() -> {
            try (AudioOut out = new AudioOut(format);
                    MulticastSocket socket = new MulticastSocket(port)) {
                socket.setReuseAddress(true);
                socket.joinGroup(group);
                byte[] data = new byte[(int) format.getSampleRate()];
                listening = true;
                while (listening) {
                    DatagramPacket packet =
                        new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    if (NetworkInterface
                            .getByInetAddress(packet.getAddress()) == null) {
                        out.write(data);
                    }
                }
                socket.leaveGroup(group);
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
