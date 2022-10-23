package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ServerSocketFactory;

import client.Chat;
import client.Drawable;
import client.Info;
import client.Message;

public class Server {

    // Declare the port number
    private static int port = 4321;

    private static final ArrayList<Drawable> drawables = new ArrayList<>();
    private static final HashMap<Info, ObjectOutputStream> clients = new HashMap<>();
    private static final ArrayList<Thread> threads = new ArrayList<>();
    private static Socket manager;
    private static ObjectOutputStream managerOut;
    private static ObjectInputStream managerIn;
    private static ServerSocket server;
    
    public static void main(String[] args)
    {
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
            while (true) {
                Socket client = server.accept();
                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                threads.add(t);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serveClient(Socket client) {
        try {
            ObjectInputStream in = client == manager ? managerIn : new ObjectInputStream(client.getInputStream());
            ObjectOutputStream out = client == manager ? managerOut : new ObjectOutputStream(client.getOutputStream());

            String username = (String) in.readObject();
            System.out.println(username + " connected");

            Info info = client == manager ? new Info(username, Info.IN) : new Info(username, Info.JOINED);
            managerOut.writeObject(info);
            clients.put(info, out);

        	while (true) {
                Message message = (Message) in.readObject();
                Info inf = message.getInfo();
                Drawable d = message.getDrawable();
                Chat c = message.getChat();

                // Verify acceptance from manager
                if (info.getAction() == Info.JOINED) {
                    if (inf != null && inf.getUsername().equals(username) && inf.getAction() == Info.IN) {
                        info.setAction(Info.IN);
                        syncHistory(out);
                    }
                    continue;
                }

                if (inf != null && inf.getAction() == Info.LEFT) {
                    System.out.println("Client left");
                    in.close();
                    out.close();
                    client.close();
                    clients.remove(info);
                    broadcast(message);

                    if (client == manager) {
                        close();
                        break;
                    }
                }

                if (d != null) {
                    System.out.println("Got another drawable from a client");
                    synchronized (drawables) {
                        drawables.add(d);
                        broadcast(message);
                    }
                }
                // check for chat
                // broadcast chat

        	}
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private static void broadcast(Message d) {
		for(Info info : clients.keySet()) {
            if (info.getAction() != Info.IN) {
                continue;
            }
			System.out.println("Broadcast");
            ObjectOutputStream out = clients.get(info);
			try {
				out.writeObject(d);
			} catch (SocketException e) {
                clients.remove(info, out);
            } catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    private static void syncHistory (ObjectOutputStream out) {
        System.out.println("Syncing history drawings with new client");
        for (Drawable d : drawables) {
            try {
                out.writeObject(new Message(d));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void close() {
        try {
            for (Thread t : threads) {
                t.interrupt();
                t.join();
            }
            server.close();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
