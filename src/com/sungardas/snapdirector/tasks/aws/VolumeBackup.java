package com.sungardas.snapdirector.tasks.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.sungardas.snapdirector.tasks.aws.sdfs.SdfsConfigPathes;
import com.sungardas.snapdirector.tasks.aws.sdfs.utils.S3Utils;
import com.sungardas.snapdirector.tasks.aws.sdfs.utils.SdfsProcess;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


public class VolumeBackup {

	public static final Log LOG = LogFactory.getLog(VolumeBackup.class);
	static SdfsConfigPathes sdfsConfigPathes;

	public static Region region;
	public static Properties properties;


	public static void main(String[] commandLinaArguments) throws IOException {

		CommandLine commandLine = parseArguments(getOptions(), commandLinaArguments);
		properties = loadProperties(commandLine.getOptionValues("c")[0]);
		sdfsConfigPathes = initSdfsConfigPathes(properties);
		SdfsProcess sdfsProc=null;
		if (commandLine.hasOption("backup")) {
			sdfsProc = new SdfsProcess(null, properties);

			if (sdfsProc.alreadyRunning()) {
				LOG.error(format("SDFS pool %s is already mounted", properties.getProperty("AWS_POOL")));
				System.exit(-1);
			}

			if (commandLine.hasOption("check-md5")) {
				properties.setProperty("CHECK_MD5SUM", "true");
			}

			if (commandLine.hasOption("first-start")) {
				sdfsProc.packSdfsStateAndUpload(newAmazonS3Client(properties));
			}

			backupFlow(properties);
		} else if (commandLine.hasOption("restore")) {
			{
				restoreFlow();
			}
		}

	}


	private static CommandLine parseArguments(Options options, String[] commandLinaArguments) {
		CommandLine commandLine = null;
		try {
			CommandLineParser cmdLinePosixParser = new PosixParser();
			commandLine = cmdLinePosixParser.parse(options, commandLinaArguments);
		} catch (ParseException e) {
			printHelp(options, 120, "Options:", "", 3, 5, true, System.out);
			System.exit(-1);
		}
		return commandLine;
	}


	private static void restoreFlow() {
		// TODO Auto-generated method stub

	}


	public static void backupFlow(Properties properties) throws IOException {

		AmazonS3 s3client = newAmazonS3Client(properties);
		AmazonEC2 ec2client = newAmazonEC2Client(properties);

		Region region = Region.getRegion(Regions.fromName(properties.getProperty("REGION")));
		ec2client.setRegion(region);

		// restore sdfs state
		LOG.info(format("---==Restoring SDFS state==---"));
		SdfsProcess sdfsProc = new SdfsProcess(null, properties);
//		sdfsProc.downloadAndRestoreSdfs(s3client);

		// starting sdfs
//		LOG.info(format("---==Starting SDFS==---"));
		sdfsProc.mountsdfs();

		// prepare temp volume to backup
		LOG.info(format("---==Prepare volume for backup==---"));
		Volume volumeToBackup = createAndAttachBackupVolume(ec2client, properties.getProperty("VOLUME_TO_BACKUP"), null);

		// create volume backup
		LOG.info(format("\n---==Backuping volume to SDFS==---"));
//		if (properties.getProperty("CHECK_MD5SUM") != null && properties.getProperty("CHECK_MD5SUM").equals("true")) {
//			sdfsProc.backupVolumeToSdfs(properties.getProperty("BACKUP_SRC"), properties.getProperty("BACKUP_NAME"),
//					true);
//		} else {
//			sdfsProc.backupVolumeToSdfs(properties.getProperty("BACKUP_SRC"), properties.getProperty("BACKUP_NAME"),
//					false);
//		}

		// unattach and delete temp volume and related objects
		LOG.info(format("\n---==Clean volume and related snapshot==---"));
		volumeToBackup.getSnapshotId();
		detachAndDeleteVolume(ec2client, volumeToBackup);

		// stop sdfs
//		LOG.info(format("\n---==Stoping SDFS sdfs==---"));
//		sdfsProc.umountsdfs();

//		// backup sdfs state
//		LOG.info(format("\n---==Backuping SDFS state==---"));
//		sdfsProc.packSdfsStateAndUpload(s3client);
	}


