package com.sungardas.snapdirector.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.util.json.Jackson;

import java.util.LinkedHashMap;
import java.util.Map;

@DynamoDBTable(tableName = "BackupList")
final public class BackupEntry {
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	
	public BackupEntry() {
		super();
	}
	
	public BackupEntry(String volumeId, String fileName, String timeCreated, String size, BackupState state){
		this.setVolumeId(volumeId);
		this.setFileName(fileName);
		this.setTimeCreated(timeCreated);
		this.setSize(size);
		this.setState(state);
	}
	
	
	@DynamoDBHashKey(attributeName = "volumeId")
	public String getVolumeId() {
		return (String) attributes.get("volumeId");
	}

	public void setVolumeId(String volumeId) {
		attributes.put("volumeId", volumeId);

	}

	@DynamoDBRangeKey(attributeName = "fileName")
	public String getFileName() {
		return (String) attributes.get("fileName");
	}

	public void setFileName(String fileName) {
		attributes.put("fileName", fileName);
	}


	@DynamoDBAttribute(attributeName = "timeCreated")
	public String getTimeCreated() {
		return (String) attributes.get("timeCreated");
	}

	public void setTimeCreated(String timeCreated) {
		attributes.put("timeCreated", timeCreated);
	}
	
	@DynamoDBAttribute(attributeName = "size")
	public String getSize() {
		return (String) attributes.get("size");
	}

	public void setSize(String size) {
		attributes.put("size", size);
	}
	
	@DynamoDBAttribute(attributeName = "state")
	public String getState() {
		return (String) attributes.get("state");
	}

	public void setState(BackupState state) {
		attributes.put("state", state.getState());
	}

	@Override
	public String toString() {
		return Jackson.toJsonString(attributes);
	}

	@Override
	public boolean equals(Object obj) {

		if (obj != null && obj instanceof BackupEntry) {
			if (this.getFileName().equals(((BackupEntry) obj).getFileName())
					&& this.getTimeCreated().equals(
							((BackupEntry) obj).getTimeCreated())
					&& this.getVolumeId().equals(
							((BackupEntry) obj).getVolumeId())
					&& this.getSize().equals(
						((BackupEntry) obj).getSize()))
				return true;
		}

		return false;
	}
}
