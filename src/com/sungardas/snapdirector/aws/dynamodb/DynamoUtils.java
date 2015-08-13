package com.sungardas.snapdirector.aws.dynamodb;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.snapdirector.aws.dynamodb.model.BackupEntry;
import com.sungardas.snapdirector.aws.dynamodb.model.TaskEntry;
import com.sungardas.snapdirector.aws.dynamodb.model.User;

/**
 * DynamoUtils is a Repository for static methods which manipulate data in
 * DynamoDB </br>
 * 
 * @author dvas
 *
 */
public class DynamoUtils {

	/**
	 * Returns the list of {@link BackupEntry} objects which represent records
	 * in DB.
	 * 
	 * @param volumeId
	 *            {@link String}
	 * @return {@link List}
	 */
	public static List<BackupEntry> getBackupInfo(String volumeId,
			DynamoDBMapper mapper) {

		BackupEntry backupEntry = new BackupEntry();
		backupEntry.setVolumeId(volumeId);

		DynamoDBQueryExpression<BackupEntry> expression = new DynamoDBQueryExpression<BackupEntry>()
				.withHashKeyValues(backupEntry);

		List<BackupEntry> backupEntries = mapper.query(BackupEntry.class,
				expression);

		return backupEntries;

	}

	public static List<BackupEntry> getBackupInfo(Volume volume,
			DynamoDBMapper mapper) {
		return getBackupInfo(volume.getVolumeId(), mapper);
	}

	public static void putBackupInfo(List<BackupEntry> bakupEntries,
			DynamoDBMapper mapper) {
		
		DynamoDBMapperConfig config = new DynamoDBMapperConfig(
				DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES);
		mapper.batchWrite(bakupEntries, new ArrayList<BackupEntry>(), config);

	}
	
	public static void putbackupInfo(BackupEntry entry, DynamoDBMapper mapper){
		List<BackupEntry> singleEntryList = new ArrayList<BackupEntry>();
		singleEntryList.add(entry);
		
		putBackupInfo(singleEntryList, mapper);
		
	}

	public static boolean removeBackupInfo(String volumeId, String fileName, DynamoDBMapper mapper) {
		BackupEntry remEntry = new BackupEntry();
		remEntry.setVolumeId(volumeId);
		remEntry.setFileName(fileName);
		
		List<FailedBatch> failed = mapper.batchDelete(remEntry);
		System.out.println(failed.isEmpty());
		if (failed.isEmpty()){
			return true;
		}
		
		return false;
	}

	public static boolean authenticateUser(String email, String pass,
			DynamoDBMapper mapper) {
		User user = getUser(email, mapper);
		
		if (user != null && user.getPassword().equals(getPasswordHash(pass))) {
			return true;
		}

		return false;

	}

	public static String getFullUserInfoByEmail(String email, DynamoDBMapper mapper) {
		return getUser(email, mapper).getUserInfo();
	}

	private static User getUser(String email, DynamoDBMapper mapper) {
		User user = new User();
		user.setEmail(email);
		DynamoDBQueryExpression<User> expression = new DynamoDBQueryExpression<User>()
				.withHashKeyValues(user)
				.withConsistentRead(false);

		List<User> userEntries = mapper.query(User.class, expression);

		if (userEntries == null || userEntries.isEmpty()) {
			return null;
		}

		return userEntries.get(0);
	}

	private static String getPasswordHash(String pass) {
		return DigestUtils.sha512Hex(pass);
	}
	
	public static String getTask(String taskId, DynamoDBMapper mapper){
		TaskEntry t = new TaskEntry();
		t.setId(taskId);
		
		DynamoDBQueryExpression<TaskEntry> expression = new DynamoDBQueryExpression<TaskEntry>()
				.withHashKeyValues(t);
		
		List<TaskEntry> taskList = mapper.query(TaskEntry.class, expression);
		
		if (taskList == null || taskList.isEmpty()){
			return null;
		}
		
		return taskList.get(0).toString();
			
	}
	
	public static List<TaskEntry> getTasks(DynamoDBMapper mapper){
		
		DynamoDBScanExpression expression = new DynamoDBScanExpression();
		
		List<TaskEntry> scanResult = mapper.scan(TaskEntry.class, expression);
		
		return scanResult;
		
	}
	
	public static String putTask(TaskEntry task, DynamoDBMapper mapper){
		return putOrDeleteTask(task, null, mapper);
	}
	
	public static String putTask(JSONObject JSONTask, DynamoDBMapper mapper){
		TaskEntry task = new TaskEntry(JSONTask);
		return putOrDeleteTask(task, null, mapper);
	}
	
	public static void deleteTask(String taskId, DynamoDBMapper mapper){
		TaskEntry task = new TaskEntry();
		task.setId(taskId);
		putOrDeleteTask(null, task, mapper);
	}
	
	
	private static String putOrDeleteTask(TaskEntry taskToPut, TaskEntry taskToDelete, DynamoDBMapper mapper){
		String taskToPutId = null;
		List<TaskEntry> singleTaskToPut = new ArrayList<TaskEntry>();
		List<TaskEntry> singleTaskToDelete = new ArrayList<TaskEntry>();
		
		if (taskToPut != null) {
			taskToPutId = GregorianCalendar.getInstance().getTimeInMillis()
					+ taskToPut.getVolume();
			taskToPut.setId(taskToPutId);
			singleTaskToPut.add(taskToPut);
		}
		
		if (taskToDelete != null){
			singleTaskToDelete.add(taskToDelete);
		}
		
		DynamoDBMapperConfig config = new DynamoDBMapperConfig(
				DynamoDBMapperConfig.SaveBehavior.CLOBBER);
		mapper.batchWrite(singleTaskToPut, singleTaskToDelete, config);
		
		return taskToPutId;
	}

}
