package dev.appsody.customer.client;

import java.util.Base64;

import javax.enterprise.context.Dependent;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.ibm.json.java.JSONObject;

@Dependent
@RegisterRestClient
@Produces("application/json")
@RegisterProvider(UnknownCustomerExceptionManager.class)
@ClientHeaderParam(name = "Authorization", value = "{lookupAuth}")
@Path("/customers")
public interface CouchDBClientService {
	
	default String lookupAuth() {
	    return "Basic " + 
	         Base64.getEncoder().encodeToString("admin:password".getBytes());
	}
	
	@GET
	@Produces("application/json")
	public Response getInfo() throws UnknownCustomerException;
	
	@POST
    @Path("/_find")
    @Produces("application/json")
    @Consumes("application/json")
    public Response getUsername(JSONObject body) throws UnknownCustomerException;

}
