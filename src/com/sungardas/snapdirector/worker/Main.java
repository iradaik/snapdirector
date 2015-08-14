package com.sungardas.snapdirector.worker;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.sungardas.snapdirector.aws.SysProperiesBasedCredentialsProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	public static final Log LOG = LogFactory.getLog(Main.class);
	public static void main(String[] args) {
		String instanceId = null;
		CommandLineArgumentsProvider argsProvider = new CommandLineArgumentsProvider(args);
		
		if(argsProvider.hasWorkerInstanceId()) {
			instanceId = argsProvider.getWorkerInstanceId();
		}
		else {
			instanceId = retrieveInstanceIdFromMetadata();
		}
		
		AWSCredentialsProvider awsCredentialsProvider =  new SysProperiesBasedCredentialsProvider(argsProvider);
		
		
		DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(awsCredentialsProvider));
		Table workerConfigurationTable = dynamoDB.getTable("WorkerConfiguration");
		Item configuration = workerConfigurationTable.getItem("workerId", instanceId);
		LOG.info("Loaded Worker Configuration: " + configuration.toJSON()+"\n");
		checkFakeBackupSourceUsage(configuration);
		
		
		ExecutorService  executor = Executors.newCachedThreadPool();
		executor.execute(new AWSTaskWorker(awsCredentialsProvider,configuration, instanceId));
		executor.execute(new AWSTaskManager(awsCredentialsProvider,configuration));
		
		
		
	}
	
	private static String retrieveInstanceIdFromMetadata() {
		String instanceId = null;
		try {
			URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
			URLConnection conn = url.openConnection();
			Scanner s = new Scanner(conn.getInputStream());
			if (s.hasNext()) {
				instanceId = s.next();
				LOG.info("Getting Worker InstanceId from metadata: " + instanceId);
			}
			s.close();
		} catch (IOException e) {
			LOG.info("Can't get InstanceId from metadata.");
			System.exit(0);
		}
		return instanceId;

	}
	
	private static void checkFakeBackupSourceUsage(Item configuration) {
		String fakeBackupSource;	
		if ( configuration.getBoolean("useFakeBackup") &&
				((fakeBackupSource= configuration.getString("fakeBackupSource"))!=null)) {
				if(new File(fakeBackupSource).exists()) {
					LOG.info("Fake backup source will be used to test flow");
				} else {
					LOG.info("Can't find fake backup file.");
					System.exit(0);
				}
				
			}
		return;
	}
	
	
	
	
	

}
