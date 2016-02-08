package com.uom.cse.distsearch.service;


import com.uom.cse.distsearch.app.Node;
import com.uom.cse.distsearch.dto.NodeInfo;
import com.uom.cse.distsearch.dto.Query;
import com.uom.cse.distsearch.dto.Result;
import com.uom.cse.distsearch.util.Constant;
import com.uom.cse.distsearch.util.MovieList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author gobinath
 */
@Path("/service")
public class Service {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    @Context
    private ServletContext context;

    @Context
    private HttpServletRequest httpRequest;

    private Node node = Node.getInstance();


    @Path("/movies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMovies() {
        LOGGER.debug("Request to list the selected movies");
        MovieList movieList = MovieList.getInstance(context);
        return Response.status(Response.Status.OK).entity(movieList.getSelectedMovies()).build();
    }

    @Path("/peers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPeers() {
        LOGGER.debug("Request to list the connected peers");
        List<NodeInfo> lst = node.getPeers();
        LOGGER.debug("PEERS {}", lst.toString());
        return Response.status(Response.Status.OK).entity(lst).build();
    }

    @Path("/connect/{ip}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(@NotNull @PathParam("ip") String ip, @NotNull @PathParam("username") String username) {
        LOGGER.debug("Request to connect to the bootstrap server");
        // Connect to the Bootstrap
        Response.Status status = Response.Status.OK;
        if (!node.connect(ip, httpRequest.getLocalPort(), username)) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        // Disconnect from the Bootstrap
        return Response.status(status).build();
    }

    @Path("/disconnect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect() {
        LOGGER.debug("Request to disconnect from the bootstrap server");
        Response.Status status = Response.Status.OK;
        if (!node.disconnect()) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        // Disconnect from the Bootstrap
        return Response.status(status).build();
    }

    @Path("/join")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response join(@NotNull NodeInfo nodeInfo) {
        LOGGER.debug("Request to join from {}", nodeInfo);
        node.join(nodeInfo);
        return Response.status(Response.Status.OK).entity(Constant.JOINOK).build();
    }

    @Path("/leave")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response leave(@NotNull NodeInfo nodeInfo) {
        LOGGER.debug("Request to leave from {}", nodeInfo);
        node.leave(nodeInfo);
        return Response.status(Response.Status.OK).entity(Constant.LEAVEOK).build();
    }

    @Path("/searchuser")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchuser(@NotNull @PathParam("ip") String ip, @NotNull @QueryParam("query") String query) {
        LOGGER.debug("Request to search {}", query);
        node.startSearch(context, query);
        return Response.status(Response.Status.OK).entity(Constant.SEROK).build();
    }

    @Path("/search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response search(@NotNull Query query) {
        LOGGER.debug("Request to search {} from {}", query.getQueryInfo().getQuery(), query.getSender());
        node.search(context, query);
        return Response.status(Response.Status.OK).entity(Constant.SEROK).build();
    }

    @Path("/results")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response results(@NotNull Result result) {
        LOGGER.info("Movies found at {} are {} after {} hops", result.getOwner(), result.getMovies(), result.getHops());
        return Response.status(Response.Status.OK).entity("OK").build();
    }
}
