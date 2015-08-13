package com.sungardas.snapdirector.rest;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry;
import com.sungardas.snapdirector.tasks.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static java.lang.String.format;

@RestController
@RequestMapping("/task")
public class TaskController {

    private static final Logger LOG = LogManager.getLogger(TaskController.class);

    @Context
    ServletContext context;
    @Context
    private HttpServletRequest servletRequest;


    @RequestMapping(method = RequestMethod.GET)
    public String getTasks() throws ParseException {
        String result = null;
        //try {
        //addTask(null);
        List<TaskEntry> taskModels = DynamoUtils.getTasks(getMapper(servletRequest));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        JSONArray tasks = new JSONArray();
        for(TaskEntry nextTask:taskModels) {
            JSONObject jsonTask = new JSONObject();
            jsonTask.put("id", nextTask.getId());
            jsonTask.put("priority",nextTask.getPriority());
            jsonTask.put("schedulerManual",Boolean.valueOf(nextTask.getSchedulerManual()));
            jsonTask.put("schedulerName",nextTask.getSchedulerName());
            jsonTask.put("schedulerTime",Long.valueOf(format.parse(nextTask.getSchedulerTime()).getTime()));
            jsonTask.put("status",nextTask.getStatus());
            jsonTask.put("type",nextTask.getType());
            jsonTask.put("volume",nextTask.getVolume());
            tasks.put(jsonTask);
        }

        return tasks.toString();
    }

    private DynamoDBMapper getMapper(ServletRequest request) {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(new EnvironmentBasedCredentialsProvider());
        String region = request.getServletContext().getInitParameter("aws:dynamodb-region");
        client.setRegion(Region.getRegion(Regions.fromName(region)));
        return new DynamoDBMapper(client);
    }


    @RequestMapping(method = RequestMethod.POST)
    public String addTask(String task) {
        LOG.info("put message:" + task);
        String sqsRegion = context.getInitParameter("aws:sqs-region");
        String queueURL = context.getInitParameter("aws:sqs-queue-url");
        AmazonSQS sqs = new AmazonSQSClient(new EnvironmentBasedCredentialsProvider());
        Region usWest2 = Region.getRegion(Regions.fromName(sqsRegion));
        sqs.setRegion(usWest2);

        //String path = context.getInitParameter("rest:mock-directory");
        //String body = newTask.toString();
        String body = task;
        try {
            SendMessageRequest sendRequest = new SendMessageRequest(queueURL, body);
            sendRequest.setDelaySeconds(0);
            SendMessageResult sendResult = sqs.sendMessage(sendRequest);
            LOG.info(format("TaskRestService: sended message: %s; body:%s", sendResult.getMessageId(),body)  );
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
        return null;
    }
}
