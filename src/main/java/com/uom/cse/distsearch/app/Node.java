package com.uom.cse.distsearch.app;

import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.dto.Query;
import com.uom.cse.distsearch.dto.QueryInfo;
import com.uom.cse.distsearch.dto.Result;
import com.uom.cse.distsearch.util.Constant;
import com.uom.cse.distsearch.util.IPAddressValidator;
import com.uom.cse.distsearch.util.MovieList;
import com.uom.cse.distsearch.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

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

    public String bootstrapHost;

    public int bootstrapPort;

    private NodeInfo currentNodeInfo;

    private static class InstanceHolder {
        private static Node instance = new Node();
    }

    private Node() {
    }

    public static Node getInstance() {
        return InstanceHolder.instance;
    }


    public synchronized void join(NodeInfo info) {
        // Validation
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }

        Objects.requireNonNull(info, "NodeInfo cannot be null");

        if (Objects.equals(info.getIp(), currentNodeInfo.getIp()) && info.getPort() == currentNodeInfo.getPort()) {
            throw new IllegalArgumentException("Cannot add this node as a peer of itself");
        }

        LOGGER.debug("Adding {} as a peer of {}", info, currentNodeInfo);
        if (!peerList.contains(info)) {
            peerList.add(info);
        }
    }

    public synchronized void leave(NodeInfo info) {
        // Validation
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }

        Objects.requireNonNull(info, "NodeInfo cannot be null");

        LOGGER.debug("Removing {} from the peer list of {}", info, currentNodeInfo);
        peerList.remove(info);
    }


    public synchronized void startSearch(MovieList movieList, String name) {
        // Validation
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }

        Objects.requireNonNull(movieList, "MovieList cannot be null");

        Objects.requireNonNull(name, "Name cannot be null");

        if ("".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        LOGGER.debug("Searching for {} on {}", name, currentNodeInfo);

        // Construct the repository
        QueryInfo info = new QueryInfo();
        info.setOrigin(currentNodeInfo);
        info.setQuery(name);
        info.setTimestamp(System.currentTimeMillis());

        Query query = new Query();
        query.setHops(0);
        query.setSender(currentNodeInfo);
        query.setQueryInfo(info);


        // Search within myself
        NodeInfo sender = query.getSender();
        List<String> results = movieList.search(info.getQuery());

        Result result = new Result();
        result.setOwner(currentNodeInfo);
        result.setMovies(results);
        result.setHops(0);
        result.setTimestamp(info.getTimestamp());

        // Send the results
        post(info.getOrigin().url() + "results", result);

        // Spread to the peers
        for (NodeInfo peer : peerList) {
            // Don't send to the sender again
            if (!Objects.equals(peer, sender)) {
                post(peer.url() + "search", query);
            }
        }
    }

    public synchronized void search(MovieList movieList, Query query) {
        // Validation

        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }

        Objects.requireNonNull(query, "Query cannot be null");

        QueryInfo info = query.getQueryInfo();

        Objects.requireNonNull(query, "QueryInfo of the given query cannot be null");

        if (queryList.contains(info)) {
            // Duplicate query
            return;
        } else {
            queryList.add(info);
        }

        // Increase the number of hops by one
        query.setHops(query.getHops() + 1);

        NodeInfo sender = query.getSender();

        List<String> results = movieList.search(info.getQuery());

        Result result = new Result();
        result.setOwner(currentNodeInfo);
        result.setMovies(results);
        result.setHops(query.getHops());
        result.setTimestamp(info.getTimestamp());

        // Send the results
        post(info.getOrigin().url() + "results", result);

        // Spread to the peers
        for (NodeInfo peer : peerList) {
            if (!peer.equals(sender)) {
                LOGGER.debug("Sending request to {}", peer);
                post(peer.url() + "search", query);
            }
        }
    }

    public synchronized boolean connect(String serverIP, int serverPort, String nodeIP, int port, String username) {
        // Validate
        Objects.requireNonNull(serverIP, "Bootstrap server ip cannot be null");
        Objects.requireNonNull(nodeIP, "Node ip cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");

        if (!IPAddressValidator.validate(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip is not valid");
        }

        if (!IPAddressValidator.validate(nodeIP)) {
            throw new IllegalArgumentException("Node ip is not valid");
        }

        this.bootstrapHost = serverIP;
        this.bootstrapPort = serverPort;
        this.currentNodeInfo = new NodeInfo(nodeIP, port, username);

        // Generate the command
        String message = String.format(" REG %s %d %s", nodeIP, port, username);
        message = String.format("%04d", (message.length() + 4)) + message;

        try {
            String result = Utility.sendTcpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);

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
                        LOGGER.debug("First node registered");
                        break;

                    case 1:
                        LOGGER.debug("Second node registered");
                        String ipAddress = tokenizer.nextToken();
                        int portNumber = Integer.parseInt(tokenizer.nextToken());
                        // TODO: Test the following line
                        String userName = tokenizer.nextToken();

                        NodeInfo nodeInfo = new NodeInfo(ipAddress, portNumber, userName);
                        // JOIN to first node
                        join(nodeInfo);
                        post(nodeInfo.url() + "join", new NodeInfo(nodeIP, port));
                        break;

                    default:

                        LOGGER.debug("{} nodes registered", no_nodes);
                        List<NodeInfo> returnedNodes = new ArrayList<>();

                        // Select random 2 nodes
                        for (int i = 0; i < no_nodes; i++) {
                            String host = tokenizer.nextToken();
                            String hostPost = tokenizer.nextToken();
                            String userID = tokenizer.nextToken();

                            LOGGER.debug(String.format("%s:%s - %s", host, hostPost, userID));

                            NodeInfo node = new NodeInfo(host, Integer.parseInt(hostPost), userID);
                            returnedNodes.add(node);
                        }

                        Collections.shuffle(returnedNodes);

                        NodeInfo nodeA = returnedNodes.get(0);
                        NodeInfo nodeB = returnedNodes.get(1);

                        join(nodeA);
                        post(nodeA.url() + "join", currentNodeInfo);

                        join(nodeB);
                        post(nodeB.url() + "join", currentNodeInfo);
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

    public synchronized boolean disconnect() {
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }

        // Update other nodes
        final int peerSize = peerList.size();
        for (int i = 0; i < peerSize; i++) {
            NodeInfo on = peerList.get(i);
            if (on.equals(currentNodeInfo)) {
                continue;
            }
            for (int j = 0; j < peerSize; j++) {
                NodeInfo node = peerList.get(j);
                if (i != j) {
                    post(on.url() + "join", node);
                }
            }
        }

        for (NodeInfo peer : peerList) {
            //send leave msg
            post(peer.url() + "leave", currentNodeInfo);
        }

        String message = String.format(" UNREG %s %d %s", currentNodeInfo.getIp(), currentNodeInfo.getPort(), currentNodeInfo.getUsername());
        message = String.format("%04d", (message.length() + 4)) + message;
        try {
            String result = Utility.sendTcpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);
            StringTokenizer tokenizer = new StringTokenizer(result, " ");
            String length = tokenizer.nextToken();
            String command = tokenizer.nextToken();
            return Constant.UNROK.equals(command);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public synchronized List<NodeInfo> getPeers() {
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is registered in the bootstrap server");
        }
        return peerList;
    }

    private void post(final String url, final Object object) {
        LOGGER.debug("POST URL: {}", url);
        new Thread() {
            @Override
            public void run() {
                try {
                    WebTarget target = ClientBuilder.newClient().target(url);
                    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_PLAIN);
                    Response response = builder.post(Entity.json(object));
                    int status = response.getStatus();
                    LOGGER.debug("Status: {}", status);
                    Object str = response.getEntity();
                    LOGGER.debug("Message: {}", str);
                    response.close();
                } catch (Exception ex) {
                    LOGGER.error("Exception in sending request", ex.getMessage());
                }
            }
        }.start();
    }
}
