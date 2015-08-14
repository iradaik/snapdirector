package com.sungardas.snapdirector.worker;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class AWSTaskManager implements Runnable {
	public static final Log LOG = LogFactory.getLog(AWSTaskManager.class);
	private AWSCredentialsProvider awsCredentialsProvider;
	private String queueURL;
	private String region;
	private Item configuration;
	
	public AWSTaskManager(AWSCredentialsProvider awsCredentialsProvider, Item configuration) {
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.queueURL = configuration.getString("taskQueueURL");
		this.region = configuration.getString("ec2Region");
		this.configuration = configuration;
	}

	@Override
	public void run() {
		AmazonSQS sqs = newAmazonSQSClient();
		LOG.info(format("Starting listening tasks queue: %s", queueURL));
		
		try {
			
			while (true) {
				sleep();
				TaskEntry taskEntry = null;
				
				ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL);
				List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
				if(messages!=null && messages.size()>0)
					LOG.info(format("Resieved %d messages from tasks queue\n", messages.size()));
				
				for (Message message : messages) {
					try {
						taskEntry = createTaskEntry(message);
						if(taskEntry!=null) {
							LOG.info(format("Handling message : %s : %s",message.getMessageId(), taskEntry.toString()));
							DynamoUtils.putTask(taskEntry, getMapper());	
						}
						
						
					} catch (JSONException e) {	e.printStackTrace(); }
					
					// Delete a message
					LOG.info(format("AWSTaskWorker: message deleted: %s", message.getMessageId()));
					String messageRecieptHandle = message.getReceiptHandle();
					sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageRecieptHandle));
				}
			
			}
			
		} catch (AmazonServiceException ase) {
			printASE(ase);
		} catch (AmazonClientException ace) {
			printACE(ace);
		}
		
	}
	
	private TaskEntry createTaskEntry(Message message) {
		JSONObject jsonTask=null;
		com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry taskEntity = null;
		try {
			jsonTask = new JSONObject(message.getBody());
			if(jsonTask.getString("type").equalsIgnoreCase("backup"))	{
				taskEntity = new com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry(jsonTask);
			}
		} catch (JSONException e) {e.printStackTrace();	}
		return taskEntity;
	}
	
	private AmazonSQS newAmazonSQSClient() {
		AmazonSQS sqs = new AmazonSQSClient(awsCredentialsProvider);
		Region sqsRegion = Region.getRegion(Regions.fromName(region));
		sqs.setRegion(sqsRegion);
		return sqs;
	}
	
	private void sleep() {
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {	e.printStackTrace(); }
	}
	
	private void printASE(AmazonServiceException ase) {
		System.out.println("Caught an AmazonServiceException, which means your request made it "
				+ "to Amazon SQS, but was rejected with an error response for some reason.");
		System.out.println("Error Message:    " + ase.getMessage());
		System.out.println("HTTP Status Code: " + ase.getStatusCode());
		System.out.println("AWS Error Code:   " + ase.getErrorCode());
		System.out.println("Error Type:       " + ase.getErrorType());
		System.out.println("Request ID:       " + ase.getRequestId());
	}
	
	private void printACE(AmazonClientException ace) {
		System.out.println("Caught an AmazonClientException, which means the client encountered "
				+ "a serious internal problem while trying to communicate with SQS, such as not "
				+ "being able to access the network.");
		System.out.println("Error Message: " + ace.getMessage());
	}
	
	private DynamoDBMapper getMapper() {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
		return new DynamoDBMapper(client);
	}

}
