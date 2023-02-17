package com.sivannsan.millidb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public final class MilliDBServer {
    private static ServerSocket server;

    private MilliDBServer() {
    }

    public static void main(String[] args) {
        MilliDBLogs.makeNewFile();
        MilliDBLogs.info("Starting the server...");
        MilliDBConfigurations.load();
        MilliDBFiles.load();
        listenToCommands();
        listenToClients();
    }

    private static void saveAll() {
        MilliDBLogs.save();
    }

    public static void shutdown() {
        MilliDBLogs.info("The server will stop in 5 seconds...");
        saveAll();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, 1000L * 5);
    }

    private static void onCommand(String command) {
        if (command.equals("help") || command.equals("h") || command.equals("?")) {
            MilliDBLogs.info("Command help:");
            MilliDBLogs.info("- help - Show all commands");
            MilliDBLogs.info("- connections - Show all services or client connections");
            MilliDBLogs.info("- save-all - Save everything");
            MilliDBLogs.info("- stop - Stop the server");
            return;
        }
        if (command.equals("connections") || command.equals("c")) {
            MilliDBLogs.info("Connections:");
            for (MilliDBService service : MilliDBService.Holder.getAll()) {
                MilliDBLogs.info("- " + service.getIP());
            }
            return;
        }
        if (command.equals("save-all")) {
            saveAll();
            MilliDBLogs.info("Saved");
            return;
        }
        if (command.equals("shutdown") || command.equals("stop") || command.equals("s")) {
            shutdown();
            return;
        }
        MilliDBLogs.warning("Unknown command: " + command);
    }

    private static void listenToCommands() {
        new Thread(() -> {
            while (true) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    onCommand(reader.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void listenToClients() {
        new Thread(() -> {
            try {
                server = new ServerSocket(MilliDBConfigurations.getPort());
                MilliDBLogs.info("The server started. Listening on port '" + MilliDBConfigurations.getPort() + "'");
                while (true) {
                    Socket socket = server.accept();
                    MilliDBService service = new MilliDBService(socket);
                    MilliDBService.Holder.add(service);
                    service.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
