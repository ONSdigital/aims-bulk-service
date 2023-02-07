package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonDeserialize(using = LocalDateDeserializer.class)
public @Data class IdsBulkInfo extends BulkInfo {
	
	@JsonProperty("idsjobid")
	private String idsJobId;
	private boolean test;

	public IdsBulkInfo(String idsJobId, String userid, String status, long totalrecs, long recssofar, boolean test) {
		this.idsJobId = idsJobId;
		this.test = test;
		super.setUserid(userid);
		super.setStatus(status);
		super.setTotalrecs(totalrecs);
		super.setRecssofar(recssofar);
	}
}