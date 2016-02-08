package com.uom.cse.distsearch.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author gobinath
 */
public class Result implements Serializable {
    private NodeInfo owner;
    private List<String> movies;

    public NodeInfo getOwner() {
        return owner;
    }

    public void setOwner(NodeInfo owner) {
        this.owner = owner;
    }

    public List<String> getMovies() {
        return movies;
    }

    public void setMovies(List<String> movies) {
        this.movies = movies;
    }
}
