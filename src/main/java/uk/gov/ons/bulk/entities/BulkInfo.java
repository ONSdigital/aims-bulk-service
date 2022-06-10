package uk.gov.ons.bulk.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonDeserialize(using = LocalDateDeserializer.class)
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
public @Data class BulkInfo {
	
	private long runid;
	private String userid;
	private String status;
	private long totalrecs;
	private long recssofar;
	private LocalDateTime startdate;
	private LocalDateTime enddate;
	
	public BulkInfo(String userid, String status, long totalrecs, long recssofar) {
		super();
		this.userid = userid;
		this.status = status;
		this.totalrecs = totalrecs;
		this.recssofar = recssofar;
	}
}
