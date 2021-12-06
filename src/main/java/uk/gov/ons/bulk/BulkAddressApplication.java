package uk.gov.ons.bulk;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.Message;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.repository.fat.AddressRepository;
import uk.gov.ons.bulk.service.AddressService;

@Slf4j
@SpringBootApplication
public class BulkAddressApplication {

	@Autowired
	private AddressService addressService;
	
	@Autowired
	private AddressRepository addressRepository;

	@Value("${spring.cloud.gcp.project-id}")
	private String gcpProject;
	
	@Value("${aims.pubsub.subscription}")
	private String pubsubSubscription;
	
	public static void main(String[] args) {
		SpringApplication.run(BulkAddressApplication.class, args);
	}
	
	// Inbound channel adapter.
	@Bean
	public MessageChannel pubsubInputChannel() {
		return new DirectChannel();
	}
	
	@Bean
	public PubSubInboundChannelAdapter messageChannelAdapter(
			@Qualifier("pubsubInputChannel") MessageChannel inputChannel, PubSubTemplate pubSubTemplate) {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate,
				String.format("projects/%s/subscriptions/%s", gcpProject, pubsubSubscription));
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}
	
	@Bean
	@ServiceActivator(inputChannel = "pubsubInputChannel")
	public MessageHandler messageReceiver() {
		return message -> {
			log.debug("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
			
			try {
				Message msg = new ObjectMapper().setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY)).readValue((byte[]) message.getPayload(), Message.class);
				log.debug(String.format("Message: %s", msg.toString()));
				
				// Save the new address to ES
				addressService.createAddressFromMsg(msg.getPayload().getNewAddress().getCollectionCase().getAddress()).subscribe(
						response -> {						
							if (response != null) {
								
								// Show ES Content for new Address - DEMO
								addressRepository.findById(String.valueOf(response.getUprn())).subscribe(address -> {
									log.debug(String.format("ES content for new address with ID: %s = %s", response.getUprn(), address.toString()));
								});
								
								// Send ACK
								BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
										.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
								originalMessage.ack();	
							}
							log.debug(String.format("Response: %s",  response != null ? response.toString() : "Response is null!"));
						});
			} catch (IOException e) {
				log.info(String.format("Unable to read message: %s", e));
			} catch (BulkAddressException cae) {
				log.info(String.format("Unable to read message: %s", cae));
				
				// Send NACK
				BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
						.get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
				originalMessage.nack();	
			}
		};
	}
}
