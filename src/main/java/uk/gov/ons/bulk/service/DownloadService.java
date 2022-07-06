package uk.gov.ons.bulk.service;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DownloadService {
	
	@Value("${aims.project-number}")
	private String projectNumber;
	
	public void downloadGCSObject(String jobId, String downloadPath, String filename) {
				
		CompletableFuture.runAsync(() -> {
			
			String gcsResultsBucket = String.format("%s%s_%s", BIG_QUERY_TABLE_PREFIX, jobId, projectNumber);
			Path path = Paths.get(String.format("%s/%s", downloadPath, filename));

			Storage storage = StorageOptions.getDefaultInstance().getService();
			Page<Blob> blobs = storage.list(gcsResultsBucket);
			
			for (Blob blob : blobs.iterateAll()) {
				
				if (blob.getName().equals(filename)) {
					blob.downloadTo(path);
					log.debug(String.format("Downloading: %s", filename));
				}
			}			
		});
	}
}
