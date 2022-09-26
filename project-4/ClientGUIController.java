import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import java.util.concurrent.*;

import static java.lang.System.err;

public final class ClientGUIController
    implements Initializable {

    @FXML private Label outHeader;
    @FXML private ListView<String> out;
    @FXML private TextArea in;
    @FXML private Button enterButton;
    @FXML private Button voiceNoteButton;
    @FXML private Button channelButton;
    @FXML private ListView<String> online;

    private boolean running;
    private Client client;
    private boolean inChannel;
    private boolean recording;
    private LinkedBlockingQueue<byte[]> voiceNoteBuffer;

    @Override
    public void initialize(URL url, ResourceBundle resources)
    {
        online.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        in.requestFocus();

        // TODO: Try assigning the thread to a variable and using suspend() and
        // continue() to manage its execution instead of the running variable.
        new Thread(() -> {
            try {
                client = new Client(InetAddress.getByName("25.52.211.56"),
                        Client.DEFAULT_SERVER_PORT, Client.generateRandomID());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> outHeader.setText("Host: " + client));
            connect();
            running = true;
            while (running) {
                Message m = receive();
                err.println("here");
                try {
                    if (m.getAudio() != null) {
                        new Thread(() -> {
                            try (AudioOut ao = new AudioOut(VoIP.AUDIO_FORMAT)) {
                                print(m);
                                m.getAudio().forEach(b -> ao.write(b));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                        return;
                    }
                    switch (m.getText()) {
                        case "client_list":
                            Platform.runLater(() ->
                                    online.getItems().setAll(m.getClientIDs()));
                            break;
                        case "channel_invite":
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
            String text = "(" + ((selected.size() > 1) ? "BC" : "DM") +
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
    private void voiceNoteButtonHandler()
    {
        if (!recording) {
            voiceNoteButton.setText("Send");
            new Thread(() -> {
                try (AudioIn ai = new AudioIn(VoIP.AUDIO_FORMAT)) {
                    voiceNoteBuffer = new LinkedBlockingQueue<>();
                    recording = true;
                    while (recording) {
                        byte[] data =
                            new byte[(int) ai.getFormat().getSampleRate()];
                        ai.read(data);
                        voiceNoteBuffer.add(data);
                    }
                } catch (Exception e) {
                    // TODO: Make exception catching more nuanced throughout
                    // entire project.
                    e.printStackTrace();
                }
            }).start();
        } else {
            voiceNoteButton.setText("Record");
            recording = false;
            new Thread(() -> {
                List<String> selected =

                    online.getSelectionModel().getSelectedItems();
                List<byte[]> audio = new ArrayList<byte[]>();
                    voiceNoteBuffer.drainTo(audio);
                send(new Message().setReceiver(selected.get(0))
                        .setText("voice note").setAudio(audio));
                err.println("done");
            }).start();
        }
    }

    @FXML
    private void channelButtonHandler()
    {
        if (!inChannel) {
            List<String> selected =
                online.getSelectionModel().getSelectedItems();
            if (selected.size() == 0) {
                print("Failed. First select other clients to invite.");
            } else if (selected.contains(client.toString())) {
                print("Failed. Cannot send channel invitation to yourself.");
            } else {
                send(new Message().setReceiver("list").setText("start_channel")
                        .setClientIDs(selected.stream().toList()));
                channelButton.setText("Exit channel");
                inChannel = true;
            }
        } else {
            send(new Message().setReceiver("server").setText("exit_channel"));
            resetChannelButton();
        }
        online.getSelectionModel().clearSelection();
    }

    private void resetChannelButton()
    {
        Platform.runLater(() -> {
            channelButton.setText("Start channel");
            inChannel = false;
        });
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
            print("Lost connection to the server.");
            resetChannelButton();
            connect();
        } catch (Exception e) {
            exit();
        }

        return m;
    }

    private void connect()
    {
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
        print("Connection successful.");
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
        client.close();
        Platform.exit();
        System.exit(0);
    }
}
