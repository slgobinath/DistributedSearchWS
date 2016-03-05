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

    /**
     * Only use it for testing purposes.
     *
     * @return
     */
    static Node createInstance() {
        return new Node();
    }

    public synchronized void join(NodeInfo info) {
        // Validation
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("NodeInfo cannot be null");
        }

        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

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
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("NodeInfo cannot be null");
        }

        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        LOGGER.debug("Removing {} from the peer list of {}", info, currentNodeInfo);
        peerList.remove(info);
    }


    public synchronized void startSearch(MovieList movieList, String name) {
        // Validation
        if (Objects.isNull(movieList)) {
            throw new IllegalArgumentException("MovieList cannot be null");
        }

        if (Objects.isNull(name) || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }


        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
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
        Utility.post(info.getOrigin().url() + "results", result);

        // Spread to the peers
        for (NodeInfo peer : peerList) {
            // Don't send to the sender again
            if (!Objects.equals(peer, sender)) {
                Utility.post(peer.url() + "search", query);
            }
        }
    }

    public synchronized void search(MovieList movieList, Query query) {
        // Validation
        if (Objects.isNull(movieList)) {
            throw new IllegalArgumentException("MovieList cannot be null");
        }
        if (Objects.isNull(query) || Objects.isNull(query.getQueryInfo())) {
            throw new IllegalArgumentException("Query or QueryInfo of this query cannot be null");
        }

        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        QueryInfo info = query.getQueryInfo();

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
        Utility.post(info.getOrigin().url() + "results", result);

        // Spread to the peers
        for (NodeInfo peer : peerList) {
            if (!peer.equals(sender)) {
                LOGGER.debug("Sending request to {}", peer);
                Utility.post(peer.url() + "search", query);
            }
        }
    }

    public synchronized boolean connect(String serverIP, int serverPort, String nodeIP, int port, String username) {
        // Validate
        if (Objects.isNull(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip cannot be null");
        }
        if (Objects.isNull(nodeIP)) {
            throw new IllegalArgumentException("Node ip cannot be null");
        }
        if (Objects.isNull(username) || "".equals(username.trim())) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }
        if (!IPAddressValidator.validate(serverIP)) {
            throw new IllegalArgumentException("Bootstrap server ip is not valid");
        }
        if (!IPAddressValidator.validate(nodeIP)) {
            throw new IllegalArgumentException("Node ip is not valid");
        }

        // State check
        if (!Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is already registered.");
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
                        Utility.post(nodeInfo.url() + "join", new NodeInfo(nodeIP, port));
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
                        Utility.post(nodeA.url() + "join", currentNodeInfo);

                        join(nodeB);
                        Utility.post(nodeB.url() + "join", currentNodeInfo);
                        break;

                    case 9996:
                        LOGGER.error("Failed to register. BootstrapServer is full.");
                        this.currentNodeInfo = null;
                        return false;

                    case 9997:
                        LOGGER.error("Failed to register. This ip and port is already used by another Node.");
                        this.currentNodeInfo = null;
                        return false;
                }

                return true;
            } else {
                this.currentNodeInfo = null;
                return false;
            }

        } catch (IOException e) {
            this.currentNodeInfo = null;
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public synchronized boolean disconnect() throws IOException {
        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }

        // Update other nodes
        final int peerSize = peerList.size();
        for (int i = 0; i < peerSize; i++) {
            NodeInfo on = peerList.get(i);
            for (int j = 0; j < peerSize; j++) {
                NodeInfo node = peerList.get(j);
                if (i != j) {
                    Utility.post(on.url() + "join", node);
                }
            }
        }

        for (NodeInfo peer : peerList) {
            //send leave msg
            Utility.post(peer.url() + "leave", currentNodeInfo);
        }

        String message = String.format(" UNREG %s %d %s", currentNodeInfo.getIp(), currentNodeInfo.getPort(), currentNodeInfo.getUsername());
        message = String.format("%04d", (message.length() + 4)) + message;
        String result = Utility.sendTcpToBootstrapServer(message, this.bootstrapHost, this.bootstrapPort);
        StringTokenizer tokenizer = new StringTokenizer(result, " ");
        String length = tokenizer.nextToken();
        String command = tokenizer.nextToken();
        boolean success = Constant.UNROK.equals(command);
        if (success) {
            this.currentNodeInfo = null;
        }
        return success;
    }

    public synchronized List<NodeInfo> getPeers() {
        // State check
        if (Objects.isNull(currentNodeInfo)) {
            throw new InvalidStateException("Node is not registered in the bootstrap server");
        }
        return peerList;
    }

}
