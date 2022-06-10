package uk.gov.ons.bulk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.repository.BulkStatusRepository;

@Service
public class BulkStatusService {

	@Autowired
	private BulkStatusRepository bulkStatusRepository;
	
	public Long saveJob(BulkInfo job) {
		return bulkStatusRepository.saveJob(job);
	}
	
	public BulkInfo queryJob(long jobId) {
		return bulkStatusRepository.queryJob(jobId);
	}
}