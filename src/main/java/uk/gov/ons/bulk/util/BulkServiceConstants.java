package uk.gov.ons.bulk.util;

public final class BulkServiceConstants {

	public static final String BIG_QUERY_TABLE_PREFIX = "results_";
	public static final String BIG_QUERY_IDS_TABLE_PREFIX = "ids_results_";
	
	public enum Status {
		IP("in-progress"), 
		PF("processing-finished"), 
		RR("results-ready"),
		RE("results-exported"),
		F("failed"),
		RD("results-deleted");
		
		private String status;
		
		Status(String status) {
			this.status = status;
		}
		
		public String getStatus() {
			return status;
		}
	}
}
