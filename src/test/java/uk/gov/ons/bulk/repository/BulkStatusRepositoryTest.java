package uk.gov.ons.bulk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

@SpringBootTest
@ActiveProfiles("test")
@Suite
@SuiteDisplayName("Repository Test Suite")
@AutoConfigureTestDatabase
class BulkStatusRepositoryTest {
	
    @Autowired
    private BulkStatusRepository bulkStatusRepository;
    
    private LocalDateTime ldt1 = LocalDateTime.parse("2022-07-04T16:36:58.944848");
    private LocalDateTime ldt2 = LocalDateTime.parse("2022-07-05T12:42:58.944872");
    
	@Test
	public void testSaveJob() {
		
		BulkInfo bulkInfo = new BulkInfo("fred", "in-progress", 107, 0);
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

		BulkInfo result = bulkStatusRepository.queryJob(1).get(0);
		assertThat(result.getJobid()).isEqualTo(1);
		assertThat(result.getUserid()).isEqualTo("bob");
		assertThat(result.getStatus()).isEqualTo("in-progress");
		assertThat(result.getTotalrecs()).isEqualTo(107);
		assertThat(result.getRecssofar()).isEqualTo(45);
		assertThat(result.getStartdate()).isEqualTo(ldt1.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}
	
	@Test
	public void testQueryIdsJob() {

		IdsBulkInfo result = bulkStatusRepository.queryIdsJob(1).get(0);
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
			assertEquals(bulkInfo.getUserid(), "mrrobot");
			assertEquals(bulkInfo.getStatus(), "in-progress");
		});
				
		assertTrue(bulkInfos.stream().map(bulkInfo -> bulkInfo.getTotalrecs()).collect(Collectors.toList()).containsAll(List.of(348076L, 107L)));
		assertTrue(bulkInfos.stream().map(bulkInfo -> bulkInfo.getRecssofar()).collect(Collectors.toList()).containsAll(List.of(2000L, 45L)));
	}
	
	@Test
	public void testgetIdsJobs() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJobs("ids-user-y","in-progress");
		
		assertEquals(2, idsBulkInfos.size());
		
		idsBulkInfos.forEach(bulkInfo -> {	
			assertEquals(bulkInfo.getUserid(), "ids-user-y");
			assertEquals(bulkInfo.getStatus(), "in-progress");
		});
				
		assertTrue(idsBulkInfos.stream().map(idsBulkInfo -> idsBulkInfo.getIdsJobId()).collect(Collectors.toList()).containsAll(List.of("ids-job-2", "ids-job-3")));
		assertTrue(idsBulkInfos.stream().map(idsBulkInfo -> idsBulkInfo.getTotalrecs()).collect(Collectors.toList()).containsAll(List.of(348076L, 107L)));
		assertTrue(idsBulkInfos.stream().map(idsBulkInfo -> idsBulkInfo.getRecssofar()).collect(Collectors.toList()).containsAll(List.of(2000L, 45L)));
	}
	
	@Test
	public void testgetIdsJob() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJob("ids-job-2");
		
		assertEquals(1, idsBulkInfos.size());
		assertEquals(idsBulkInfos.get(0).getIdsJobId(), "ids-job-2");
		assertEquals(idsBulkInfos.get(0).getUserid(), "ids-user-y");
		assertEquals(idsBulkInfos.get(0).getStatus(), "in-progress");
		assertEquals(idsBulkInfos.get(0).getRecssofar(), 2000L);
		assertEquals(idsBulkInfos.get(0).getTotalrecs(), 348076L);
	}
	
	@Test
	public void testgetNonExistentIdsJob() {

		List<IdsBulkInfo> idsBulkInfos = bulkStatusRepository.getIdsJob("ids-job-99");
		assertEquals(0, idsBulkInfos.size());
	}
	
	@Test
	public void testUpdateStatus() {
		
		bulkStatusRepository.updateStatus(2L, Status.RD);
				
		IdsBulkInfo result = bulkStatusRepository.queryIdsJob(2L).get(0);
		assertThat(result.getJobid()).isEqualTo(2L);
		assertThat(result.getIdsJobId()).isEqualTo("ids-job-2");
		assertThat(result.getUserid()).isEqualTo("ids-user-y");
		assertThat(result.getStatus()).isEqualTo("results-deleted");
		assertThat(result.getTotalrecs()).isEqualTo(348076);
		assertThat(result.getRecssofar()).isEqualTo(2000);
		assertThat(result.getStartdate()).isEqualTo(ldt2.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}
}