	private static AmazonS3Client newAmazonS3Client(Properties properties) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(properties.getProperty("ACCESS_KEY"),
				properties.getProperty("SECRET_KEY"));
		return new AmazonS3Client(awsCreds);
	}


	private static AmazonEC2Client newAmazonEC2Client(Properties properties) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(properties.getProperty("ACCESS_KEY"),
				properties.getProperty("SECRET_KEY"));
		return new AmazonEC2Client(awsCreds);
	}


	private static Properties loadProperties(String pathToPropertyFile) {
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(pathToPropertyFile);
			properties.load(fis);
			fis.close();
			LOG.info("\nUsed configuration:");
			printProperties(properties);
		} catch (IOException e) {
			LOG.error("Cant read property file: " + pathToPropertyFile);
			LOG.error(e.getMessage());
		}
		return properties;
	}


	private static void printProperties(Properties properties) {
		Set<Entry<Object, Object>> entries = properties.entrySet();
		for (Entry<Object, Object> e : entries) {
			System.out.println(e.getKey().toString() + " : " + e.getValue().toString());
		}
	}


	public static SdfsConfigPathes initSdfsConfigPathes(Properties properties) {
		SdfsConfigPathes configPathes = new SdfsConfigPathes();
		configPathes.setChunkStorePath(properties.getProperty("CHUNK_STORE_PATH"));
		configPathes.setClusterConfigPath(properties.getProperty("CLUSTER_CONFIG_PATH"));
		configPathes.setDedupDbStorePath(properties.getProperty("DEDUP_DB_STORE_PATH"));
		configPathes.setHashDBStorePath(properties.getProperty("HASH_DB_STORE_PATH"));
		configPathes.setIOLogPath(properties.getProperty("IO_LOG_PATH"));
		configPathes.setPoolConfigPath(properties.getProperty("POOL_CONFIG_PATH"));
		configPathes.setVolumeKeysPath(properties.getProperty("VOLUME_KEYSTORE"));
		configPathes.setVolumePath(properties.getProperty("VOLUME_PATH"));
		return configPathes;
	}


