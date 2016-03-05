package com.uom.cse.distsearch.util;

import javax.rmi.CORBA.Util;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.Socket;

/**
 * @author gobinath
 */
public class Utility {
    private Utility() {
    }

    public static String sendTcpToBootstrapServer(String message, String ip, int port) throws IOException {
        Socket clientSocket = new Socket(ip, port);
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        //DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //sentence = inFromUser.readLine();
        out.print(message.trim());
        out.flush();
        //outToServer.flush();
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = inFromServer.readLine()) != null) {
            builder.append(line);
        }
        String response = builder.toString().replace("\r\n", " ").replace('\n', ' ').replace("", "");
        System.out.println("FROM SERVER: " + response);
        clientSocket.close();

        return response;
    }


    public static void post(final String url, final Object object) {
        new Thread() {
            @Override
            public void run() {
                try {
                    WebTarget target = ClientBuilder.newClient().target(url);
                    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                    Response response = builder.post(Entity.json(object));
                    int status = response.getStatus();
                    Object str = response.getEntity();
                    response.close();
                } catch (Exception ex) {
                }
            }
        }.start();
    }

    /*public static String sendTcpToBootstrapServer(String message, String ip, int port) throws IOException {
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            echoSocket = new Socket(ip, port);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    echoSocket.getInputStream()));
            out.println(message);
            out.flush();
            StringBuilder result = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {
                //Since c is an integer, cast it to a char. If it isn't -1, it will be in the correct range of char.
                result.append( (char)c ) ;
            }
            return result.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                out.close();
                in.close();
                echoSocket.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }*/
}
