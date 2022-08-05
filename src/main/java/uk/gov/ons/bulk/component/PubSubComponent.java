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
import uk.gov.ons.bulk.entities.Message;

@Slf4j
@Component
public class PubSubComponent {
	
	@Value("${ids.cloud.gcp.project-id}")
	private String idsGcpProject;
	
	@Value("${ids.pubsub.subscription-table-available}")
	private String pubsubSubscriptionTableAvailable;
	
	@Value("${ids.pubsub.subscription-table-pulled}")
	private String pubsubSubscriptionTablePulled;
	
	@Bean
	public MessageChannel pubsubInputChannelTableAvailable() {
		return new DirectChannel();
	}
	
	@Bean
	public MessageChannel pubsubInputChannelTablePulled() {
		return new DirectChannel();
	}
	
	@Bean
	public PubSubInboundChannelAdapter messageTableAvailableChannelAdapter(
			@Qualifier("pubsubInputChannelTableAvailable") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", idsGcpProject, pubsubSubscriptionTableAvailable));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}

	@Bean
	public PubSubInboundChannelAdapter messageTablePulledChannelAdapter(
			@Qualifier("pubsubInputChannelTablePulled") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", idsGcpProject, pubsubSubscriptionTablePulled));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}
	
	@Bean
	@ServiceActivator(inputChannel = "pubsubInputChannelTableAvailable")
	public MessageHandler messageTableAvailableReceiver() {
		return message -> {
			log.debug("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
			
			try {
				Message msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
						.readValue((byte[]) message.getPayload(), Message.class);
				log.debug(String.format("Message: %s", msg.toString()));
				
				// Schedule Job for this resultset
				String jobId = msg.getPayload().getJobId();
				
				
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
	@ServiceActivator(inputChannel = "pubsubInputChannelTablePulled")
	public MessageHandler messageTablePulledReceiver() {
		return message -> {
			log.debug("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
			
			try {
				Message msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
						.readValue((byte[]) message.getPayload(), Message.class);
				log.debug(String.format("Message: %s", msg.toString()));
				
				// Schedule Job for this resultset
				String jobId = msg.getPayload().getJobId();
				
				
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
