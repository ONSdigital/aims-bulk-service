package uk.gov.ons.bulk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.repository.BulkStatusRepository;

import java.util.List;

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

	public List<BulkInfo> getJobs(String userid, String status) {
		return bulkStatusRepository.getJobs(userid, status);
	}
}