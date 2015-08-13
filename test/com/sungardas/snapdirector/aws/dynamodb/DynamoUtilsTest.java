package com.sungardas.snapdirector.aws.dynamodb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.sungardas.snapdirector.aws.EnvironmentBasedCredentialsProvider;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.BackupEntry;
import com.sungardas.snapdirector.aws.dynamodb.model.BackupState;

public class DynamoUtilsTest {

	
	private AmazonDynamoDBClient client = new AmazonDynamoDBClient(new EnvironmentBasedCredentialsProvider());
	private DynamoDBMapper mapper = new DynamoDBMapper(client);
	
	@Test
	public void testGetVolumeBackups() {
		
		
		List<BackupEntry> items = DynamoUtils.getBackupInfo("vol-69dee6a0", this.mapper);

		assertNotNull(items);
		assertFalse(items.isEmpty());
		
	}
	
	@Test
	public void testPutBackupInfo() {
		
		double salt = Math.random();
		
		BackupEntry newBackup = new BackupEntry("vol-69dee6a0" + salt, "vol-69dee6a0111.backup","201507311025", "111111", BackupState.INPROGRESS);
		newBackup.setVolumeId("vol-69dee6a0" + salt);
		newBackup.setFileName("vol-69dee6a0111.backup");
		newBackup.setTimeCreated("201507311025");

		
		List<BackupEntry> items = new ArrayList<BackupEntry>();
		items.add(newBackup);

		DynamoUtils.putBackupInfo(items, mapper);
		
		BackupEntry fetched = DynamoUtils.getBackupInfo("vol-69dee6a0" + salt, mapper).get(0);
		
		assertNotNull(fetched);
		assertTrue(newBackup.equals(fetched));
		
	}
	
	@Test
	public void testRemoveBackupInfo() {
		
		
		boolean res = DynamoUtils.removeBackupInfo("vol-69dee6a00.9619267669690094", "vol-69dee6a0111.backup", this.mapper);
		boolean res1 = DynamoUtils.removeBackupInfo("vol-69dee6a0qweqwe", "vol-69dee6a0ssss.backup", this.mapper);

		assertTrue(res);
		assertFalse(res1);
		
	}
	
	
	@Test
	public void testAuthenticateUser(){
		boolean info = DynamoUtils.authenticateUser("admin@sungard.com", "admin", mapper);
		assertNotNull(info);
		assertTrue(info);
		
	}
	
	@Test
	public void testGetFullUserInfo(){
		String info = DynamoUtils.getFullUserInfoByEmail("admin@sungard.com", mapper);
		assertNotNull(info);
		assertTrue(!info.isEmpty());
		
	}
	
	
	
}
