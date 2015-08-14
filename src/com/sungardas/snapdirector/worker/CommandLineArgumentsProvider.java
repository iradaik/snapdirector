package com.sungardas.snapdirector.worker;

import org.apache.commons.cli.*;

import java.io.OutputStream;
import java.io.PrintWriter;

public class CommandLineArgumentsProvider {
	private static final String WORKER_ID_ARG = "worker-id";
	private static final String ACESS_KEY_ARG = "access-key";
	private static final String SECRET_KEY_ARG = "secret-key";
	private CommandLine commandLine;
	private String workerInstanceId;
	private String accessKey;
	private String secretKey;

	public CommandLineArgumentsProvider(String[] args) {
		commandLine = parseArguments(getOptions(), args);
		workerInstanceId = commandLine.getOptionValue(WORKER_ID_ARG);
		accessKey = commandLine.getOptionValue(ACESS_KEY_ARG);
		secretKey = commandLine.getOptionValue(SECRET_KEY_ARG);
	}
	
	public boolean hasWorkerInstanceId() {
		return workerInstanceId!=null;
	}
	public String getWorkerInstanceId() {
		return workerInstanceId;
	}

	private static Options getOptions() {
		Option workerId = new Option("w", WORKER_ID_ARG, true, "Worker's configuration identifier");
		workerId.setArgs(1);
		workerId.setArgName("worker instance id");
		workerId.setRequired(true);
		
		
		Option accessKey = new Option(null,  ACESS_KEY_ARG, true, "The AWS assigned access key");
		accessKey.setArgs(1);
		accessKey.setArgName("aws access key");
		accessKey.setRequired(true);
		
		Option secretKey = new Option(null,  SECRET_KEY_ARG, true, "The AWS assigned secret key");
		secretKey.setArgs(1);
		secretKey.setArgName("aws secret key");
		secretKey.setRequired(false);
		
		
		Options opts = new Options();
		opts.addOption(workerId);
		opts.addOption(accessKey);
		opts.addOption(secretKey);
		return opts;
	}

	private CommandLine parseArguments(Options options, String[] commandLineArguments) {
		CommandLine commandLine = null;
		try {
			CommandLineParser cmdLinePosixParser = new DefaultParser();
			commandLine = cmdLinePosixParser.parse(options, commandLineArguments);
		} catch (ParseException e) {
			printHelp(options, 120, "Options:", "", 3, 5, true, System.out);
			System.exit(-1);
		}
		return commandLine;
	}

	private void printHelp(final Options options, final int printedRowWidth, final String header, final String footer,
			final int spacesBeforeOption, final int spacesBeforeOptionDescription, final boolean displayUsage,
			final OutputStream out) {
		final String commandLineSyntax = "java -jar snapdirector.jar";
		final PrintWriter writer = new PrintWriter(out);
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(writer, printedRowWidth, commandLineSyntax, header, options, spacesBeforeOption,
				spacesBeforeOptionDescription, footer, displayUsage);
		writer.flush();
	}

	public String getAwsAccessKey() {
		return accessKey;
	}

	public String getAwsSecretKey() {
		return secretKey;
	}

}
