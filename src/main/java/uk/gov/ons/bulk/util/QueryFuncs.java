package uk.gov.ons.bulk.util;

import java.util.ArrayList;
import java.util.Map;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryFuncs {

    public static Iterable<FieldValueList> runQuery(String queryText,BigQuery bigquery) throws InterruptedException {

        if (queryText == "") return new ArrayList<FieldValueList>();
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryText).build();
        return bigquery.query(queryConfig).iterateAll();

    }

    public static void createTable(BigQuery bigquery, String datasetName, String tableName, Schema schema) {
        try {
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigquery.create(tableInfo);
            log.info(String.format("Table %s created successfully", tableId));
        } catch (BigQueryException e) {
            log.error(String.format("Table was not created. \n %s", e.toString()));
        }
    }

//    public static String InsertRow(BigQuery bigquery, TableId tableId, Map<String, Object> row1Data) {
//        InsertAllResponse response = bigquery
//                .insertAll(InsertAllRequest.newBuilder(tableId).addRow("runid", row1Data).build());
//		
//        if (response.hasErrors()) {
//			// If any of the insertions failed, this lets you inspect the errors
//			response.getInsertErrors()
//					.forEach((key, value) -> log.error(String.format("Row: %s Errors: %s", key, value.toString())));
//		}
//
//        return response.toString();
//    }
}

