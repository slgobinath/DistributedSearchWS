package com.uom.cse.distsearch.app;

import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.dto.Query;
import com.uom.cse.distsearch.dto.QueryInfo;
import com.uom.cse.distsearch.mock.BootstrapServer;
import com.uom.cse.distsearch.util.MovieList;
import jdk.internal.dynalink.support.BottomGuardingDynamicLinker;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author gobinath
 */
public class NodeConnectTest {
    private BootstrapServer server = new BootstrapServer();
    private MovieList movieList = MovieList.getInstance("/home/gobinath/Workspace/IntelliJIDEA/DistributedSearch/src/main/webapp/WEB-INF/movies.txt");

    @BeforeClass
    public void startBootstrapServer() {
        boolean started = server.start(8888);
    }

    @AfterClass
    public void stopBootstrapServer() {
        server.stop();
    }


    /*
    * Test connect method
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullBootstrapServerIP() {
        Node node = Node.createInstance();
        node.connect(null, 8888, "127.0.0.1", 8080, "alice");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullNodeIP() {
        Node node = Node.createInstance();
        node.connect("192.248.15.229", 8888, null, 8080, "alice");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullUsername() {
        Node node = Node.createInstance();
        node.connect("192.248.15.229", 8888, "127.0.0.1", 8080, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBootstrapServerIP() {
        Node node = Node.createInstance();
        node.connect("", 8888, "127.0.0.1", 8080, "alice");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyNodeIP() {
        Node node = Node.createInstance();
        node.connect("192.248.15.229", 8888, "", 8080, "alice");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyUsername() {
        Node node = Node.createInstance();
        node.connect("192.248.15.229", 8888, "127.0.0.1", 8080, "");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidBootstrapServerIP() {
        Node node = Node.createInstance();
        node.connect("abc.def.gh.ijk", 8888, "127.0.0.1", 8080, "alice");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidNodeIP() {
        Node node = Node.createInstance();
        node.connect("192.248.15.229", 8888, "abc.def.gh.ijk", 8080, "alice");
    }

    @Test
    public void testInvalidServerIP() {
        Node node = Node.createInstance();
        boolean result = node.connect("192.248.15.229", 8888, "127.0.0.1", 8080, "alice");
        Assert.assertFalse(result, "Invalid bootstrap server is accepted");
    }

    @Test
    public void testFailedBootstrapServer() {
        server.setFailed(true);
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8080, "alice");
        Assert.assertFalse(result, "Failed bootstrap server is accepted");
        server.setFailed(false);
    }

    @Test
    public void testFullBootstrapServer() {
        server.setFull(true);
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8080, "alice");
        Assert.assertFalse(result, "Fulled bootstrap server is accepted");
        server.setFull(false);
    }

    @Test
    public void testValidConnection() {
        Node node = Node.getInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8080, "alice");
        Assert.assertTrue(result, "Failed to register the node");
    }

    @Test(expectedExceptions = InvalidStateException.class, dependsOnMethods = "testValidConnection")
    public void testInvalidReconnect() {
        Node node = Node.getInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8080, "alice");
        Assert.assertTrue(result, "Registering two times is accepted by the node");
    }

    @Test(dependsOnMethods = "testValidConnection")
    public void testInvalidRegistrationWithDuplicatePort() {
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8080, "bob");
        Assert.assertFalse(result, "Duplicate port from same ip is accepted");
    }

    @Test(dependsOnMethods = "testValidConnection")
    public void testOnePeerConnection() {
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8081, "bob");
        Assert.assertTrue(result, "Registering from different port is not accepted");

        List<NodeInfo> peers = node.getPeers();
        Assert.assertEquals(peers.size(), 1, "Number of peers are wrong");
    }

    @Test(dependsOnMethods = {"testValidConnection", "testOnePeerConnection"})
    public void testTwoPeersConnection() {
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8082, "carol");
        Assert.assertTrue(result, "Registering from different port is not accepted");

        List<NodeInfo> peers = node.getPeers();
        Assert.assertEquals(peers.size(), 2, "Number of peers are wrong");
    }

    @Test(dependsOnMethods = {"testValidConnection", "testOnePeerConnection", "testTwoPeersConnection"})
    public void testMoreThanTwoPeersConnection() {
        Node node = Node.createInstance();
        boolean result = node.connect("127.0.0.1", 8888, "127.0.0.1", 8083, "david");
        Assert.assertTrue(result, "Registering from different port is not accepted");

        List<NodeInfo> peers = node.getPeers();
        Assert.assertEquals(peers.size(), 2, "Number of peers are wrong");
    }

    /*
    * Test join method
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullJoin() {
        Node node = Node.createInstance();
        node.join(null);
    }

    @Test(dependsOnMethods = {"testValidConnection"}, expectedExceptions = IllegalArgumentException.class)
    public void testJoinItself() {
        Node node = Node.getInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8080, "alice");
        node.join(info);
    }

    @Test(expectedExceptions = InvalidStateException.class)
    public void testJoinToUnregisteredNode() {
        Node node = Node.createInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8080, "alice");
        node.join(info);
    }

    @Test(dependsOnMethods = {"testValidConnection"})
    public void testValidJoin() {
        Node node = Node.getInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8085, "bob");
        node.join(info);
    }

    /*
    * Test leave method
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullLeave() {
        Node node = Node.createInstance();
        node.leave(null);
    }

    @Test(expectedExceptions = InvalidStateException.class)
    public void testLeaveToUnregisteredNode() {
        Node node = Node.createInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8080, "alice");
        node.leave(info);
    }

    @Test(dependsOnMethods = {"testValidJoin"})
    public void testValidLeave() {
        Node node = Node.getInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8085, "bob");
        node.leave(info);
    }

    /*
    * Test disconnect method
     */
    @Test(expectedExceptions = InvalidStateException.class)
    public void testDisconnectToUnregisteredNode() throws IOException {
        Node node = Node.createInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8080, "alice");
        node.disconnect();
    }

    @Test(dependsOnMethods = {"testValidStartSearch"})
    public void testValidDisconnect() throws IOException {
        Node node = Node.getInstance();
        node.disconnect();
    }

    /*
     * Test getPeers method
     */
    @Test(expectedExceptions = InvalidStateException.class)
    public void testGetPeersOfUnregisteredNode() {
        Node node = Node.createInstance();
        node.getPeers();
    }

    /*
    * Test startSearch method
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStartSearchWithNullName() {
        Node node = Node.getInstance();
        node.startSearch(movieList, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStartSearchWithEmptyName() {
        Node node = Node.getInstance();
        node.startSearch(movieList, "");
    }

    @Test(expectedExceptions = InvalidStateException.class)
    public void testStartSearchInUnregisteredNode() {
        Node node = Node.createInstance();
        node.startSearch(movieList, "TinTin");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStartSearchWithNullMovieList() {
        Node node = Node.getInstance();
        node.startSearch(null, "abc");
    }

    @Test(dependsOnMethods = {"testValidLeave"})
    public void testValidStartSearch() {
        Node node = Node.getInstance();
        NodeInfo info = new NodeInfo("127.0.0.1", 8085, "bob");
        node.join(info);
        info = new NodeInfo("127.0.0.1", 8086, "carol");
        node.join(info);
        node.startSearch(movieList, "abc");
    }

    /*
    * Test search method
     */
    @Test(dependsOnMethods = {"testValidConnection"}, expectedExceptions = IllegalArgumentException.class)
    public void testSearchWithNullQuery() {
        Node node = Node.getInstance();
        node.search(movieList, null);
    }

    @Test(expectedExceptions = InvalidStateException.class)
    public void testSearchInUnregisteredNode() {
        Node node = Node.createInstance();
        node.search(movieList, createQuery());
    }

    @Test(dependsOnMethods = {"testValidConnection"}, expectedExceptions = IllegalArgumentException.class)
    public void testSearchWithNullMovieList() {
        Node node =  Node.getInstance();
        node.search(null, createQuery());
    }

    @Test(dependsOnMethods = {"testValidLeave"})
    public void testSearchValidQuery() {
        Node node = Node.getInstance();
        node.search(movieList, createQuery());
    }

    @Test(dependsOnMethods = {"testSearchValidQuery"})
    public void testSearchDuplicateQuery() {
        Node node = Node.getInstance();
        Query query = createQuery();
        node.search(movieList, query);
        node.search(movieList, query);
    }

    private static Query createQuery() {
        NodeInfo nodeInfo = new NodeInfo("127.0.0.1", 8080, "alice");
        QueryInfo queryInfo = new QueryInfo();
        queryInfo.setTimestamp(System.currentTimeMillis());
        queryInfo.setQuery("Tin Tin");
        queryInfo.setOrigin(nodeInfo);
        Query query = new Query();
        query.setHops(0);
        query.setQueryInfo(queryInfo);
        query.setSender(nodeInfo);

        return query;
    }
}
