package com.uom.cse.distsearch.app;

import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.mock.BootstrapServer;
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

    @BeforeClass
    public void startBootstrapServer() {
        boolean started = server.start(8888);
    }

    @AfterClass
    public void stopBootstrapServer() {
        server.stop();
    }

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
}
