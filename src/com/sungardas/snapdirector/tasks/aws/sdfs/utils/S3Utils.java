package com.sungardas.snapdirector.tasks.aws.sdfs.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


public class S3Utils {

	public static final Log LOG = LogFactory.getLog(S3Utils.class);


	public static void upload(AmazonS3 s3client, String bucketName, String uploadFileName, String keyName) {
		try {
			LOG.info("Uploading a new object to S3 bucket: " + bucketName);
			LOG.info("Upload file name: " + uploadFileName);
			LOG.info("Key name: " + keyName);
			long timestamp = System.currentTimeMillis();

			File file = new File(uploadFileName);
			s3client.putObject(new PutObjectRequest(bucketName, keyName, file));

			LOG.info("Uploading time: " + (System.currentTimeMillis() - timestamp) + "ms");

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which " + "means your request made it "
					+ "to Amazon S3, but was rejected with an error response" + " for some reason.");
			LOG.error("Error Message:    " + ase.getMessage());
			LOG.error("HTTP Status Code: " + ase.getStatusCode());
			LOG.error("AWS Error Code:   " + ase.getErrorCode());
			LOG.error("Error Type:       " + ase.getErrorType());
			LOG.error("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which " + "means the client encountered "
					+ "an internal error while trying to " + "communicate with S3, "
					+ "such as not being able to access the network.");
			LOG.error("Error Message: " + ace.getMessage());
		}
	}


	public static void download(AmazonS3 s3client, String bucketName, String localPath, String keyName) {

		try {
			LOG.info("Downloading an object from S3 bucket:" + bucketName);
			LOG.info("Local path: " + localPath);
			LOG.info("Key name: " + keyName);
			long timestamp = System.currentTimeMillis();
			S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, keyName));

			File f = new File(localPath);
			BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));

			int count;
			byte[] buffer = new byte[2048];
			S3ObjectInputStream s3in = s3object.getObjectContent();
			while ((count = s3in.read(buffer)) != -1) {
				bout.write(buffer, 0, count);
			}
			bout.flush();
			bout.close();

			LOG.info("Download time: " + (System.currentTimeMillis() - timestamp) + "ms");

		} catch (AmazonServiceException ase) {
			LOG.error("Error Message:    " + ase.getMessage());
			LOG.error("HTTP Status Code: " + ase.getStatusCode());
			LOG.error("AWS Error Code:   " + ase.getErrorCode());
			LOG.error("Error Type:       " + ase.getErrorType());
			LOG.error("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			LOG.error("Error Message: " + ace.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static Snapshot createSnapshot(AmazonEC2 ec2client, Volume volume) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd'_T'hh:mm:ss");

		String volumeId = volume.getVolumeId();
		LOG.info(format("Starting creating snapshot for %s", volumeId));
		CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest(volumeId, volumeId + "__"
				+ formatter.format(new Date(System.currentTimeMillis())));
		CreateSnapshotResult crSnapshotResult = ec2client.createSnapshot(snapshotRequest);
		Snapshot snapshot = crSnapshotResult.getSnapshot();
		return snapshot;
	}


	public static void deleteSnapshot(AmazonEC2 ec2client, Volume associatedVolume) {
		deleteSnapshot(ec2client, associatedVolume.getSnapshotId());
	}


	public static void deleteSnapshot(AmazonEC2 ec2client, String snapshotId) {
		LOG.info(format("Deleting snapshot: %s", snapshotId));
		DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest();
		deleteSnapshotRequest.setSnapshotId(snapshotId);
		ec2client.deleteSnapshot(deleteSnapshotRequest);
	}


	public static Snapshot waitForCompleteState(AmazonEC2 ec2client, Snapshot snapshot) {
		String state;
		Snapshot result;
		do {
			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			result = syncSnapshot(ec2client, snapshot);
			state = result.getState();
			if (state.equals(SnapshotState.Error)) {
				// TODO:exception
			}
		} while (state.equals(SnapshotState.Pending));

		return result;
	}


	public static Snapshot syncSnapshot(AmazonEC2 ec2client, Snapshot snapshot) {
		DescribeSnapshotsRequest describeSnapshotsRequest = new DescribeSnapshotsRequest();
		LinkedList<String> ids = new LinkedList<String>();
		ids.add(snapshot.getSnapshotId());
		describeSnapshotsRequest.setSnapshotIds(ids);
		DescribeSnapshotsResult describeSnapshotsResult = ec2client.describeSnapshots(describeSnapshotsRequest);
		return describeSnapshotsResult.getSnapshots().get(0);
	}


	public static Volume waitForAvailableState(AmazonEC2 ec2client, Volume volume) {
		String state;
		Volume result;
		do {
			try {
				TimeUnit.SECONDS.sleep(25);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			result = syncVolume(ec2client, volume);
			state = result.getState();
			System.out.println("waitForAvailableState.current state: " + state);
			if (state.equals(VolumeState.Error.toString())) {
				throw new RuntimeException("error...");
			}
		} while (!state.equals(VolumeState.Available.toString()) && !state.equals(VolumeState.Deleted.toString()));
		return result;
	}


	public static Volume syncVolume(AmazonEC2 ec2client, Volume volume) {
		DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
		LinkedList<String> ids = new LinkedList<String>();
		ids.add(volume.getVolumeId());
		describeVolumesRequest.setVolumeIds(ids);
		DescribeVolumesResult describeVolumesResult = ec2client.describeVolumes(describeVolumesRequest);
		return describeVolumesResult.getVolumes().get(0);
	}


	// public static Volume createVolumeFromSnapshot(AmazonEC2 ec2client,
	// Snapshot sourceSnapshot) {
	// DescribeAvailabilityZonesResult zonesResult =
	// ec2client.describeAvailabilityZones();
	// List<AvailabilityZone> zones = zonesResult.getAvailabilityZones();
	// Volume vol = null;
	// if (zones.size() > 0) {
	// LOG.info(format("Starting creating volume from %s",
	// sourceSnapshot.getSnapshotId()));
	//
	// CreateVolumeRequest crVolumeRequest = new
	// CreateVolumeRequest(sourceSnapshot.getSnapshotId(), zones.get(1)
	// .getZoneName());
	// CreateVolumeResult crVolumeResult =
	// ec2client.createVolume(crVolumeRequest);
	// vol = crVolumeResult.getVolume();
	// }
	// return vol;
	// }

	public static Volume createVolumeFromSnapshot(AmazonEC2 ec2client, Snapshot sourceSnapshot,
			String availabilityZoneName) {
		DescribeAvailabilityZonesResult zonesResult = ec2client.describeAvailabilityZones();
		List<AvailabilityZone> zones = zonesResult.getAvailabilityZones();
		Volume vol = null;
		if (zones.size() > 0) {
			LOG.info(format("Starting creating volume from %s", sourceSnapshot.getSnapshotId()));

			boolean incorrectState = true;
			long timeout = 10L;
			while (incorrectState) {
				try {
					incorrectState = false;
					CreateVolumeRequest crVolumeRequest = new CreateVolumeRequest(sourceSnapshot.getSnapshotId(),
							availabilityZoneName);
					CreateVolumeResult crVolumeResult = ec2client.createVolume(crVolumeRequest);
					vol = crVolumeResult.getVolume();
				} catch (AmazonServiceException incorrectStateException) {
					LOG.info(incorrectStateException.getMessage() + "\n Waiting for new try");
					incorrectState = true;
					timeout += timeout < 120 ? timeout * 2 : 0;
					try {
						TimeUnit.SECONDS.sleep(timeout);
					} catch (InterruptedException e) {
					}
				}
			}

		}
		return vol;
	}


	public static void deleteVolume(AmazonEC2 ec2client, Volume volume) {

		boolean incorrectState = true;
		long timeout = 10L;
		while (incorrectState) {
			try {
				incorrectState = false;
				DeleteVolumeRequest deleteVolumeRequest = new DeleteVolumeRequest(volume.getVolumeId());
				ec2client.deleteVolume(deleteVolumeRequest);
			} catch (AmazonServiceException incorrectStateException) {
				LOG.info(incorrectStateException.getMessage() + "\n Waiting for new try");
				incorrectState = true;
				timeout += timeout < 120 ? timeout * 2 : 0;
				try {
					TimeUnit.SECONDS.sleep(timeout);
				} catch (InterruptedException e) {
				}
			}
		}
		LOG.info(format("Volume %s deleted", volume.getVolumeId()));
	}


	public static void attachVolume(AmazonEC2 ec2client, Instance instance, Volume volume) {
		String deviceName = getNextAvaiableDeviceName(instance);
		boolean incorrectState = true;
		long timeout = 10L;
		while (incorrectState) {
			try {
				incorrectState = false;
		AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest(volume.getVolumeId(),
				instance.getInstanceId(), deviceName);
		AttachVolumeResult res = ec2client.attachVolume(attachVolumeRequest);
			} catch (AmazonServiceException incorrectStateException) {
				LOG.info(incorrectStateException.getMessage() + "\n Waiting for new try");
				incorrectState = true;
				timeout += timeout < 120 ? timeout * 2 : 0;
				try {
					TimeUnit.SECONDS.sleep(timeout);
				} catch (InterruptedException e) {
				}
			}
		}
		LOG.info(format("\nVolume attached. check instance data\n %s", instance.toString()));

	}


	public static void unattachVolume(AmazonEC2 ec2client, Volume volume) {
		boolean incorrectState = true;
		long timeout = 10L;
		while (incorrectState) {
			try {
				incorrectState = false;
		DetachVolumeRequest detachVolumeRequest = new DetachVolumeRequest(volume.getVolumeId());
		DetachVolumeResult detachVolumeResult = ec2client.detachVolume(detachVolumeRequest);
			} catch (AmazonServiceException incorrectStateException) {
				LOG.info(incorrectStateException.getMessage() + "\n Waiting for new try");
				incorrectState = true;
				timeout += timeout < 120 ? timeout * 2 : 0;
				try {
					TimeUnit.SECONDS.sleep(timeout);
				} catch (InterruptedException e) {
				}
			}
		}
		LOG.info(format("\nVolume %s unattached", volume.getVolumeId()));
	}


	public static VolumeAttachment detachVolumeFromInstance(AmazonEC2 ec2client, Volume volume) {
		DetachVolumeRequest detachVolumeRequest = new DetachVolumeRequest(volume.getVolumeId());
		DetachVolumeResult detachVolumeResult = ec2client.detachVolume(detachVolumeRequest);
		return detachVolumeResult.getAttachment();
	}


	public static List<Volume> getVolumeList(AmazonEC2 ec2client) {
		DescribeVolumesResult volumeResult = ec2client.describeVolumes();
		return volumeResult.getVolumes();
	}


	public static List<Instance> getInstanceList(AmazonEC2 ec2client) {
		DescribeInstancesResult descInstancesResult = ec2client.describeInstances();
		List<Reservation> reservations = descInstancesResult.getReservations();
		List<Instance> instances = new LinkedList<Instance>();
		for (Reservation res : reservations) {
			instances.addAll(res.getInstances());
		}

		return instances;
	}


	private static String getNextAvaiableDeviceName(Instance instance) {
		String devName = "";

		List<InstanceBlockDeviceMapping> devList = instance.getBlockDeviceMappings();
		for (InstanceBlockDeviceMapping map : devList) {
			String tmp = map.getDeviceName();
			if (tmp.compareToIgnoreCase(devName) > 0) {
				devName = tmp;
			}
		}

		if (devName.length() > 0) {
			char ch = devName.charAt(devName.length() - 1);
			if (ch < 'f') {
				ch = 'f' - 1;
			}
			if (ch < 'p') {
				ch += 1;
				return "/dev/sd" + (char) ch;
			}
		}
		return "/dev/sdf";
	}


	private static List<Snapshot> getSnapshotsList(AmazonEC2 ec2client) {
		DescribeSnapshotsResult snapshotsResult = ec2client.describeSnapshots();
		return snapshotsResult.getSnapshots();
	}


	private static Set<Instance> getInstanceList(AmazonEC2 ec2client, Region region) {
		DescribeInstancesResult describeInstancesResult = ec2client.describeInstances();
		Set<Instance> instances = new HashSet<Instance>();
		for (Reservation reservation : describeInstancesResult.getReservations()) {
			instances.addAll(reservation.getInstances());
		}

		System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
		return instances;
	}

}
