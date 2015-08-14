package com.sungardas.snapdirector.quartz;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TestJob implements Job {

    @Override
    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        System.out.println("Executing Job");
        
        AWSCredentials credentials = null;
        try {
            credentials = new EnvironmentBasedCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\ogor\\.aws\\credentials), and is in valid format.",
                    e);
        }

        AmazonSQS sqs = new AmazonSQSClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        sqs.setRegion(usWest2);
        
        try {
        	String myQueueUrl=null;
        	//Check that MyQueue is not existed
        	if (sqs.listQueues("MyQueue").getQueueUrls().size()==0) {
        		// Create a queue
                System.out.println("Creating a new SQS queue called MyQueue.\n");
                CreateQueueRequest createQueueRequest = new CreateQueueRequest("MyQueue");
                myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
        	} else {
        		// get queue url for MyQueue
        		myQueueUrl = sqs.listQueues("MyQueue").getQueueUrls().get(0);
        	}
        	
				sqs.sendMessage(new SendMessageRequest(myQueueUrl, "DO_BACKUP"));
				
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

    }

}