package org.example.mail_server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;

public class ConnexionClient {
    public void startServer() {
        System.out.println("Finestra del socket server");
        try {
            ServerSocket s = new ServerSocket(8189);
            Socket incoming = s.accept( );
            try {
                ObjectInputStream inStream = new ObjectInputStream(incoming.getInputStream());
                OutputStream outStream = incoming.getOutputStream();

                PrintWriter out = new PrintWriter(outStream, true);

                out.println( "Hello! Waiting for data." );

                // echo client input
                Vector<Date> v = null;
                try {
                    v = ((Vector<Date>)inStream.readObject());
                } catch (ClassNotFoundException e) {System.out.println(e.getMessage());}

                if (v!=null)
                    for (int i=0; i<v.size(); i++) {
                        Date date = v.get(i);
                        System.out.println("Echo: " + date);
                        out.println("Echo: " + date);
                    }
            }
            finally {
                incoming.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
