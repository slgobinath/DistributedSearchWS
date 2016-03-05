package com.uom.cse.distsearch.mock;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * @author gobinath
 */
public class BootstrapServer {
    private boolean failed;
    private boolean full;

    private ServerSocket welcomeSocket;
    private static List<Neighbour> nodes = new ArrayList<Neighbour>();


    public static void main(String[] args) {
        new BootstrapServer().start(8888);
        System.out.println("End");
    }

    public String process(String s) {
        if (failed) {
            return "0010 ERROR";
        }

        if (full) {
            return "0114 REGOK 9996";
        }

        StringTokenizer st = new StringTokenizer(s, " ");

        String length = st.nextToken();
        String command = st.nextToken();

        if (command.equals("REG")) {
            String reply = "0114 REGOK ";

            String ip = st.nextToken();
            int port = Integer.parseInt(st.nextToken());
            String username = st.nextToken();
            if (nodes.size() == 0) {
                reply += "0";
                nodes.add(new Neighbour(ip, port, username));
            } else {
                boolean isOkay = true;
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).getPort() == port) {
                        if (nodes.get(i).getUsername().equals(username)) {
                            reply += "9998";
                        } else {
                            reply += "9997";
                        }
                        isOkay = false;
                    }
                }
                if (isOkay) {
                    reply += nodes.size() + " " + nodes.stream().map(x -> x.toString()).collect(Collectors.joining(" "));
                    nodes.add(new Neighbour(ip, port, username));
                }
            }
            return reply;
        } else if (command.equals("UNREG")) {
            String ip = st.nextToken();
            int port = Integer.parseInt(st.nextToken());
            String username = st.nextToken();
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).getPort() == port) {
                    nodes.remove(i);
                }
            }
            String reply = "0012 UNROK 0";
            return reply;
        } else {
            String reply = "0012 ECHOK 0";
            return reply;
        }
    }

    public boolean start(int port) {
        if (welcomeSocket != null) {
            System.out.println("Server is already running");
            return false;
        }
        try {
            welcomeSocket = new ServerSocket(port);
        } catch (IOException e) {
            return false;
        }
        new Thread() {
            @Override
            public void run() {
                System.out.println("Started....");
                while (welcomeSocket != null && !welcomeSocket.isClosed()) {
                    try {
                        Socket connectionSocket = welcomeSocket.accept();
                        System.out.println("Received...");
                        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

                        // Read from input stream
                        StringBuilder builder = new StringBuilder();
                        char[] buffer = new char[4];
                        inFromClient.read(buffer);
                        builder.append(buffer);
                        buffer = new char[Integer.parseInt(builder.toString()) - 4];
                        inFromClient.read(buffer);
                        builder.append(buffer);

                        String command = builder.toString();//inFromClient.readLine();
                        System.out.println("Received: " + command);
                        String reply = process(command);
                        System.out.println("Reply: " + reply);

                        System.out.println(connectionSocket.getInetAddress().getHostAddress());


                        outToClient.writeBytes(reply);
                        outToClient.close();
                        System.out.println("Written");

                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }.start();
        return true;
    }

    public void stop() {
        if (welcomeSocket != null && !welcomeSocket.isClosed()) {
            try {
                welcomeSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //simple function to echo data to terminal
    private static void echo(String msg) {
        System.out.println(msg);
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    private static class Neighbour {
        private String ip;
        private int port;
        private String username;

        public Neighbour(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public Neighbour(String ip, int port, String username) {
            this.ip = ip;
            this.port = port;
            this.username = username;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return ip + " " + port + " " + username;
        }
    }

}
