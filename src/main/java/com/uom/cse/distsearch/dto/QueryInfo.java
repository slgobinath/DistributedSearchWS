package com.uom.cse.distsearch.dto;

import java.io.Serializable;

/**
 * @author gobinath
 */
public class QueryInfo {

    private NodeInfo origin;
    private String query;
    private long timestamp;


    public NodeInfo getOrigin() {
        return origin;
    }

    public void setOrigin(NodeInfo origin) {
        this.origin = origin;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryInfo info = (QueryInfo) o;

        if (timestamp != info.timestamp) return false;
        if (origin != null ? !origin.equals(info.origin) : info.origin != null) return false;
        return query != null ? query.equals(info.query) : info.query == null;

    }

    @Override
    public int hashCode() {
        int result = origin != null ? origin.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
