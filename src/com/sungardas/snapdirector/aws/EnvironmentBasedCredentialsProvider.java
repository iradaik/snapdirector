package com.sungardas.snapdirector.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

import static java.lang.String.format;

public class EnvironmentBasedCredentialsProvider implements
		AWSCredentialsProvider {
	private static final Log LOG = LogFactory.getLog(EnvironmentBasedCredentialsProvider.class);
	private static final String ACCESS_KEY_ENV_VARIABLE = "AWS_SANGARDAS_ACCESS_KEY";
	private static final String SECRET_KEY_ENV_VARIABLE = "AWS_SANGARDAS_SECRET_KEY";
	private static BasicAWSCredentials credentials;

	@Override
	public AWSCredentials getCredentials() {
		if (credentials == null)
			refresh();
		return credentials;
	}

	@Override
	public void refresh() {
		String accessKey, secretKey;
		Map<String, String> env = System.getenv();

		accessKey = env.get(ACCESS_KEY_ENV_VARIABLE);
		secretKey = env.get(SECRET_KEY_ENV_VARIABLE);
		if (accessKey != null && secretKey != null) {
			credentials = new BasicAWSCredentials(accessKey, secretKey);
		}else {
			String message = format("There are no AWS credentials environment variables. Register %s and %s",ACCESS_KEY_ENV_VARIABLE, SECRET_KEY_ENV_VARIABLE);
			RuntimeException noCredVariables = new RuntimeException(message);
			LOG.error(message,noCredVariables);
		}

	}

}
