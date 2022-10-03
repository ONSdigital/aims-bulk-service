package uk.gov.ons.bulk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.repository.BulkStatusRepository;
import uk.gov.ons.bulk.util.BulkServiceConstants.Status;

import java.util.List;

@Service
public class BulkStatusService {

	@Autowired
	private BulkStatusRepository bulkStatusRepository;
	
	public Long saveJob(BulkInfo job) {
		return bulkStatusRepository.saveJob(job);
	}
	
	public List<BulkInfo> queryJob(long jobId) {
		return bulkStatusRepository.queryJob(jobId);
	}

	public List<BulkInfo> getJobs(String userid, String status) {
		return bulkStatusRepository.getJobs(userid, status);
	}
	
	public Long saveIdsJob(IdsBulkInfo job) {
		return bulkStatusRepository.saveIdsJob(job);
	}
	
	public List<IdsBulkInfo> queryIdsJob(long jobId) {
		return bulkStatusRepository.queryIdsJob(jobId);
	}

	public List<IdsBulkInfo> getIdsJobs(String userid, String status) {
		return bulkStatusRepository.getIdsJobs(userid, status);
	}
	
	public List<IdsBulkInfo> getIdsJob(String idsJobId) {
		return bulkStatusRepository.getIdsJob(idsJobId);
	}
	
	public void updateStatus(long jobId, Status status) {
		bulkStatusRepository.updateStatus(jobId, status);
	}
}