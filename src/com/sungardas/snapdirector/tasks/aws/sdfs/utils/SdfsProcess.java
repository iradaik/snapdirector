package com.sungardas.snapdirector.tasks.aws.sdfs.utils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import com.sungardas.snapdirector.tasks.aws.sdfs.SdfsConfigPathes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


public class SdfsProcess {

	public static final Log LOG = LogFactory.getLog(SdfsProcess.class);
	private Process mountsdfsProcess;
	private Process umountsdfsProcess;
	private String sdfs;
	private String mountPoint;
	private SdfsState state;
	
	private SdfsConfigPathes sdfsConfigPathes;
	private Properties properties;
	private String processID;


	public SdfsProcess(String processID, Properties properties) {
		this.sdfs = properties.getProperty("AWS_POOL");
		this.mountPoint = properties.getProperty("AWS_POOL_MOUNT_POINT");
		state = SdfsState.Unmounted;
		this.properties = properties;
		this.sdfsConfigPathes = initSdfsConfigPathes(properties);
		this.processID = processID;
	}
	
	private static SdfsConfigPathes initSdfsConfigPathes(Properties properties) {
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

	public void packSdfsStateAndUpload(AmazonS3 s3client) {
		LOG.info(format("\nCreating SDFS configuration & metadata archive"));
		TarUtils.packToTar(properties.getProperty("SDFS_BACKUP_UPLOAD_FILE"), sdfsConfigPathes.getPathes());
		LOG.info(format("\nUploading SDFS configuration & metadata archiveto S3"));
		S3Utils.upload(s3client, properties.getProperty("BUCKET_NAME"),
				properties.getProperty("SDFS_BACKUP_UPLOAD_FILE"), properties.getProperty("KEY_NAME"));
	}
	
	public void downloadAndRestoreSdfs(AmazonS3 s3client) {
		LOG.info(format("\nDownloading SDFS configuration & metadata from S3"));
		S3Utils.download(s3client, properties.getProperty("BUCKET_NAME"),
				properties.getProperty("SDFS_BACKUP_DOWNLOAD_FILE"), properties.getProperty("KEY_NAME"));
		LOG.info(format("\nRestoring SDFS configuration & metadata from archive"));
		TarUtils.unpackFromTar(properties.getProperty("SDFS_BACKUP_DOWNLOAD_FILE"));
	}

	public SdfsState getState() {
		return state;
	}


	public boolean alreadyRunning() {
		boolean running = false;
		LOG.info(format("Check SDFS state for:%s", sdfs));
		if (mountsdfsProcess != null || state != SdfsState.Unmounted)
			running= true;

		ProcessBuilder processbuilder = new ProcessBuilder("mount");
		try {
			Process describeMountProcess = processbuilder.start();
			InputStream mntIs = describeMountProcess.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(mntIs));
			String s; String matcher = "/"+sdfs+" ";
			while((s = reader.readLine())!=null) {
				if (s.indexOf(matcher)>=0) {
					running= true;
				}
			}
			
		} catch (IOException e) {e.printStackTrace();}
		LOG.info(format("SDFS state for:%s is %s", sdfs, running?"already running":"stopped"));
		return running;
	}


	public void mountsdfs() throws IOException {
		if (alreadyRunning())
			throw new IllegalSdfsStateException("Expected state is " + SdfsState.Unmounted + ", but "
					+ state.toString());

		LOG.info(format("Trying to mount '%s'", sdfs));
		ProcessBuilder processbuilder = new ProcessBuilder("mount.sdfs", sdfs, mountPoint, "&");
		mountsdfsProcess = processbuilder.start();
		state = SdfsState.Mounting;

		InputStream sdfsIn = mountsdfsProcess.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(sdfsIn));

		InputStream error = mountsdfsProcess.getErrorStream();

		while (state.equals(SdfsState.Mounting)) {
			String sdfsData = reader.readLine();
			// System.out.println(sdfsData);
			if (error.available() > 0)
				state = SdfsState.Error;
			if (sdfsData.equals("Mounted Filesystem"))
				state = SdfsState.Mounted;
		}
		
