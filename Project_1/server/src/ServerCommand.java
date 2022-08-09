/**
 * @file    ServerCommand.java
 * @brief   {@code Runnable} that executes server commands from standard input
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.*;
import java.net.*;

/**
 * The {@code ServerCommand} class provides a method that runs in the
 * background, taking commands from standard input and executing them.
 */
public class ServerCommand
    implements Runnable {

    /* --- Instance Variables ----------------------------------------------- */

    /** command to execute */
    private String command;

    /** server socket */
    private final ServerSocket listener;

    /* --- Constructors ----------------------------------------------------- */

    /**
     * Creates and returns a new {@code ServerCommand} instance.
     *
     * @param command  command to execute
     * @param listener server socket 
     */
    public ServerCommand(String command, ServerSocket listener)
    {
        this.command = command;
        this.listener = listener;
    }

    /* --- Instance Methods ------------------------------------------------- */

    /** Executes commands entered onto standard input. */
    @Override
    public void run()
    {
        BufferedReader input =
            new BufferedReader(new InputStreamReader(System.in));

        while (!command.equals("exit")) {
            System.out.println("[Server] Enter command >");
            try {
                command = input.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            System.out.println("[Server] Shutting down...");
            listener.close();

            //System.exit(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
