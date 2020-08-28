package com.synopsys.integration.jenkins.polaris.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.polaris.service.PolarisCliIssueCountService;
import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.cli.model.CliCommonResponseModel;
import com.synopsys.integration.polaris.common.cli.model.CommonIssueSummary;
import com.synopsys.integration.polaris.common.cli.model.CommonScanInfo;
import com.synopsys.integration.polaris.common.cli.model.CommonToolInfo;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.rest.HttpUrl;

public class PolarisCliIssueCountServiceTest {
    public static final Integer EXPECTED_ISSUE_COUNT = 5;
    public static final Integer VALID_TIMEOUT = 1;
    public static final Integer INVALID_TIMEOUT = -1;
    public static final String VALID_ISSUE_API_URL = "https://www.example.com/api/issues/";
    public static final String SUCCESSFUL_JOB_STATUS_URL = "https://www.example.com/api/jobs/successfuljob/";
    public static final String FAILING_JOB_STATUS_URL = "https://www.example.com/api/jobs/failingjob/";
    private static final String MOCK_JSON = "Json to convert into a mocked ResponseModel";
    private CliCommonResponseModel mockedResponseModel;
    private JenkinsIntLogger mockedLogger;
    private CountService mockedCountService;
    private JobService mockedJobService;
    private PolarisCliResponseUtility mockedPolarisCliResponseUtility;
    private CommonScanInfo mockedScanInfo;
    private CommonToolInfo successfulToolA;
    private CommonToolInfo successfulToolB;
    private CommonToolInfo failingTool;
    private CommonToolInfo invalidTool;

    @BeforeEach
    public void setUpMocks() {
        try {
            mockedLogger = Mockito.mock(JenkinsIntLogger.class);
            mockedCountService = Mockito.mock(CountService.class);
            mockedJobService = Mockito.mock(JobService.class);

            mockedPolarisCliResponseUtility = Mockito.mock(PolarisCliResponseUtility.class);
            mockedResponseModel = Mockito.mock(CliCommonResponseModel.class);
            Mockito.when(mockedPolarisCliResponseUtility.getPolarisCliResponseModelFromString(MOCK_JSON)).thenReturn(mockedResponseModel);

            mockedScanInfo = Mockito.mock(CommonScanInfo.class);
            Mockito.when(mockedResponseModel.getScanInfo()).thenReturn(mockedScanInfo);

            Mockito.when(mockedCountService.getTotalIssueCountFromIssueApiUrl(VALID_ISSUE_API_URL)).thenReturn(EXPECTED_ISSUE_COUNT);

            successfulToolA = new CommonToolInfo();
            successfulToolA.setJobStatusUrl(SUCCESSFUL_JOB_STATUS_URL);

            successfulToolB = new CommonToolInfo();
            successfulToolB.setJobStatusUrl(SUCCESSFUL_JOB_STATUS_URL);

            failingTool = new CommonToolInfo();
            failingTool.setJobStatusUrl(FAILING_JOB_STATUS_URL);

            invalidTool = new CommonToolInfo();

            Mockito.doThrow(new IntegrationException()).when(mockedJobService).waitForJobStateIsCompletedOrDieByUrl(new HttpUrl(FAILING_JOB_STATUS_URL), VALID_TIMEOUT, JobService.DEFAULT_WAIT_INTERVAL);
        } catch (Exception e) {
            fail("Unexpected exception in test code: ", e);
        }
    }

    @Test
    public void testGetCountFromIssueSummary() {
        CommonIssueSummary mockedIssueSummary = Mockito.mock(CommonIssueSummary.class);
        Mockito.when(mockedIssueSummary.getTotalIssueCount()).thenReturn(EXPECTED_ISSUE_COUNT);
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.of(mockedIssueSummary));

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);
        try {
            Integer actualIssueCount = polarisCliIssueCountService.getIssueCount(VALID_TIMEOUT, MOCK_JSON);

            assertEquals(EXPECTED_ISSUE_COUNT, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

    @Test
    public void testGetCountFromIssueSummaryWithInvalidTimeout() {
        CommonIssueSummary mockedIssueSummary = Mockito.mock(CommonIssueSummary.class);
        Mockito.when(mockedIssueSummary.getTotalIssueCount()).thenReturn(EXPECTED_ISSUE_COUNT);
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.of(mockedIssueSummary));

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);

        // Since no polling was done, the timeout shouldn't matter -- rotte APR 2020
        try {
            Integer actualIssueCount = polarisCliIssueCountService.getIssueCount(INVALID_TIMEOUT, MOCK_JSON);

            assertEquals(EXPECTED_ISSUE_COUNT, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

    @Test
    public void testGetCountFromPolaris() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, successfulToolB));

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);

        try {
            Integer actualIssueCount = polarisCliIssueCountService.getIssueCount(VALID_TIMEOUT, MOCK_JSON);

            Mockito.verify(mockedJobService, Mockito.times(2)).waitForJobStateIsCompletedOrDieByUrl(new HttpUrl(SUCCESSFUL_JOB_STATUS_URL), VALID_TIMEOUT, JobService.DEFAULT_WAIT_INTERVAL);
            assertEquals(EXPECTED_ISSUE_COUNT, actualIssueCount);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

    @Test
    public void testGetCountFromPolarisWithInvalidTimeout() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Collections.emptyList());

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);

        try {
            assertThrows(PolarisIntegrationException.class, () -> polarisCliIssueCountService.getIssueCount(INVALID_TIMEOUT, MOCK_JSON));
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

    @Test
    public void testGetCountFromPolarisWithFailingTool() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, failingTool, successfulToolB));

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);

        try {
            assertThrows(IntegrationException.class, () -> polarisCliIssueCountService.getIssueCount(VALID_TIMEOUT, MOCK_JSON));
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

    @Test
    public void testGetCountFromPolarisWithInvalidTool() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, invalidTool, successfulToolB));

        PolarisCliIssueCountService polarisCliIssueCountService = new PolarisCliIssueCountService(mockedLogger, mockedCountService, mockedJobService, mockedPolarisCliResponseUtility);

        try {
            assertThrows(PolarisIntegrationException.class, () -> polarisCliIssueCountService.getIssueCount(VALID_TIMEOUT, MOCK_JSON));
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code, it may need to be fixed.", e);
        }
    }

}
