package uk.gov.ons.bulk.repository;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.bulk.entities.BulkInfo;

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
}
