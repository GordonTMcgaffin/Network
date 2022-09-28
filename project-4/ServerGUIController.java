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

public final class ServerGUIController {
    
    @FXML private ListView<String> out;
    @FXML private ListView<String> online;

    private Server server;
    private Client client;
    private boolean running;

    public void initialize(int port)
    {
        server = new Server(port);
        Client client = null;
        try {
            client = new Client(InetAddress.getByName("localhost"),
                    Client.DEFAULT_SERVER_PORT, "server");
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.client = client;

        new Thread(() -> {
            connect();
            running = true;
            while (running) {
                try {
                    Message m = receive();
                    if (m.getText().equals("client_list")) {
                        Platform.runLater(() ->
                                online.getItems().setAll(m.getClientIDs()));
                    } else {
                        print(m.getText());
                    }
                } catch (NullPointerException npe) {
                    // IGNORE: Server disconnected.
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Message receive()
    {
        Message m = null;
        try {
            m = client.receive();
        } catch (EOFException eofe) {
            print("Lost connection to the server.");
            connect();
        } catch (Exception e) {
            exit();
        }
        return m;
    }

    private void connect()
    {
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
    }

    private void print(Message m)
    {
        print(m.getText());
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
        server.close();
        Platform.exit();
        System.exit(0);
    }
}
