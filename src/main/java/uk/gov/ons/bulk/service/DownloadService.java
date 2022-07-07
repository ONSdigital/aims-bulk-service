package uk.gov.ons.bulk.service;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.gax.paging.Page;
import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DownloadService {

	@Value("${aims.project-number}")
	private String projectNumber;

	@Value("${spring.cloud.gcp.project-id}")
	private String projectName;
	
	public String downloadGCSObject(String jobId, String downloadPath, String filename) throws IOException {

		String gcsResultsBucket = String.format("%s%s_%s", BIG_QUERY_TABLE_PREFIX, jobId, projectNumber);
		Path path = Paths.get(String.format("%s/%s", downloadPath, filename));

		Storage storage = StorageOptions.getDefaultInstance().getService();
		
		BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsResultsBucket, filename)).build();
		
		URL url =
		        storage.signUrl(blobInfo, 15, TimeUnit.MINUTES, 
		        		Storage.SignUrlOption.withV4Signature(), 
		        		Storage.SignUrlOption.signWith((ServiceAccountSigner)storage.getOptions().getCredentials()));

		return url.toString();
		

//		Page<Blob> blobs = storage.list(gcsResultsBucket);
//
//		for (Blob blob : blobs.iterateAll()) {
//
//			if (blob.getName().equals(filename)) {
////				blob.downloadTo(path);
//				URL signedUrl = blob.signUrl(1, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature());
//				
//				
//				log.debug(String.format("Downloading: %s", filename));
//				return signedUrl.toString();
//			} else {
//				return String.format("%s does not exist", filename);
//			}
//		}

//		return String.format("%s ius empty", gcsResultsBucket);
	}
}
