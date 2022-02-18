package uk.gov.ons.bulk.util;

import java.lang.InterruptedException;
import java.util.*;
import com.google.cloud.bigquery.*;
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

}