		LOG.info(format("Sdfs '%s' started with state %s", sdfs, state.toString()));
		

	}


	public void umountsdfs() throws IOException {
		if (mountsdfsProcess == null)
			return;
		if (umountsdfsProcess != null)
			return;
		if (state != SdfsState.Mounted)
			throw new IllegalSdfsStateException("Expected state is " + SdfsState.Mounted.toString() + ", but " + state.toString());
		LOG.info(format("Trying to unmount '%s'", sdfs));
		InputStream sdfsIn = mountsdfsProcess.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(sdfsIn));
		InputStream error = mountsdfsProcess.getErrorStream();

		ProcessBuilder processbuilder = new ProcessBuilder("umount", mountPoint);
		umountsdfsProcess = processbuilder.start();
		state = SdfsState.Unmounting;
		while (state == SdfsState.Unmounting) {
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!isAlive(umountsdfsProcess)) {
				if (umountsdfsProcess != null) {
					LOG.info("Unmouning process ended.");
					umountsdfsProcess = null;
				}

				if (mountsdfsProcess != null) {
					LOG.info(format("Sdfs '%s' finished", sdfs));
					mountsdfsProcess = null;
				}
			}
			state = SdfsState.Unmounted;
			LOG.info("SdfsProcess in 'Unmounted' state");
		}
	}


	public void backupVolumeToSdfs(String volume, String backupFileName, boolean checkSum) {
		try {
			LOG.info(format("Try to create backup from '%s' with name '%s'", volume, backupFileName));
			if(checkSum) {
				LOG.info(format("Calculating MD5sum for source %s", volume));
				byte[] md5 = Md5Utils.computeMD5Hash(new File(volume));
				String md5Base64 = BinaryUtils.toBase64(md5);
				LOG.info(format("MD5: %s", md5Base64));
			}
			Process binaryCopy = binaryCopy(volume, backupFileName);
			while (isAlive(binaryCopy)) {
				try {
					System.out.print("*");
					TimeUnit.SECONDS.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			LOG.info(format("Backup '%s' created", backupFileName));
			
			if(checkSum) {
				LOG.info(format("Calculating MD5sum for destination %s", backupFileName));
				byte[] md5 = Md5Utils.computeMD5Hash(new File(backupFileName));
				String md5Base64 = BinaryUtils.toBase64(md5);
				LOG.info(format("MD5: %s", md5Base64));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public boolean isAlive(Process process) {
		try {
			process.exitValue();
			return false;
		} catch (IllegalThreadStateException e) {
			return true;
		}
	}


	public void restoreVolumeFromSdfs(String volume, String backupFileName) {

	}


	private Process binaryCopy(String source, String destination) throws IOException {
		ProcessBuilder processbuilder = new ProcessBuilder("dd", "if=" + source, "of=" + mountPoint + "/" + destination);

		Process p = processbuilder.start();

		return p;
	}

	enum SdfsState {
		Mounting("mounting"), Mounted("mounted"), Unmounting("unmounting"), Unmounted("unmounted"), Error("error");

		private String value;


		SdfsState(String value) {
			this.value = value;
		}


		public String toString() {
			return value;
		}


		public static SdfsState fromValue(String value) {
			if (value == null || "".equals(value)) {
				throw new IllegalArgumentException("Value cannot be null or empty!");

			} else if ("mounting".equals(value)) {
				return SdfsState.Mounting;
			} else if ("mounted".equals(value)) {
				return SdfsState.Mounted;
			} else if ("nmounting".equals(value)) {
				return SdfsState.Unmounting;
			} else if ("unmounted".equals(value)) {
				return SdfsState.Unmounted;
			} else if ("error".equals(value)) {
				return SdfsState.Error;
			} else {
				throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
			}
		}

	}
}

/*
 * sudo dd if=/dev/sdb1 of=/mnt/pool0/vdi0.backup - backup sudo dd of=/dev/sdb1
 * if=/mnt/pool0/vdi0.backup - restore
 */
