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

    public static String createTable(BigQuery bigquery, String datasetName, String tableName, Schema schema) {
        try {
            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigquery.create(tableInfo);
            log.info(String.format("Table %s created successfully", tableId));
        } catch (BigQueryException e) {
            log.error(String.format("Table was not created. \n %s", e.toString()));
        }
        return "OK";
    }

    /**
     * Send individual address to GCP Cloud Function for matching.
     * This won't work locally as it requires a service account.
     * Service account has Cloud Functions Invoker role but must also authenticate.
     *
     * @param creatTaskFunction the name of the function
     * @param jobId the id for this job
     * @param id    input address id
     * @param input the address to match
     *  @throws IOException
     */
    public static void createTask(String createTaskFunction, String jobId, String id, String input) throws IOException {

        BulkAddressController.BulkJobRequest bjr = new BulkAddressController.BulkJobRequest(jobId, id, input);
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

        if (!(credentials instanceof IdTokenProvider)) {
            throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
        }

        IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
                .setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createTaskFunction).build();

        GenericUrl genericUrl = new GenericUrl(createTaskFunction);
        HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
        HttpTransport transport = new NetHttpTransport();
        HttpContent content = new JsonHttpContent(new JacksonFactory(), bjr.getJob());

        HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
        request.execute();
    }



}

