package com.uom.cse.distsearch.app;

import org.testng.annotations.Test;

/**
 * @author gobinath
 */
public class NodeConnectTest {
    @Test(expectedExceptions = NullPointerException.class)
    public void testNullBootstrapServerIP() {
        Node node = Node.getInstance();
        node.connect(null, 8888, "127.0.0.1", 8080, "alice");
    }
}
