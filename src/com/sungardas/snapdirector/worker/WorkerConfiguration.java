package com.sungardas.snapdirector.worker;

import com.amazonaws.services.dynamodbv2.document.Item;

import java.util.Map;

public class WorkerConfiguration {
	private Map<String,String> sdfsVolume;
	private Map<String,String> configPathes;
	private String ec2Region;
	private String workerId;
	
	private boolean useFakeBackup;
	private boolean useFakeEC2;
	private String fakeBackupSource;
	
	public WorkerConfiguration(Item workerConfiguration) {
		sdfsVolume = workerConfiguration.getMap("sdfsVolume");
		configPathes = workerConfiguration.getMap("sdfsConfigPathes");
		ec2Region = workerConfiguration.getString("ec2Region");
		workerId = workerConfiguration.getString("workerId");
		
		useFakeBackup = workerConfiguration.getBoolean("useFakeBackup");
		fakeBackupSource = workerConfiguration.getString("fakeBackupSource");
		useFakeEC2 = workerConfiguration.getBoolean("useFakeEC2");
	}

	public String getSdfsVolumeName() {
		return sdfsVolume.get("volumeName");
	}

	public String getSdfsMountPoint() {
		return sdfsVolume.get("mountPoint");
		
	}

	public String getEc2Region() {
		// TODO Auto-generated method stub
		return ec2Region;
	}

	public String getInstanceId() {
		
		return workerId;
	}

	public boolean isFakeBackup() {
		return useFakeBackup;
		
	}
	
	public boolean isFakeEC2() {
		return useFakeEC2;
		
	}

	public String getFakeBackupSource() {
		return fakeBackupSource;
	}

}
