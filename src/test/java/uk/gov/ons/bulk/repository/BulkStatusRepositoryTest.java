package uk.gov.ons.bulk.repository;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.util.BulkServiceConstants.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Suite
@SuiteDisplayName("Repository Test Suite")
@AutoConfigureTestDatabase
class BulkStatusRepositoryTest {
	
    @Autowired
    private BulkStatusRepository bulkStatusRepository;
    
    private final LocalDateTime ldt1 = LocalDateTime.parse("2022-07-04T16:36:58.944848");
    private final LocalDateTime ldt2 = LocalDateTime.parse("2022-07-05T12:42:58.944872");
    
	@Test
	public void testSaveJob() {
		
		BulkInfo bulkInfo = new BulkInfo("fred","in-progress", 107, 0, "Dataset 1", "Topic 1", "{'header_export': 'true'}");
        bulkInfo.setStartdate(ldt1);
		Long result = bulkStatusRepository.saveJob(bulkInfo);
		
		assertThat(result == 1L);
	}
	
	@Test
	public void testSaveIdsJob() {
		
		IdsBulkInfo idsBulkInfo = new IdsBulkInfo("ids-job-xx", "ids-user-xx", "in-progress", 107, 0, false);
		idsBulkInfo.setStartdate(ldt1);
		Long result = bulkStatusRepository.saveJob(idsBulkInfo);
		
		assertThat(result == 1L);
	}
    
	@Test
	public void testQueryJob() {

		BulkInfo result = bulkStatusRepository.queryJob(1).getFirst();
		assertThat(result.getJobid()).isEqualTo(1);
		assertThat(result.getUserid()).isEqualTo("bob");
		assertThat(result.getStatus()).isEqualTo("in-progress");
		assertThat(result.getTotalrecs()).isEqualTo(107);
		assertThat(result.getRecssofar()).isEqualTo(45);
		assertThat(result.getStartdate()).isEqualTo(ldt1.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}
	
	@Test
	public void testQueryIdsJob() {

		IdsBulkInfo result = bulkStatusRepository.queryIdsJob(1).getFirst();
		assertThat(result.getJobid()).isEqualTo(1);
		assertThat(result.getIdsJobId()).isEqualTo("ids-job-1");
		assertThat(result.getUserid()).isEqualTo("ids-user-x");
		assertThat(result.getStatus()).isEqualTo("in-progress");
		assertThat(result.getTotalrecs()).isEqualTo(107);
		assertThat(result.getRecssofar()).isEqualTo(45);
		assertThat(result.getStartdate()).isEqualTo(ldt1.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}

	@Test
	public void testgetJobs() {

		List<BulkInfo> bulkInfos = bulkStatusRepository.getJobs("mrrobot","in-progress");
		
		assertEquals(2, bulkInfos.size());
		
		bulkInfos.forEach(bulkInfo -> {	
			assertEquals("mrrobot", bulkInfo.getUserid());
			assertEquals("in-progress", bulkInfo.getStatus());
		});
				
		assertTrue(bulkInfos.stream().map(BulkInfo::getTotalrecs).toList().containsAll(List.of(348076L, 107L)));
		assertTrue(bulkInfos.stream().map(BulkInfo::getRecssofar).toList().containsAll(List.of(2000L, 45L)));
	}
	
	@Test
	public void testgetIdsJobs() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJobs("ids-user-y","in-progress");
		
		assertEquals(2, idsBulkInfos.size());
		
		idsBulkInfos.forEach(bulkInfo -> {	
			assertEquals("ids-user-y", bulkInfo.getUserid());
			assertEquals("in-progress", bulkInfo.getStatus());
		});
				
		assertTrue(idsBulkInfos.stream().map(IdsBulkInfo::getIdsJobId).toList().containsAll(List.of("ids-job-2", "ids-job-3")));
		assertTrue(idsBulkInfos.stream().map(BulkInfo::getTotalrecs).toList().containsAll(List.of(348076L, 107L)));
		assertTrue(idsBulkInfos.stream().map(BulkInfo::getRecssofar).toList().containsAll(List.of(2000L, 45L)));
	}
	
	@Test
	public void testgetIdsJob() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJob("ids-job-2");
		
		assertEquals(1, idsBulkInfos.size());
		assertEquals("ids-job-2", idsBulkInfos.getFirst().getIdsJobId());
		assertEquals("ids-user-y", idsBulkInfos.getFirst().getUserid());
		assertEquals("in-progress", idsBulkInfos.getFirst().getStatus());
		assertEquals(2000L, idsBulkInfos.getFirst().getRecssofar());
		assertEquals(348076L, idsBulkInfos.getFirst().getTotalrecs());
	}
	
	@Test
	public void testgetNonExistentIdsJob() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJob("ids-job-99");
		assertEquals(0, idsBulkInfos.size());
	}
	
	@Test
	public void testUpdateStatus() {
		
		bulkStatusRepository.updateStatus(2L, Status.RD);
				
		IdsBulkInfo result = bulkStatusRepository.queryIdsJob(2L).getFirst();
		assertThat(result.getJobid()).isEqualTo(2L);
		assertThat(result.getIdsJobId()).isEqualTo("ids-job-2");
		assertThat(result.getUserid()).isEqualTo("ids-user-y");
		assertThat(result.getStatus()).isEqualTo("results-deleted");
		assertThat(result.getTotalrecs()).isEqualTo(348076);
		assertThat(result.getRecssofar()).isEqualTo(2000);
		assertThat(result.getStartdate()).isEqualTo(ldt2.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}
}
