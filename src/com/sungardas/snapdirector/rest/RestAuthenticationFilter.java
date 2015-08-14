package com.sungardas.snapdirector.rest;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.rest.utils.JsonFromStream;
import com.sungardas.snapdirector.rest.utils.MultiReadHttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static com.sungardas.snapdirector.rest.utils.Constants.*;

@WebFilter("/rest/*")
public class RestAuthenticationFilter implements Filter {
	private static final Log LOG = LogFactory.getLog(RestAuthenticationFilter.class);

	public RestAuthenticationFilter() {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException,
			IOException {

		Set<String> allowedSessions = (Set<String>) request.getServletContext().getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
		if (allowedSessions == null) {
			request.getServletContext().setAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME, new HashSet<String>());
			allowedSessions = (Set<String>) request.getServletContext().getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
		}

		LOG.info("RestAuthenticationFilter:doFilter");
		LOG.info("RestAuthenticationFilter:sessionid=" + ((HttpServletRequest) request).getSession().getId());
		if (request instanceof HttpServletRequest) {
			MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest((HttpServletRequest) request);
			HttpSession session = ((HttpServletRequest) request).getSession();
			boolean allowed = false;

			allowed = allowedSessions.contains(session.getId());

			if (!allowed && multiReadRequest.getPathInfo().endsWith("/session")) {
				System.out.println(multiReadRequest.getPathInfo());
				LOG.info("RestAuthenticationFilter: new session " + session.getId());
				JSONObject authCredentials = null;

				InputStream requestStream = multiReadRequest.getInputStream();
				authCredentials = JsonFromStream.newJSONObject(requestStream);

				LOG.info("email: " + authCredentials.getString(JSON_AUTHENTIFICATION_EMAIL));
				LOG.info("password: " + authCredentials.getString(JSON_AUTHENTIFICATION_PASSWORD));

				allowed = DynamoUtils.authenticateUser(authCredentials.getString(JSON_AUTHENTIFICATION_EMAIL),
						authCredentials.getString(JSON_AUTHENTIFICATION_PASSWORD), getMapper(request));

			}

			if (allowed) {
				allowedSessions.add(session.getId());
				LOG.info("RestAuthenticationFilter: alloved; session" + session.getId());
				try {
					chain.doFilter(multiReadRequest, response);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				LOG.info("RestAuthenticationFilter: deny; session" + session.getId());
				if (response instanceof HttpServletResponse) {
					HttpServletResponse httpServletResponse = (HttpServletResponse) response;
					httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}
			}
		}

	}

	public void init(FilterConfig fConfig) throws ServletException {
	}

	private DynamoDBMapper getMapper(ServletRequest request) {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(new EnvironmentBasedCredentialsProvider());
		String region = request.getServletContext().getInitParameter("aws:dynamodb-region");
		return new DynamoDBMapper(client);
	}

}
