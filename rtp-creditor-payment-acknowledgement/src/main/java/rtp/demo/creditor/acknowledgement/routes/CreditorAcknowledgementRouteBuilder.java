package rtp.demo.creditor.acknowledgement.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import rtp.demo.creditor.acknowledgement.beans.MessageStatusReportTransformer;

@Component
public class CreditorAcknowledgementRouteBuilder extends RouteBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(CreditorAcknowledgementRouteBuilder.class);

	private String kafkaBootstrap = System.getenv("BOOTSTRAP_SERVERS");
	private String kafkaCreditorAcknowledgmentTopic = System.getenv("MOCK_RTP_CREDITOR_ACK_TOPIC");
	private String kafkaCreditorPaymentsTopic = System.getenv("CREDITOR_PAYMENTS_TOPIC");
	private String consumerMaxPollRecords = System.getenv("CONSUMER_MAX_POLL_RECORDS");
	private String consumerCount = System.getenv("CONSUMER_COUNT");
	private String consumerSeekTo = System.getenv("CONSUMER_SEEK_TO");
	private String consumerGroup = System.getenv("CONSUMER_GROUP");

	@Override
	public void configure() throws Exception {
		LOG.info("Configuring Creditor Acknowledgement Routes");

		KafkaComponent kafka = new KafkaComponent();
		this.getContext().addComponent("kafka", kafka);

		from("kafka:" + kafkaCreditorPaymentsTopic + "?brokers=" + kafkaBootstrap + "&maxPollRecords="
				+ consumerMaxPollRecords + "&consumersCount=" + consumerCount + "&seekTo=" + consumerSeekTo
				+ "&groupId=" + consumerGroup
				+ "&valueDeserializer=rtp.demo.creditor.domain.payments.serde.PaymentDeserializer").routeId("FromKafka")
						.log("\n/// Creditor Acknowledegement Route >>> ${body}")
						.bean(MessageStatusReportTransformer.class, "toMessageStatusReport").log(">>> ${body}")
						.to("kafka:" + kafkaCreditorAcknowledgmentTopic
								+ "?serializerClass=rtp.message.model.serde.FIToFIPaymentStatusReportV07Serializer");
	}

}