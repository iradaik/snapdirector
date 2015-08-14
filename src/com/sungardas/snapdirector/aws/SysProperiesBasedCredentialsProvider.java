package com.sungardas.snapdirector.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.sungardas.snapdirector.worker.CommandLineArgumentsProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;

public class SysProperiesBasedCredentialsProvider implements
		AWSCredentialsProvider {
	private static final Log LOG = LogFactory.getLog(SysProperiesBasedCredentialsProvider.class);
	private BasicAWSCredentials credentials;
	private CommandLineArgumentsProvider argsProvider;

	public SysProperiesBasedCredentialsProvider(CommandLineArgumentsProvider argsProvider) {
		this.argsProvider = argsProvider;
	}
	
	@Override
	public AWSCredentials getCredentials() {
		if (credentials == null)
			refresh();
		return credentials;
	}

	@Override
	public void refresh() {
		String accessKey, secretKey;
		accessKey = argsProvider.getAwsAccessKey();
		secretKey = argsProvider.getAwsSecretKey();
		
		if (accessKey != null && secretKey != null) {
			credentials = new BasicAWSCredentials(accessKey, secretKey);
		}else {
			String message = format("There are no AWS credentials werenot provided for application.");
			RuntimeException noCredVariables = new RuntimeException(message);
			LOG.error(message,noCredVariables);
		}
	}

}
