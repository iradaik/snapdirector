package com.sungardas.snapdirector.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.util.json.Jackson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@DynamoDBTable(tableName="Users")
public class User {

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	
	public User(){
		super();
	}
	
	public User(String userName, String lastName, String fullName, String email, String password, List<String> groups){
		this.setUserName(userName);
		this.setLastName(lastName);
		this.setFullName(fullName);
		this.setEmail(email);
		this.setPassword(password);
		this.setGroups(groups);
	}
	
	@DynamoDBHashKey(attributeName="userName")
	public String getUserName(){
		return (String) attributes.get("username");
	}
	public void setUserName(String userName){
		attributes.put("username", userName);
	}
	
	
	@DynamoDBAttribute(attributeName="password")
	public String getPassword(){
		return (String) attributes.get("password");
	}
	public void setPassword(String password){
		attributes.put("password", password);
	}
	
	@DynamoDBIndexHashKey(globalSecondaryIndexName="email-index")
	public String getEmail(){
		return (String) attributes.get("email");
	}
	public void setEmail(String email){
		attributes.put("email", email);
	}
	
	@DynamoDBAttribute(attributeName="fullName")
	public String getFullName(){
		return (String) attributes.get("fullname");
	}
	public void setFullName(String fullName){
		attributes.put("fullname", fullName);
	}
	
	@DynamoDBAttribute(attributeName="lastName")
	public String getLastName(){
		return (String) attributes.get("lastname");
	}
	public void setLastName(String lastName){
		attributes.put("lastname", lastName);
	}
	
	@SuppressWarnings("unchecked")
	@DynamoDBAttribute(attributeName="groups")
	public List<String> getGroups(){
		return (List<String>) attributes.get("groups");
	}
	public void setGroups(List<String> groups){
		attributes.put("groups", groups);
	}
	
	@Override
	public String toString() {
		return getUserInfo();
	}
	
	public String getUserInfo() {
		Map<String, Object> passwordlessUserInfo = new LinkedHashMap<String, Object>();
		passwordlessUserInfo.putAll(attributes);
		passwordlessUserInfo.remove("password");
		return Jackson.toJsonString(passwordlessUserInfo);
	}
	
}
