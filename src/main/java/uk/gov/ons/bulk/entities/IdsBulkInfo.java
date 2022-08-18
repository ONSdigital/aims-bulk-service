package uk.gov.ons.bulk.entities;

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
	
	private String idsJobId;

	public IdsBulkInfo(String idsJobId, String userid, String status, long totalrecs, long recssofar) {
		this.idsJobId = idsJobId;
		super.setUserid(userid);
		super.setStatus(status);
		super.setTotalrecs(totalrecs);
		super.setRecssofar(recssofar);
	}
}
