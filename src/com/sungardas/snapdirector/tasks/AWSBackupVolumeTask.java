package com.sungardas.snapdirector.tasks;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.snapdirector.aws.dynamodb.DynamoUtils;
import com.sungardas.snapdirector.aws.dynamodb.model.BackupEntry;
import com.sungardas.snapdirector.aws.dynamodb.model.BackupState;
import com.sungardas.snapdirector.tasks.aws.VolumeBackup;
import com.sungardas.snapdirector.tasks.aws.sdfs.utils.SdfsManager;
import com.sungardas.snapdirector.worker.WorkerConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import static java.lang.String.format;


public class AWSBackupVolumeTask implements Task {
	//TODO: remove temporary counter, use dbdata instead
	public static int taskId = 0;
	
	public static final Log LOG = LogFactory.getLog(AWSBackupVolumeTask.class);
	private AWSCredentialsProvider awsCredentialsProvider;
	private String volumeId;
	private WorkerConfiguration configuration;
	;


	public AWSBackupVolumeTask(AWSCredentialsProvider awsCredentialsProvider, String volumeId, String routineInstanceId,WorkerConfiguration configuration) {
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.volumeId = volumeId;
		this.configuration = configuration;
		LOG.info(format("AWSBackupVolumeTask[%d]: Initialized backup process for volume %s",taskId, volumeId));
	}


	public void execute() {
		LOG.info(format("AWSBackupVolumeTask[%d]: Starting backup process for volume %s",taskId, volumeId));
		AmazonEC2Client ec2client = new AmazonEC2Client(awsCredentialsProvider);
		SdfsManager sdfs = new SdfsManager( configuration);
		String ec2Region = configuration.getEc2Region();
		ec2client.setRegion(Region.getRegion(Regions.fromName(ec2Region)));
		
		Volume tempVolume = null;
		String attachedDeviceName = null;
		if(!configuration.isFakeEC2()){
			tempVolume = VolumeBackup.createAndAttachBackupVolume(ec2client, volumeId, configuration.getInstanceId());
			attachedDeviceName = tempVolume.getAttachments().get(0).getDevice();
		}
		
		String backupDate = String.valueOf(System.currentTimeMillis());
		String backupfileName = volumeId + "."+backupDate+".backup";
		
		BackupEntry backup = new BackupEntry(volumeId, backupfileName, backupDate, "", BackupState.INPROGRESS);
		DynamoUtils.putbackupInfo(backup, getMapper());
		
		boolean backupStatus = false;
		try {
			if( configuration.isFakeBackup() ) {
				backupStatus = sdfs.backupVolumeToSdfs(configuration.getFakeBackupSource(), backupfileName);
			}
			else if(!configuration.isFakeEC2()){
				backupStatus = sdfs.backupVolumeToSdfs(attachedDeviceName, backupfileName);
				
			}
		} catch (IOException e) {
			LOG.fatal(format("Backup of volume %s failed", volumeId));
			backup.setState(BackupState.FAILED);
			DynamoUtils.putbackupInfo(backup, getMapper());
		}
		
		if(backupStatus) {
			long backupSize=sdfs.getBackupSize(backupfileName);
			LOG.info("Backup creation time: " + sdfs.getBackupCreationTime(backupfileName));
			LOG.info("Backup size: " + backupSize);
			
			
			LOG.info("Put backup entry to the Backup List: "+backup.toString());
			backup.setState(BackupState.COMPLETED);
			backup.setSize(String.valueOf(backupSize));
			DynamoUtils.putbackupInfo(backup, getMapper());
		}
		
		
		
		
		
		if (!configuration.isFakeEC2()) {
			VolumeBackup.detachAndDeleteVolume(ec2client, tempVolume);
		}
		

		LOG.info(format("Backup process for volume %s finished successfully ",taskId, volumeId));
	}
	
	
	private DynamoDBMapper getMapper() {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
		String region = configuration.getEc2Region();
		client.setRegion(Region.getRegion(Regions.fromName(region)));
		return new DynamoDBMapper(client);
	}
	
	
	
}
