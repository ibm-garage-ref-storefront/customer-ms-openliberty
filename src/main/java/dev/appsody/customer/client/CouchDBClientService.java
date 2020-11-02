package dev.appsody.customer.client;

import javax.enterprise.context.Dependent;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.ibm.json.java.JSONObject;

@Dependent
@RegisterRestClient
@Produces("application/json")
@RegisterProvider(UnknownCustomerExceptionManager.class)
@Path("/customers")
public interface CouchDBClientService {
	
	@POST
    @Path("/_find")
    @Produces("application/json")
    @Consumes("application/json")
    public javax.ws.rs.core.Response getUsername(JSONObject body) throws UnknownCustomerException;

}
