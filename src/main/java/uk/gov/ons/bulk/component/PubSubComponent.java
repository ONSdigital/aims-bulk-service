package uk.gov.ons.bulk.component;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.DownloadCompleteMessage;
import uk.gov.ons.bulk.entities.NewIdsJobMessage;

@Slf4j
@Component
public class PubSubComponent {
	
	@Value("${ids.cloud.gcp.project-id}")
	private String idsGcpProject;
	
	@Value("${ids.pubsub.subscription-new-ids-job}")
	private String pubsubSubscriptionNewIdsJob;
	
	@Value("${ids.pubsub.subscription-download-complete}")
	private String pubsubSubscriptionDownloadComplete;
	
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
				String.format("projects/%s/subscriptions/%s", idsGcpProject, pubsubSubscriptionNewIdsJob));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}

	@Bean
	public PubSubInboundChannelAdapter messageDownloadCompleteChannelAdapter(
			@Qualifier("pubsubInputChannelDownloadComplete") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", idsGcpProject, pubsubSubscriptionDownloadComplete));
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
				
				// Read the BigQuery table in IDS and start creating Cloud Tasks
				String idsJobId = msg.getPayload().getIdsJobId();
				
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
			
			try {
				DownloadCompleteMessage msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
						.readValue((byte[]) message.getPayload(), DownloadCompleteMessage.class);
				log.debug(String.format("Message: %s", msg.toString()));
				
				// Delete the Big Query Table associated with this IDS job
				String idsJobId = msg.getPayload().getIdsJobId();
				
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
}