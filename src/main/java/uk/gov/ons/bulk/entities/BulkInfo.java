package uk.gov.ons.bulk.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonDeserialize(using = LocalDateDeserializer.class)
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
public @Data class BulkInfo {

	@Schema (type="long", example="42")
	private long jobid;
	@Schema (type="string", example="smithrm")
	private String userid;
	@Schema (type="string", example="in-progress")
	private String status;
	@Schema (type="long", example="5000000")
	private long totalrecs;
	@Schema (type="long", example="1000000")
	private long recssofar;
	private LocalDateTime startdate;
	private LocalDateTime enddate;
	@Schema (type="string", example="data set 3")
	private String dataset;
	@Schema (type="string", example="topic 1")
	private String topic;
	@Schema (type="string", example="{'header_export': 'true'}")
	private String uimetadata;
	


	public BulkInfo(String userid, String status, long totalrecs, long recssofar, String dataset, String topic,
			String uimetadata) {
		super();
		this.userid = userid;
		this.status = status;
		this.totalrecs = totalrecs;
		this.recssofar = recssofar;
		this.dataset = dataset;
		this.topic = topic;
		this.uimetadata = uimetadata;
	}
}
