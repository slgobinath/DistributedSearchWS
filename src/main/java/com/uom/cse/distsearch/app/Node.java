package com.uom.cse.distsearch.app;

import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.dto.Query;
import com.uom.cse.distsearch.dto.QueryInfo;
import com.uom.cse.distsearch.util.Constant;
import com.uom.cse.distsearch.util.MovieList;
import com.uom.cse.distsearch.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author gobinath
 */
public class Node {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);

    private final List<NodeInfo> peerList = new ArrayList<>();

    private final List<QueryInfo> queryList = new ArrayList<>();

    public void join(NodeInfo info) {
        if (!peerList.contains(info)) {
            peerList.add(info);
        }
    }

    public void leave(NodeInfo info) {
        peerList.remove(info);
    }


    public void startSearch(String ip, int port, String name) {
        QueryInfo info = new QueryInfo();
        info.setOrigin(new NodeInfo(ip, port));
        info.setQuery(name);
        info.setTimestamp(System.currentTimeMillis());

        Query query = new Query();
        query.setHops(0);
        query.setSender(new NodeInfo(ip, port));
        query.setQueryInfo(info);

        for (NodeInfo peer : peerList) {
            post(peer.url() + "search", query);
        }
    }

    public void search(ServletContext context, Query query) {
        QueryInfo info = query.getQueryInfo();

        if (queryList.contains(info)) {
            // Duplicate query
            return;
        } else {
            queryList.add(info);
        }

        NodeInfo sender = query.getSender();
        MovieList movieList = MovieList.getInstance(context);
        List<String> results = movieList.search(info.getQuery());

        // Send the results
        post(info.getOrigin().url() + "results", results);

        // Increase the number of hops by 1
        query.setHops(query.getHops() + 1);
        for (NodeInfo peer : peerList) {
            if (!peer.equals(sender)) {
                post(peer.url() + "search", query);
            }
        }
    }

    public boolean connect(String ip, int port, String username) {
        String message = String.format(" REG %s %d %s", ip, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;
        try {
            String result = Utility.sendTcpToBootstrapServer(message, Constant.BOOTSTRAP_HOST, Constant.BOOTSTRAP_PORT);

            LOGGER.debug("Connect response is {}", result);
            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            if (Constant.REGOK.equals(command)) {
                int no_nodes = Integer.parseInt(tokenizer.nextToken());

                switch (no_nodes) {
                    case 0:
                        // This is the first node registered to the BootstrapServer.
                        // Do nothing
                        LOGGER.info("First node registered");
                        break;

                    case 1:
                        LOGGER.info("Second node registered");
                        String ipAddress = tokenizer.nextToken();
                        int portNumber = Integer.parseInt(tokenizer.nextToken());

                        NodeInfo nodeInfo = new NodeInfo(ipAddress, portNumber);
                        // JOIN to first node
                        join(nodeInfo);
                        post(nodeInfo.url() + "join", new NodeInfo(ip, port));
                        break;

                    case 2:
                        NodeInfo nodeA = new NodeInfo(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
                        NodeInfo nodeB = new NodeInfo(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));

                        LOGGER.info("More than 2 nodes registered");

                        // JOIN to node A
                        join(nodeA);
                        post(nodeA.url() + "join", new NodeInfo(ip, port));

                        // JOIN to node B
                        join(nodeB);
                        post(nodeB.url() + "join", new NodeInfo(ip, port));

                        break;

                    case 9996:
                        LOGGER.error("Failed to register. BootstrapServer is full.");
                        return false;

                    case 9997:
                        LOGGER.error("Failed to register. This ip and port is already used by another Node.");
                        return false;

                    case 9998:
                        LOGGER.error("You are already registered. Please unregister first.");
                        return false;

                    case 9999:
                        LOGGER.error("Error in the command. Please fix the error");
                        return false;
                }

                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean disconnect(String ip, int port, String username) {
        NodeInfo me = new NodeInfo(ip, port);
        for (NodeInfo peer : peerList) {
            //send leave msg
            post(peer.url() + "leave", me);
        }

        String message = String.format(" UNREG %s %d %s", ip, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;
        try {
            String result = Utility.sendTcpToBootstrapServer(message, Constant.BOOTSTRAP_HOST, Constant.BOOTSTRAP_PORT);
            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            if (Constant.UNROK.equals(command)) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public List<NodeInfo> getPeers() {
        return peerList;
    }

    public void post(String url, Object object) {
        WebTarget target = ClientBuilder.newClient().target(url);
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON_TYPE);
        Response response = builder.post(Entity.json(object));
    }
}