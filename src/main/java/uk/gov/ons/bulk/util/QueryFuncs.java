package uk.gov.ons.bulk.util;

import java.io.IOException;
import java.lang.InterruptedException;
import java.util.*;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.cloud.bigquery.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.controllers.BulkAddressController;

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

    public static String InsertRow(BigQuery bigquery, TableId tableId, Map<String, Object> row1Data) {
        InsertAllResponse response = bigquery
                .insertAll(InsertAllRequest.newBuilder(tableId).addRow("runid", row1Data).build());
        if (response.hasErrors()) {
            // If any of the insertions failed, this lets you inspect the errors
            for (Map.Entry<Long, List<BigQueryError>> entry : response.getInsertErrors().entrySet()) {
                log.error(String.format("entry: %s", entry.toString()));
            }

        }
        return response.toString();
    }

}