//	public static void packSdfsStateAndUpload(AmazonS3 s3client) {
//		LOG.info(format("\nCreating SDFS configuration & metadata archive"));
//		TarUtils.packToTar(properties.getProperty("SDFS_BACKUP_UPLOAD_FILE"), sdfsConfigPathes.getPathes());
//		LOG.info(format("\nUploading SDFS configuration & metadata archiveto S3"));
//		S3Utils.upload(s3client, properties.getProperty("BUCKET_NAME"),
//				properties.getProperty("SDFS_BACKUP_UPLOAD_FILE"), properties.getProperty("KEY_NAME"));
//	}
//
//
//	public static void downloadAndRestoreSdfs(AmazonS3 s3client) {
//		LOG.info(format("\nDownloading SDFS configuration & metadata from S3"));
//		S3Utils.download(s3client, properties.getProperty("BUCKET_NAME"),
//				properties.getProperty("SDFS_BACKUP_DOWNLOAD_FILE"), properties.getProperty("KEY_NAME"));
//		LOG.info(format("\nRestoring SDFS configuration & metadata from archive"));
//		TarUtils.unpackFromTar(properties.getProperty("SDFS_BACKUP_DOWNLOAD_FILE"));
//	}


	public static Volume createAndAttachBackupVolume(AmazonEC2 ec2client, String volumeId, String instanceId) {
		Instance instance = getInstance(ec2client, instanceId);
		if (instance == null) {
			LOG.error("\nCan't get access to " + instanceId + " instance");
			System.exit(-1);
		}
		LOG.info("\ninst:" + instance);
		
		// create snapshot for AMI
		Volume volumeSrc = getVolume(ec2client, volumeId);
		if (volumeSrc == null) {
			LOG.error("\nCan't get access to " + volumeId + " volume");
			System.exit(-1);
		}
		
		
		Snapshot snapshot = S3Utils.createSnapshot(ec2client, volumeSrc);
//		try {
//			TimeUnit.SECONDS.sleep(120);
//		} catch (InterruptedException e) {}
		
//		S3Utils.waitForCompleteState(ec2client, snapshot);
		LOG.info("\nSnapshot created. Check snapshot data:\n" + snapshot.toString());

		// create volume
		String instanceAvailabilityZone = instance.getPlacement().getAvailabilityZone();
		Volume volumeDest = S3Utils.createVolumeFromSnapshot(ec2client, snapshot, instanceAvailabilityZone);
//		
//		try {
//			TimeUnit.SECONDS.sleep(120);
//		} catch (InterruptedException e) {}
		
		//volumeDest = S3Utils.waitForAvailableState(ec2client, volumeDest);
		LOG.info("\nVolume created. Check volume data:\n" + volumeDest.toString());

		// mount AMI volume
		S3Utils.attachVolume(ec2client, instance, volumeDest);
		return S3Utils.syncVolume(ec2client, volumeDest);
	}
	


	private static Volume getVolume(AmazonEC2 ec2client, String volumeId) {
		LinkedList<String> volumeIds = new LinkedList<String>();
		volumeIds.add(volumeId);
		DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest(volumeIds);
		DescribeVolumesResult describeVolumesResult = ec2client.describeVolumes(describeVolumesRequest);
		if (describeVolumesResult.getVolumes().size() > 0) {
			return describeVolumesResult.getVolumes().get(0);
		}
		return null;
	}


	private static Instance getInstance(AmazonEC2 ec2client, String instanceId) {
		LinkedList<String> instanceIds = new LinkedList<String>();
		instanceIds.add(instanceId);
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		describeInstancesRequest.setInstanceIds(instanceIds);
		DescribeInstancesResult describeInstancesResult = ec2client.describeInstances(describeInstancesRequest);
		List<Reservation> reservations = describeInstancesResult.getReservations();
		List<Instance> insts = new LinkedList<Instance>();
		for (Reservation r : reservations) {
			insts.addAll(r.getInstances());
		}

		if (insts.size() > 0) {
			return insts.get(0);
		}

		return null;
	}


	public static void detachAndDeleteVolume(AmazonEC2 ec2client, Volume volume) {
		// Detaching volume
		LOG.info("Detaching volume: " + volume.getVolumeId());
		S3Utils.unattachVolume(ec2client, volume);
		
		try {
			TimeUnit.SECONDS.sleep(120);
		} catch (InterruptedException e) {}
		
		
		Volume unattachedVol = S3Utils.waitForAvailableState(ec2client, volume);
		do {
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			S3Utils.syncVolume(ec2client, volume);
		} while (unattachedVol.getAttachments().size() > 0);
		LOG.info(String.format("Volume %s detached", volume.getVolumeId()));

		// Delete snapshot
		S3Utils.deleteSnapshot(ec2client, volume);

		// Delete volume
		S3Utils.deleteVolume(ec2client, volume);
	}


	private static Options getOptions() {
		Option configFileOption = new Option("c", "configuration", true, "Path to configuration file");
		configFileOption.setArgs(1);
		configFileOption.setArgName("property file");
		configFileOption.setRequired(true);

		Option firstStartOption = new Option("f", "first-start", false,
				"Used for first start when there is no SDFS state previously backuped");
		firstStartOption.setRequired(false);

		Option md5Option = new Option("md5", "check-md5", false, "Check MD5 for source file/volume and backup file ");
		firstStartOption.setRequired(false);

		Option backupFlowOption = new Option("b", "backup", false, "Run backup process");

		Option restoreFlowOption = new Option("r", "restore", false, "Run restore process; NOT IMPLEMENTED");
		OptionGroup flowGroup = new OptionGroup();
		flowGroup.addOption(backupFlowOption);
		flowGroup.addOption(restoreFlowOption);

		flowGroup.setRequired(true);

		Options posixOptions = new Options();

		posixOptions.addOption(configFileOption);
		posixOptions.addOption(firstStartOption);
		posixOptions.addOption(md5Option);
		posixOptions.addOptionGroup(flowGroup);

		return posixOptions;
	}


	private static void printHelp(final Options options, final int printedRowWidth, final String header,
			final String footer, final int spacesBeforeOption, final int spacesBeforeOptionDescription,
			final boolean displayUsage, final OutputStream out) {
		final String commandLineSyntax = "java -jar ./sdfs-backup.jar";
		final PrintWriter writer = new PrintWriter(out);
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(writer, printedRowWidth, commandLineSyntax, header, options, spacesBeforeOption,
				spacesBeforeOptionDescription, footer, displayUsage);
		writer.flush();
	}
}
