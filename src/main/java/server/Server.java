package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ServerSocketFactory;

import client.*;

public class Server {

    // Declare the port number
    private static int port = 4321;

    private static final ArrayList<Drawable> drawables = new ArrayList<>();
    private static final HashMap<String, ObjectOutputStream> clients = new HashMap<>();
    private static final HashMap<String, Info> infos = new HashMap<>();
    private static final ArrayList<Thread> threads = new ArrayList<>();
    private static Socket manager;
    private static ObjectOutputStream managerOut;
    private static ObjectInputStream managerIn;
    private static ServerSocket server;

    public static void main(String[] args) {
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number");
                return;
            }
        }

        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try {
            server = factory.createServerSocket(port);
            System.out.println("Waiting for manager connection..");

            manager = server.accept();
            managerIn = new ObjectInputStream(manager.getInputStream());
            managerOut = new ObjectOutputStream(manager.getOutputStream());
            Thread m = new Thread(() -> serveClient(manager));
            threads.add(m);
            m.start();

            System.out.println("Waiting for client connection..");

            // Wait for connections.
            while (!server.isClosed()) {
                Socket client = server.accept();
                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                threads.add(t);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            for (Thread t : threads) {
                t.interrupt();
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void serveClient(Socket client) {
        try {
            ObjectInputStream in = client == manager ? managerIn : new ObjectInputStream(client.getInputStream());
            ObjectOutputStream out = client == manager ? managerOut : new ObjectOutputStream(client.getOutputStream());

            String username = (String) in.readObject();
            System.out.println(username + " connected");

            Info info = client == manager ? new Info(username, Info.IN) : new Info(username, Info.JOINED);
            info.isManager = client == manager;

            if (clients.containsKey(username)) {
                info.setAction(Info.LEFT);
                out.writeObject(new Message(info));
                in.close();
                out.close();
                client.close();
                return;
            }

            managerOut.writeObject(new Message(info));
            clients.put(username, out);
            infos.put(username, info);

            while (true) {
                Message message = (Message) in.readObject();
                Info inf = message.getInfo();
                Drawable d = message.getDrawable();
                Chat c = message.getChat();

                // Verify acceptance from manager
                if (info.getAction() == Info.JOINED) {
                    continue;
                }

                if (client == manager) {
                    FileRequest req = message.getFileRequest();
                    if (req != null) {
                        if (req.getString().equals("new")) {
                            broadcast(message);
                            drawables.clear();
                        }
                    }
                }

                if (inf != null) {
                    String un = inf.getUsername();
                    int act = inf.getAction();

                    if (act == Info.IN) {
                        syncHistory(clients.get(un));
                        infos.get(un).setAction(Info.IN);
                        broadcast(message);
                    }
                    if (act == Info.LEFT) {
                        broadcast(message);
                        clients.remove(un);
                        infos.remove(un);

                        if (inf.getUsername().equals(info.getUsername())) {
                            System.out.println("Client left");
                            in.close();
                            out.close();
                            client.close();

                            if (client == manager) {
                                System.out.println("Left client is manager");
                                server.close();
                            }
                            break;
                        }
                    }
                }

                if (d != null) {
                    System.out.println("Got another drawable from a client");
                    synchronized (drawables) {
                        drawables.add(d);
                        broadcast(message);
                    }
                } else if (c != null) {
                    c.setMessage(username + "> " + c.getMessage());
                    broadcast(message);
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(Message d) {
        for (String un : clients.keySet()) {
            Info info = infos.get(un);
            if (info.getAction() != Info.IN) {
                continue;
            }

            ObjectOutputStream out = clients.get(un);
            try {
                out.writeObject(d);
            } catch (IOException e) {
                clients.remove(un);
                infos.remove(un);
            }
        }
    }

    private static void syncHistory(ObjectOutputStream out) {
        System.out.println("Syncing history drawings with new client");
        for (String un : clients.keySet()) {
            Info inf = infos.get(un);
            try {
                if (inf.getAction() == Info.IN) {
                    out.writeObject(new Message(inf));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Drawable d : drawables) {
            try {
                out.writeObject(new Message(d));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
