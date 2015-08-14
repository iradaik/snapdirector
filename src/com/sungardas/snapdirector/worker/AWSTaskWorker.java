package com.sungardas.snapdirector.worker;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.sqs.model.Message;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry;
import com.sungardas.snapdirector.tasks.AWSBackupVolumeTask;
import com.sungardas.snapdirector.tasks.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class AWSTaskWorker implements Runnable {
	public static final Log LOG = LogFactory.getLog(AWSTaskWorker.class);

	private AWSCredentialsProvider awsCredentialsProvider;
	private String region;
	private String routineInstanceId;
	private Item configuration;


	public AWSTaskWorker(AWSCredentialsProvider awsCredentialsProvider, Item configuration, String routineInstanceId) {
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.region = configuration.getString("ec2Region");
		this.routineInstanceId = routineInstanceId;
		this.configuration = configuration;
	}


	@Override
	public void run() {
		
		try {
			while (true) {
				sleep();
				
				List<TaskEntry> tasks = DynamoUtils.getTasks(getMapper()); 
				
				
				for(TaskEntry taskEntry: tasks) {
					if(taskEntry.getStatus().equalsIgnoreCase("waiting")) {
						DynamoUtils.deleteTask(taskEntry.getId(), getMapper());
						taskEntry.setStatus("running");
						 String taskId = DynamoUtils.putTask(taskEntry, getMapper());
						 
						 Task task = new AWSBackupVolumeTask(awsCredentialsProvider, taskEntry.getVolume(), routineInstanceId, new WorkerConfiguration(configuration));
						 task.execute();
						 DynamoUtils.deleteTask(taskId, getMapper());
						 
					break;
					}
					
					sleep();
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