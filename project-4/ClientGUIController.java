import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

import static java.lang.System.err;

public final class ClientGUIController {

    @FXML private Label outHeader;
    @FXML private ListView<String> out;
    @FXML private TextArea in;
    @FXML private Button enterButton;
    @FXML private Button recordButton;
    @FXML private Button playButton;
    @FXML private Button channelButton;
    @FXML private ListView<String> online;

    private Client client;
    private LinkedBlockingQueue<byte[]> voiceNoteBuffer;
    private Deque<Queue<byte[]>> voiceNotes;
    private boolean running;
    private boolean recording;
    private VoIP voip;
    private boolean inChannel;

    public void initialize(String serverHostName, int serverPort)
    {
        Client client = null;
        try {
            client = new Client(InetAddress.getByName(serverHostName),
                    serverPort, Client.generateRandomID());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.client = client;
        voiceNoteBuffer = new LinkedBlockingQueue<>();
        voiceNotes = new ConcurrentLinkedDeque<>();

        outHeader.setText("Host: " + client);
        online.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        in.requestFocus();

        new Thread(() -> {
            connect();
            running = true;
            while (running) {
                Message m = receive();
                try {
                    if (m.getAudio() != null) {
                        print(m);
                        voiceNotes.add(m.getAudio());
                        print("You have " + voiceNotes.size() +
                                " voice note(s)");
                        continue;
                    }
                    switch (m.getText()) {
                        case "client_list":
                            Platform.runLater(() ->
                                    online.getItems().setAll(m.getClientIDs()));
                            break;
                        case "channel_invite":
                            if (m.getSender().equals(this.client.toString())) {
                                voip = new VoIP(m.getGroup(),
                                        VoIP.DEFAULT_PORT);
                                break;
                            }
                            Platform.runLater(() -> {
                                Alert alert = new Alert(AlertType.CONFIRMATION);
                                alert.setResizable(true);
                                alert.getDialogPane()
                                    .setMinHeight(Region.USE_PREF_SIZE);
                                alert.setContentText(
                                        "Join channel started by " +
                                        m.getSender());
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == ButtonType.OK) {
                                        m.setText("join_channel");
                                        channelButton.setText("Exit channel");
                                        voip = new VoIP(m.getGroup(),
                                                VoIP.DEFAULT_PORT);
                                        inChannel = true;
                                    } else {
                                        m.setText("decline_channel");
                                    }
                                 });
                                m.setReceiver("server");
                                send(m);
                            });
                            break;
                        default:
                            print(m);
                            break;
                    }
                } catch (NullPointerException npe) {
                    // IGNORE: Server disconnected.
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @FXML
    private void enterButtonHandler()
    {
        String input = in.getText().strip();
        if (input.equals("")) {
            return;
        }
        List<String> selected = online.getSelectionModel().getSelectedItems();
        if (selected.size() > 0) {
            print("You > (To: " + String.join("; ", selected) + ") " + input);
            String text = "(" + (selected.size() == 1 ? "DM" : "BC") +
                ") " + input;
            selected.forEach(r -> {
                send(new Message().setReceiver(r).setText(text));
            });
        } else {
            print("You > " + input);
            send(new Message().setReceiver("all").setText(input));
        }
        online.getSelectionModel().clearSelection();
        in.clear();
    }

    @FXML
    private void recordButtonHandler()
    {
        if (!recording) {
            recordButton.setText("Send");
            new Thread(() -> {
                try (AudioIn ai = new AudioIn(VoIP.AUDIO_FORMAT)) {
                    recording = true;
                    while (recording) {
                        byte[] data =
                            new byte[(int) ai.getFormat().getSampleRate()];
                        ai.read(data);
                        voiceNoteBuffer.add(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            recordButton.setText("Record");
            recording = false;
            new Thread(() -> {
                Queue<byte[]> audio = new ArrayDeque<byte[]>();
                voiceNoteBuffer.drainTo(audio);
                List<String> selected =
                    online.getSelectionModel().getSelectedItems();
                if (selected.size() == 0) {
                    print("You > voice note");
                    send(new Message().setReceiver("all").setText("voice note")
                            .setAudio(audio));
                } else {
                    print("You > (To: " + String.join("; ", selected) + 
                            ") voice note");
                    String text = "(" + (selected.size() == 1 ? "DM" : "BC") +
                        ") voice note";
                    selected.forEach(r -> {
                        send(new Message().setReceiver(r).setText(text)
                                .setAudio(audio));
                    });
                }
                voiceNoteBuffer.clear();
                Platform.runLater(() -> {
                    online.getSelectionModel().clearSelection();
                });
            }).start();
        }
    }

    @FXML
    private void playButtonHandler()
    {
        if (voiceNotes.size() == 0) {
            print("You have no voice notes to play");
            return;
        }
        new Thread(() -> {
            try (AudioOut ao = new AudioOut(VoIP.AUDIO_FORMAT)) {
                print("Playing voice note...");
                voiceNotes.pop().forEach(b -> ao.write(b));
                print("You have " + voiceNotes.size() +
                        " voice note(s) remaining");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void channelButtonHandler()
    {
        if (!inChannel) {
            List<String> selected =
                online.getSelectionModel().getSelectedItems();
            if (selected.size() == 0) {
                print("Failed. First select other clients to invite");
            } else if (selected.contains(client.toString())) {
                print("Failed. Cannot send channel invitation to yourself");
            } else {
                send(new Message().setReceiver("list").setText("start_channel")
                        .setClientIDs(selected.stream().toList()));
                channelButton.setText("Exit channel");
                inChannel = true;
            }
        } else {
            send(new Message().setReceiver("server").setText("exit_channel"));
            exitChannel();
        }
        online.getSelectionModel().clearSelection();
    }

    private void exitChannel()
    {
        Platform.runLater(() -> {
            channelButton.setText("Start channel");
        });
        if (voip != null) {
            voip.close();
            voip = null;
        }
        inChannel = false;
    }

    private void send(Message m)
    {
        client.send(m);
    }

    private Message receive()
    {
        Message m = null;
        try {
            m = client.receive();
        } catch (EOFException eofe) {
            print("Lost connection to the server");
            exitChannel();
            client.close();
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }

    private void connect()
    {
        disableButtons(true);
        print("Attempting to connect to the server...");
        while (!client.isConnected()) {
            try {
                client.connect();
            } catch (IOException ioe) {
                try {
                    Thread.currentThread().sleep(2500);
                } catch (InterruptedException ie) {
                    // IGNORE
                }
            }
        }
        print("Connection successful");
        disableButtons(false);
    }

    private void disableButtons(boolean b)
    {
        Platform.runLater(() -> {
            enterButton.setDisable(b);
            recordButton.setDisable(b);
            playButton.setDisable(b);
            channelButton.setDisable(b);
        });
    }

    private void print(Message m)
    {
        print(m.getSender() + " > " + m.getText());
    }

    private void print(String text)
    {
        Platform.runLater(() -> {
            out.getItems().add(text);
            out.scrollTo(out.getItems().size() - 1);
        });
    }

    public void exit()
    {
        running = false;
        exitChannel();
        client.close();
        Platform.exit();
        System.exit(0);
    }
}
