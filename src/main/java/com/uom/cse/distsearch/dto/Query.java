package com.uom.cse.distsearch.dto;

import com.uom.cse.distsearch.app.Node;

import java.io.Serializable;

/**
 * @author gobinath
 */
public class Query implements Serializable {
    private QueryInfo queryInfo;
    private int hops;
    private NodeInfo sender;

    public QueryInfo getQueryInfo() {
        return queryInfo;
    }

    public void setQueryInfo(QueryInfo queryInfo) {
        this.queryInfo = queryInfo;
    }

    public int getHops() {
        return hops;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }

    public NodeInfo getSender() {
        return sender;
    }

    public void setSender(NodeInfo sender) {
        this.sender = sender;
    }
}
