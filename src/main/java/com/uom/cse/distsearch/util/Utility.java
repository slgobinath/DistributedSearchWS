package com.uom.cse.distsearch.util;

import javax.rmi.CORBA.Util;
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
        while((line = inFromServer.readLine()) != null) {
            builder.append(line);
        }
        String response = builder.toString().replace("\r\n", " ").replace('\n', ' ').replace("", "");
        System.out.println("FROM SERVER: " + response);
        clientSocket.close();

        return response;
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
