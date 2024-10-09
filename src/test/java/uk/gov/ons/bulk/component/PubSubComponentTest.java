package uk.gov.ons.bulk.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;

import uk.gov.ons.bulk.entities.DownloadCompleteMessage;
import uk.gov.ons.bulk.entities.IdsErrorMessage;
import uk.gov.ons.bulk.entities.NewIdsJobMessage;

import uk.gov.ons.bulk.util.BulkProperties;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles({"test", "test-integration"})
class PubSubComponentTest {

	@Autowired
	private PubSubTemplate template;

	@Test
	public void testPubSubProcessingFinished() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();
		DownloadCompleteMessage expectedMsg = objectMapper.readValue(new File("src/test/resources/message-remove-table.json"),
				DownloadCompleteMessage.class);

		template.publish("table-pulled-test", Files.readString(Path.of("src/test/resources/message-remove-table.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("aims-remove-table-test", 1, false);
		DownloadCompleteMessage actualMessage = objectMapper.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), DownloadCompleteMessage.class);

		assertEquals(expectedMsg, actualMessage);
	}
	
	@Test
	public void testPubSubNewIdsJob() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();
		NewIdsJobMessage expectedMsg = objectMapper.readValue(new File("src/test/resources/message-new-ids-job.json"),
				NewIdsJobMessage.class);

		template.publish("table-available-test", Files.readString(Path.of("src/test/resources/message-new-ids-job.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("ids-table-available-test", 1, false);
		NewIdsJobMessage actualMessage = objectMapper.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), NewIdsJobMessage.class);

		assertEquals(expectedMsg, actualMessage);
	}

	@Test
	public void testPubSubNewIdsJobWithDefaults() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();
		NewIdsJobMessage expectedMsg = objectMapper.readValue(new File("src/test/resources/message-new-ids-job.json"),
				NewIdsJobMessage.class);

		template.publish("table-available-test", Files.readString(Path.of("src/test/resources/message-new-ids-default-job.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("ids-table-available-test", 1, false);
		NewIdsJobMessage actualMessage = objectMapper.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), NewIdsJobMessage.class);

		assertEquals(expectedMsg, actualMessage);
	}
	
	@Test
	public void testPubSubErrorMessage() throws Exception {
		
		ObjectMapper objectMapper = new ObjectMapper();
		IdsErrorMessage expectedMsg = objectMapper.readValue(new File("src/test/resources/message-error.json"),
				IdsErrorMessage.class);

		template.publish("aims-ids-error", Files.readString(Path.of("src/test/resources/message-error.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("aims-errors", 1, false);
		IdsErrorMessage actualMessage = objectMapper.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), IdsErrorMessage.class);

		assertEquals(expectedMsg, actualMessage);
	}
}
