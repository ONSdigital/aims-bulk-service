package uk.gov.ons.bulk.component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.DownloadCompleteMessage;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.entities.IdsErrorMessage;
import uk.gov.ons.bulk.entities.IdsErrors;
import uk.gov.ons.bulk.entities.NewIdsJobMessage;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.service.BulkStatusService;
import uk.gov.ons.bulk.service.IdsService;

@Slf4j
@Component
@Profile("!test")
public class PubSubComponent {
	
	@Value("${spring.cloud.gcp.project-id}")
	private String projectId;

	@Value("${ids.pubsub.subscription-new-ids-job}")
	private String pubsubSubscriptionNewIdsJob;
	
	@Value("${ids.pubsub.subscription-download-complete}")
	private String pubsubSubscriptionDownloadComplete;
	
	@Value("${aims.pubsub.error-topic}")
	private String pubsubErrorTopic;
	
	@Autowired
	private IdsService idsService;
	
	@Autowired
	private BulkStatusService bulkStatusService;
	
	@Autowired
	private PubsubOutboundGateway messagingGateway;
		
	@Bean
	public MessageChannel pubsubInputChannelNewIdsJob() {
		return new DirectChannel();
	}
	
	@Bean
	public MessageChannel pubsubInputChannelDownloadComplete() {
		return new DirectChannel();
	}
	
	@Bean
	public PubSubInboundChannelAdapter messageNewIdsJobChannelAdapter(
			@Qualifier("pubsubInputChannelNewIdsJob") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", projectId, pubsubSubscriptionNewIdsJob));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}

	@Bean
	public PubSubInboundChannelAdapter messageDownloadCompleteChannelAdapter(
			@Qualifier("pubsubInputChannelDownloadComplete") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", projectId, pubsubSubscriptionDownloadComplete));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}
	
	@Bean
	@ServiceActivator(inputChannel = "pubsubInputChannelNewIdsJob")
	public MessageHandler messageNewIdsJobReceiver() {
		return message -> {
			log.debug("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
					
			try {
				NewIdsJobMessage msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
						.readValue((byte[]) message.getPayload(), NewIdsJobMessage.class);
				log.debug(String.format("Message: %s", msg.toString()));

				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
				Validator validator = factory.getValidator();
				
				List<String> validationErrorMessages = new ArrayList<>();
				
				validator.validate(msg).forEach(violation -> {
					log.info(violation.getMessage());
					validationErrorMessages.add(violation.getMessage());
				});
				
				// Does the idsjobId already exist?
				List<IdsBulkInfo> idsBulkInfos = bulkStatusService.getIdsJob(msg.getPayload().getIdsJobId());
				if (idsBulkInfos.size() > 0) {
					String errorMessage = String.format("A job with the id %s already exists. ids_job_id must be unique.", msg.getPayload().getIdsJobId());
					log.info(errorMessage);
					validationErrorMessages.add(errorMessage);
				}

				if (validationErrorMessages.isEmpty()) {
					// Read the BigQuery table in IDS and start creating Cloud Tasks
					idsService.createTasks(msg);
					
				} else {
					// One or more problems found so send message to the PubSub topic
					messagingGateway.sendToPubsub(new ObjectMapper().writeValueAsString(new IdsErrorMessage((new IdsErrors(msg.getPayload().getIdsJobId(), 
							LocalDateTime.now().toString(), validationErrorMessages)))));
				}	
				
				// Send ACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.ack();	
				
			} catch (IOException ioe) {
				log.error(String.format("Unable to read message: %s", ioe));

				// Send NACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.nack();	
			}
		};
	}
	
	@Bean
	@ServiceActivator(inputChannel = "pubsubInputChannelDownloadComplete")
	public MessageHandler messageDownloadCompleteReceiver() {
		return message -> {
			log.debug("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
			
			String idsJobId = "";
			
			try {
				DownloadCompleteMessage msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
						.readValue((byte[]) message.getPayload(), DownloadCompleteMessage.class);
				log.debug(String.format("Message: %s", msg.toString()));
				
				idsJobId = msg.getPayload().getIdsJobId();
				
				// Delete the Big Query Table associated with this IDS job
				idsService.deleteIdsResultTable(msg.getPayload());
				
				// Send ACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.ack();	
							
			} catch (IOException ioe) {
				log.error(String.format("Unable to read message: %s", ioe));

				// Send NACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.nack();	
			} catch (BulkAddressException e) {
				
				String errorMessage = String.format("Problem deleting IDS result table: %s", e);
				log.error(errorMessage);

				try {
					messagingGateway.sendToPubsub(new ObjectMapper().writeValueAsString(new IdsErrorMessage(new IdsErrors(idsJobId, 
							LocalDateTime.now().toString(), List.of(errorMessage)))));
				} catch (JsonProcessingException jpe) {
					log.error(String.format("Problem creating JSON: %s", jpe));
				}
				
				// Send ACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.ack();	
			}
		};
	}
	
	@Bean
	@ServiceActivator(inputChannel = "pubsubOutputChannel")
	public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
		return new PubSubMessageHandler(pubsubTemplate, pubsubErrorTopic);
	}
	
	@MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
	public interface PubsubOutboundGateway {

		void sendToPubsub(String text);
	}
}
