package uk.gov.ons.bulk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.IdsBulkInfo;

@SpringBootTest
@ActiveProfiles("test")
class BulkStatusRepositoryTest {
	
    @Autowired
    private BulkStatusRepository bulkStatusRepository;
    
    private LocalDateTime ldt = LocalDateTime.parse("2022-07-04T16:36:58.944848");

	@Test
	public void testSaveJob() {
		
		BulkInfo bulkInfo = new BulkInfo("fred", "in-progress", 107, 0);
        bulkInfo.setStartdate(ldt);
		Long result = bulkStatusRepository.saveJob(bulkInfo);
		
		assertThat(result == 1L);
	}
	
	@Test
	public void testSaveIdsJob() {
		
		IdsBulkInfo idsBulkInfo = new IdsBulkInfo("ids-job-xx", "ids-user-xx", "in-progress", 107, 0);
		idsBulkInfo.setStartdate(ldt);
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
		assertThat(result.getStartdate()).isEqualTo(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
		assertThat(result.getStartdate()).isEqualTo(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
}
