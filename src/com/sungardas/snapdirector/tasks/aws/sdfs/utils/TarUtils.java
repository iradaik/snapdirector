package com.sungardas.snapdirector.tasks.aws.sdfs.utils;

import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarOutputStream;

import java.io.*;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.String.format;


public class TarUtils {

	public static final Log LOG = LogFactory.getLog(SdfsProcess.class);


	public static void packToTar(String archiveName, Collection<String> pathes) {
		try {
			LOG.info(format("\nCreating archive: %s", archiveName));
			FileOutputStream archiveDestination = new FileOutputStream(archiveName);
			TarOutputStream tarOut = new TarOutputStream(new BufferedOutputStream(new GZIPOutputStream(
					archiveDestination)));

			File f;
			for (String filepath : pathes) {
				LOG.info(format("Storeing from location: %s", filepath));
				f = new File(filepath);
				if (!f.exists())
					continue;
				if (f.isDirectory()) {
					pachFolderToTar(tarOut, f);
					continue;
				}
				putFileToTar(tarOut, f);
			}

			tarOut.close();

			if (LOG.isInfoEnabled()) {
				byte[] md5 = Md5Utils.computeMD5Hash(new File(archiveName));
				String md5Base64 = BinaryUtils.toBase64(md5);

				LOG.info(format("Archive created. MD5: %s", md5Base64));
			}

		} catch (FileNotFoundException e) {
			LOG.error("Cant create archive file: " + archiveName);
			LOG.error("Error Message:    " + e.getMessage());
			throw new RuntimeException(e);
		} catch (IOException e) {
			// close exception
			e.printStackTrace();
		}
	}


	private static final void pachFolderToTar(TarOutputStream tarOut, File folder) throws IOException {
		File[] files = folder.listFiles();

		for (File f : files) {
			if (f.isDirectory()) {
				pachFolderToTar(tarOut, f);
				continue;
			}
			putFileToTar(tarOut, f);
		}

	}


	private static void putFileToTar(TarOutputStream tarOut, File fileToPut) throws IOException {
		tarOut.putNextEntry(new TarEntry(fileToPut, fileToPut.getAbsolutePath()));

		BufferedInputStream origin = new BufferedInputStream(new FileInputStream(fileToPut));
		int count;
		byte buffer[] = new byte[2048];
		while ((count = origin.read(buffer)) != -1) {
			tarOut.write(buffer, 0, count);
		}
		tarOut.flush();
		origin.close();
	}


	public static void unpackFromTar(String archiveName) {
		try {
			LOG.info(format("\nExtracting archive: %s", archiveName));
			if (LOG.isInfoEnabled()) {
				byte[] md5 = Md5Utils.computeMD5Hash(new File(archiveName));
				String md5Base64 = BinaryUtils.toBase64(md5);

				LOG.info(format("Archive  MD5: %s", md5Base64));
			}
			FileInputStream archiveSource = new FileInputStream(archiveName);
			TarInputStream tarIn = new TarInputStream(new BufferedInputStream(new GZIPInputStream(archiveSource)));

			TarEntry entry;

			while ((entry = tarIn.getNextEntry()) != null) {
				int count;
				byte buffer[] = new byte[2048];
				String absolutePath = entry.getName();
				createDirs(absolutePath);

				BufferedOutputStream destination = new BufferedOutputStream(new FileOutputStream(absolutePath));
				while ((count = tarIn.read(buffer)) != -1) {
					destination.write(buffer, 0, count);
				}
				destination.flush();
				destination.close();
			}

			archiveSource.close();
			LOG.info("Archive extracted: " + archiveName);
		} catch (FileNotFoundException e) {
			LOG.error("Cant get access to archive file: " + archiveName);
			LOG.error("Error Message:    " + e.getMessage());
			throw new RuntimeException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private static void createDirs(String absolutePath) {
		int fileNamePos = absolutePath.lastIndexOf('\\') != -1 ? absolutePath.lastIndexOf('\\') : absolutePath
				.lastIndexOf('/');
		File pathToCreate = new File(absolutePath.substring(0, fileNamePos));
		if (pathToCreate.mkdirs()) {
			LOG.info(format("Created folder: %s", pathToCreate));
		}
	}

}
