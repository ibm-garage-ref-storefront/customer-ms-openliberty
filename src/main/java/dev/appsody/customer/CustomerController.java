package dev.appsody.customer;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import dev.appsody.customer.client.CouchDBClientService;
import dev.appsody.customer.client.UnknownCustomerExceptionManager;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

@Path("/customer")
@RequestScoped
public class CustomerController {
	
	@Inject
    private JsonWebToken jwt;
	
	Config config = ConfigProvider.getConfig();
    
    String temp = "http://" + config.getValue("host", String.class) + ":" + config.getValue("port", String.class);
    URL apiUrl = createApiUrl(temp);
    CouchDBClientService cdb =
        RestClientBuilder.newBuilder()
                        .baseUrl(apiUrl)
                        .register(UnknownCustomerExceptionManager.class)
                        .build(CouchDBClientService.class);

    private URL createApiUrl(String url) { 
        URL apiUrl = null;
        try {
            apiUrl = new URL(temp);
            System.out.println(apiUrl);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return apiUrl;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getCustomerByUsername() throws Exception{
    	System.out.println("Entered");
        try {
            JSONObject body = new JSONObject();
            JSONObject selector = new JSONObject();
            System.out.println("username "+jwt.getSubject());
            selector.put("username", jwt.getSubject());
            System.out.println("selector "+selector);
            body.put("selector", selector);
            JSONArray fields = new JSONArray();
            body.put("fields", fields);
            body.put("limit", 1);
            System.out.println(body);
            return cdb.getUsername(body);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build());
            throw new Exception(e.toString());
        }
    }

//    @Produces(MediaType.APPLICATION_JSON)
//    public javax.ws.rs.core.Response fallbackService() {
//        System.out.println();
//        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).entity("CouchDB Service is down.").build();
//    }

}
