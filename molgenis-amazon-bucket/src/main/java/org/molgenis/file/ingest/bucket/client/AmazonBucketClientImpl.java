package org.molgenis.file.ingest.bucket.client;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.file.FileStore;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Date;
import java.util.TreeMap;

@Component
public class AmazonBucketClientImpl implements AmazonBucketClient
{
	@Override
	public AmazonS3 getClient(String profile)
	{
		return AmazonS3ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider(profile))
				.withRegion(Regions.EU_CENTRAL_1).build();
	}

	@Override
	public File downloadFile(AmazonS3 s3Client, FileStore fileStore, String jobIdentifier, String bucketName,
			String keyName, boolean isExpression) throws IOException, AmazonClientException
	{
		String key;
		//The key can be a regular expression instead of the actual key.
		//This is indicated by the "isExpression" boolean
		if (isExpression)
		{
			key = this.getMostRecentMatchingKey(s3Client, bucketName, keyName);
		}
		else
		{
			key = keyName;
		}
		S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucketName, key));
		InputStream in = s3Object.getObjectContent();

		return storeFile(fileStore, key, jobIdentifier, in);
	}

	private File storeFile(FileStore fileStore, String key, String jobIdentifier, InputStream in) throws IOException
	{
		String identifier = "bucket_" + jobIdentifier;
		File folder = new File(fileStore.getStorageDir(), identifier);
		folder.mkdir();

		key = key.replaceAll("[\\/:*?\"<>|]", "_");
		String filename = identifier + '/' + key + ".xlsx";
		return fileStore.store(in, filename);
	}

	//in case of an key expression all matching keys are collected and the most recent file is downloaded.
	private String getMostRecentMatchingKey(AmazonS3 s3Client, String bucketName, String regex)
	{
		ObjectListing objectListing = s3Client.listObjects(bucketName);
		TreeMap<Date, String> keys = new TreeMap<>();
		for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
		{
			if (objectSummary.getKey().matches(regex))
			{
				keys.put(objectSummary.getLastModified(), objectSummary.getKey());
			}
		}
		if (keys.size() == 0) throw new MolgenisDataException("No key matching regular expression: " + regex);
		return keys.lastEntry().getValue();
	}
}