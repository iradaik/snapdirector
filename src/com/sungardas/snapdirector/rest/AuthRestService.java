package com.sungardas.snapdirector.rest;

import static com.sungardas.snapdirector.rest.utils.Constants.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.rest.utils.JsonFromStream;
import com.sungardas.snapdirector.rest.utils.MultiReadHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("/session")
@Path("/session")
public class AuthRestService {
	private static final Log LOG = LogFactory.getLog(AuthRestService.class);

	@Context
	ServletContext context;
	@Context
	private HttpServletRequest servletRequest;

	@POST()
	@Produces(MediaType.APPLICATION_JSON)
	@RequestMapping(method = POST)
	public String login() {
		MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(
				(HttpServletRequest) servletRequest);
		String result = null;
		try (InputStream requestStream = multiReadRequest.getInputStream()) {
			JSONObject authCredentials = JsonFromStream.newJSONObject(requestStream);
			result = DynamoUtils.getFullUserInfoByEmail(authCredentials.getString(JSON_AUTHENTIFICATION_EMAIL), getMapper(servletRequest));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return result;
	}

	private DynamoDBMapper getMapper(ServletRequest request) {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(new EnvironmentBasedCredentialsProvider());
		String region = request.getServletContext().getInitParameter("aws:dynamodb-region");
		client.setRegion(Region.getRegion(Regions.fromName(region)));
		return new DynamoDBMapper(client);
	}

	@DELETE()
	@Produces(MediaType.APPLICATION_JSON)
	public String logout(@Context HttpServletRequest request) {

		String sessionId = request.getSession().getId();
		Set<String> allowedSessions = (Set<String>) context.getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
		boolean result = allowedSessions.remove(sessionId);
		LOG.info("Logout for session: " + sessionId);
		return String.valueOf(result);

	}

	@Path("/{sessionIdFromClient}")
	@DELETE()
	@Produces(MediaType.APPLICATION_XML)
	public String logoutXML(@Context HttpServletRequest request,
			@PathParam("sessionIdFromClient") String sessionIdClient) {

		String sessionId = request.getSession().getId();
		Set<String> allowedSessions = (Set<String>) context.getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
		boolean result = allowedSessions.remove(sessionId);
		LOG.info("Logout for session: " + sessionId);
		return String.valueOf(result);

	}
}
