package uk.gov.ons.bulk.service;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.IdsRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CloudTaskService {

    @Value("${aims.cloud-functions.create-cloud-task-function}")
    private String createTaskFunction;

    @Value("${aims.report-frequency}")
    private double reportFrequency;

    @Value("${exponential-backoff.initial-interval-millis}")
    private int initialIntervalMillis;

    @Value("${exponential-backoff.max-elapsed-time-millis}")
    private int maxElapsedTimeMillis;

    @Value("${exponential-backoff.max-interval-millis}")
    private int maxIntervalMillis;

    @Value("${exponential-backoff.multiplier}")
    private double multiplier;

    @Value("${exponential-backoff.randomization-factor}")
    private double randomizationFactor;

    private GenericUrl genericUrl;
    private HttpCredentialsAdapter adapter;
    private HttpTransport transport;

    public void init() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

        if (!(credentials instanceof IdTokenProvider)) {
            throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
        }

        IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
                .setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createTaskFunction).build();

        genericUrl = new GenericUrl(createTaskFunction);
        adapter = new HttpCredentialsAdapter(tokenCredential);
        transport = new NetHttpTransport();
    }

    /**
     * Send each address to GCP Cloud Function for matching. Done asynchronously so
     * as not to block the client response. This won't work locally as it requires a
     * service account. Service account has Cloud Functions Invoker role but must
     * also authenticate.
     *
     * @param jobId
     * @param addresses
     * @throws IOException
     */
    @Async
    public void createTasks(long jobId, BulkRequest[] addresses, long totalAddresses,
                            BulkRequestParams bulkRequestParams, String user, String topic, String dataset, String uiMetadata) throws IOException {

        List<Integer> reportAddresses = reportAddresses(addresses.length);
        reportAddresses.add(addresses.length);
        init();
        ExponentialBackOff backoff = createExponentialBackOff();

        for (int i = 0; i < addresses.length; i++) {

            if (reportAddresses.contains(i + 1)) {
                log.debug("Reporting: {}", i + 1);
            }

            BulkJobRequest bjr = new BulkJobRequest(String.valueOf(jobId), "", user, topic, dataset, uiMetadata, addresses[i].getId(),
                    addresses[i].getAddress(), String.valueOf(i + 1), String.valueOf(totalAddresses),
                    String.valueOf(reportAddresses.contains(i + 1)), bulkRequestParams);

            HttpRequest request = createHttpRequest(bjr, backoff);
            HttpResponse response = request.setConnectTimeout(60000).execute();

            try {
                log.debug("Response Status Code: {}", response.getStatusCode());
                log.debug("Response Status Message: {}", response.getStatusMessage());

                if (log.isDebugEnabled()) {
                    try (InputStream is = response.getContent()) {
                        log.debug("Response Object: {}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
                    }

                    if (response.getRequest().getContent() instanceof JsonHttpContent) {
                        log.debug("Request Content: {}", ((JsonHttpContent) response.getRequest().getContent()).getData());
                    }
                }
            } finally {
                response.disconnect();
            }
        }
    }

    @Async
    public void createIdsTasks(long jobId, String idsJobId, List<IdsRequest> addresses, long totalAddresses,
                               BulkRequestParams bulkRequestParams, String user, String topic, String dataset, String uiMetadata) throws IOException {

        List<Integer> reportAddresses = reportAddresses(addresses.size());
        reportAddresses.add(addresses.size());
        init();
        ExponentialBackOff backoff = createExponentialBackOff();

        for (int i = 0; i < addresses.size(); i++) {

            if (reportAddresses.contains(i + 1)) {
                log.debug("Reporting: {}", i + 1);
            }

            BulkJobRequest bjr = new BulkJobRequest(String.valueOf(jobId), idsJobId, user, topic, dataset, uiMetadata, addresses.get(i).getId(),
                    addresses.get(i).getAddress(), String.valueOf(i + 1), String.valueOf(totalAddresses),
                    String.valueOf(reportAddresses.contains(i + 1)), bulkRequestParams);

            HttpRequest request = createHttpRequest(bjr, backoff);
            HttpResponse response = request.setConnectTimeout(60000).execute();

            try {
                log.debug("Response Status Code: {}", response.getStatusCode());
                log.debug("Response Status Message: {}", response.getStatusMessage());

                if (log.isDebugEnabled()) {
                    try (InputStream is = response.getContent()) {
                        log.debug("Response Object: {}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
                    }

                    if (response.getRequest().getContent() instanceof JsonHttpContent) {
                        log.debug("Request Content: {}", ((JsonHttpContent) response.getRequest().getContent()).getData());
                    }
                }
            } finally {
                response.disconnect();
            }
        }

    }

    private ExponentialBackOff createExponentialBackOff() {
        return new ExponentialBackOff.Builder()
                .setInitialIntervalMillis(initialIntervalMillis)
                .setMaxElapsedTimeMillis(maxElapsedTimeMillis)
                .setMaxIntervalMillis(maxIntervalMillis)
                .setMultiplier(multiplier)
                .setRandomizationFactor(randomizationFactor)
                .build();
    }

    private HttpRequest createHttpRequest(BulkJobRequest bjr, ExponentialBackOff backoff) throws IOException {
        HttpContent content = new JsonHttpContent(new GsonFactory(), bjr.getJob());
        HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
        request.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(backoff));
        return request;
    }

    private List<Integer> reportAddresses(double length) {

        double x = reportFrequency / 100;
        List<Integer> reportAddresses = new ArrayList<Integer>();

        do {
            reportAddresses.add((int) Math.round(length * x));
            x = x + (reportFrequency / 100);
        } while (x < 1);

        return reportAddresses;
    }

    public @Data class BulkJobRequest {
        private Map<String, String> job;

        public BulkJobRequest(String jobId, String idsJobId, String user, String topic, String dataset, String uiMetadata, String id, String address, String addressNumber,
                              String totalAddresses, String report, BulkRequestParams bulkRequestParams) {
            super();
            job = new HashMap<String, String>();
            job.put("jobId", jobId);
            job.put("idsJobId", idsJobId);
            job.put("user", user);
            job.put("topic", topic);
            job.put("dataset", dataset);
            job.put("uimetadata", uiMetadata);
            job.put("id", id);
            job.put("address", address);
            job.put("item", addressNumber);
            job.put("total", totalAddresses);
            job.put("report", report);
            job.put("limitperaddress", bulkRequestParams.getLimitperaddress());
            job.put("classificationfilter", bulkRequestParams.getClassificationfilter());
            job.put("historical", bulkRequestParams.getHistorical());
            job.put("matchthreshold", bulkRequestParams.getMatchthreshold());
            job.put("verbose", bulkRequestParams.getVerbose());
            job.put("epoch", bulkRequestParams.getEpoch());
            job.put("eboost", bulkRequestParams.getEboost());
            job.put("sboost", bulkRequestParams.getSboost());
            job.put("wboost", bulkRequestParams.getWboost());
            job.put("nboost", bulkRequestParams.getNboost());
            job.put("lboost", bulkRequestParams.getLboost());
            job.put("mboost", bulkRequestParams.getMboost());
            job.put("jboost", bulkRequestParams.getJboost());
            job.put("pafdefault", bulkRequestParams.getPafdefault());
        }
    }
}
