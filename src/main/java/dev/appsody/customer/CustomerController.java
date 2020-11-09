package dev.appsody.customer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.appsody.customer.client.CouchDBClientService;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

@Path("/customer")
@RequestScoped
public class CustomerController {
		
	@Inject
	@RestClient
	private CouchDBClientService cdb;
	
	@Inject
    private JsonWebToken jwt;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomerByUsername() throws Exception{
        try {
            JSONObject body = new JSONObject();
            JSONObject selector = new JSONObject();
            
            selector.put("username", jwt.getSubject());
            
            body.put("selector", selector);
            JSONArray fields = new JSONArray();
            body.put("fields", fields);
            body.put("limit", 1);
          
            return cdb.getUsername(body);

        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build());
            throw new Exception(e.toString());
        }
    }

}
