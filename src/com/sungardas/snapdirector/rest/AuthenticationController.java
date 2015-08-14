package com.sungardas.snapdirector.rest;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.User;
import com.sungardas.snapdirector.rest.utils.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

;

/**
 * Created by iradaik on 8/13/2015.
 */
@RestController
@RequestMapping("/session")
public class AuthenticationController {

    private static final Logger LOG = LogManager.getLogger(AuthenticationController.class);

    @Autowired
    private ServletContext context;
    @Autowired
    private HttpServletRequest servletRequest;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> login(@RequestBody User user) {
        ResponseEntity<String> responseEntity;
        String result = DynamoUtils.getFullUserInfoByEmail(user.getEmail(), getMapper(servletRequest));
        if (result == null) {
            responseEntity = new ResponseEntity("No appropriate user was found", HttpStatus.INTERNAL_SERVER_ERROR);
            LOG.debug("No user registered with email [{}] was found.", user.getEmail());
        } else {
            responseEntity = new ResponseEntity(result, HttpStatus.OK);
        }
        return responseEntity;
    }

    private DynamoDBMapper getMapper(ServletRequest request) {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(new EnvironmentBasedCredentialsProvider());
        String region = request.getServletContext().getInitParameter(Constants.JSON_DYNAMODB_REGION);
        client.setRegion(Region.getRegion(Regions.fromName(region)));
        return new DynamoDBMapper(client);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<String> logout() {
        String sessionId = servletRequest.getSession().getId();
        Set<String> allowedSessions = (Set<String>) context.getAttribute(Constants.CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
        allowedSessions.remove(sessionId);
        LOG.debug("Logout for session: [{}].", sessionId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
