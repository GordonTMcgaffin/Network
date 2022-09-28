import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

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
                InetAddress localhost = InetAddress.getLocalHost();
                listening = true;
                while (listening) {
                    DatagramPacket packet =
                        new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    // This may be a problem area.
                    if (!packet.getAddress().equals(localhost)) {
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
