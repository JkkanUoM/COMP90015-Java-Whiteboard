package server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import javax.net.ServerSocketFactory;
import client.Drawable;

public class Server {

    // Declare the port number
    private static int port = 4321;

    // Identifies the user number connected
    private static int counter = 0;
    private static final ArrayList<Drawable> drawables = new ArrayList<>();
    private static final ArrayList<ObjectOutputStream> clients = new ArrayList<>();
    
    public static void main(String[] args)
    {
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try(ServerSocket server = factory.createServerSocket(port)){
            System.out.println("Waiting for client connection..");

            // Wait for connections.
            while(true){
                Socket client = server.accept();
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                clients.add(out);
                counter++;
                System.out.println("Client "+counter+": Applying for connection!");

                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                t.start();
                broadcastAll(out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serveClient(Socket client)
    {
        try(Socket clientSocket = client)
        {
        	ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        	
        	while(true) {
        		Drawable d = (Drawable) in.readObject();
        		System.out.println("Got another drawable from a client");
        		synchronized (drawables) {
        			drawables.add(d);
        			broadcast(d);
				}
        	}
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private static void broadcast(Drawable d) {
		for(ObjectOutputStream out : clients) {
			System.out.println("Broadcast");
			try {
				out.writeObject(d);
			} catch (SocketException e) {
                clients.remove(out);
            } catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    private static void broadcastAll (ObjectOutputStream out) {
        System.out.println("Syncing history drawings with new client");
        for (Drawable d : drawables) {
            try {
                out.writeObject(d);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
