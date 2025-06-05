package uk.gov.ons.bulk.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.core.io.Resource;

public class DownloadServiceTest {

    private DownloadService downloadService;
    private ResourceLoader mockResourceLoader;

    @BeforeEach
    public void setUp() {
        downloadService = new DownloadService();

        // Set the projectNumber which makes the expectedBucket value
        ReflectionTestUtils.setField(downloadService, "projectNumber", "200008240056");

        mockResourceLoader = mock(ResourceLoader.class);
        ReflectionTestUtils.setField(downloadService, "resourceLoader", mockResourceLoader);
    }

    @Test
    public void testDownloadServiceExpectedInputs() throws Exception {
        String jobId = "123456789012";
        String filename = "results_123456789012.csv.gz";

        String expectedBucket = "results_123456789012_200008240056";
        String expectedGcsPath = "gs://" + expectedBucket + "/" + filename;

        Resource dummyResource = org.mockito.Mockito.mock(Resource.class);
        when(dummyResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockResourceLoader.getResource(expectedGcsPath)).thenReturn(dummyResource);

        InputStream result = downloadService.getResultFile(jobId, filename);
        assertNotNull(result, "Expected a non-null InputStream for a valid GCS path");
    }

    @Test
    public void testDownloadServiceFailingJobId() throws Exception {
        String jobId = "CATS0R0COOL!"; // Invalid jobId
        String filename = "results_123456789012.csv.gz"; // Valid filename to isolate jobId as the issue

        assertThrows(
            IllegalArgumentException.class,
            () -> downloadService.getResultFile(jobId, filename),
            "Expected an IllegalArgumentException when the GCS URL does not match the required pattern"
        );
    }

    @Test
    public void testDownloadServiceFailingFileName() throws Exception {
        String jobId = "123456789012"; // Valid jobId to isolate filename as the issue
        String filename = "results_CATS0R0COOL0.csv.gz"; // Invalid filename

        assertThrows(
            IllegalArgumentException.class,
            () -> downloadService.getResultFile(jobId, filename),
            "Expected an IllegalArgumentException when the GCS URL does not match the required pattern"
        );
    }

}

