package com.uom.cse.distsearch.app;


import com.uom.cse.distsearch.util.Constant;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * This class register the JAX-RS service providers package.
 * <p>
 * Created by gobinath on 11/27/15.
 */
public class WebServiceConfiguration extends ResourceConfig {
    public WebServiceConfiguration() {
        // Now you can expect validation errors to be sent to the client.
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        // @ValidateOnExecution annotations on subclasses won't cause errors.
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
        // Define the package which contains the service classes.
        packages(Constant.WEB_SERVICE_PACKAGE);
    }
}
