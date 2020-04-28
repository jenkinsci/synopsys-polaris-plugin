package com.synopsys.integration.jenkins.polaris.workflow;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.polaris.common.cli.PolarisCliResponseUtility;
import com.synopsys.integration.polaris.common.cli.model.CliCommonResponseModel;
import com.synopsys.integration.polaris.common.cli.model.CommonIssueSummary;
import com.synopsys.integration.polaris.common.cli.model.CommonScanInfo;
import com.synopsys.integration.polaris.common.cli.model.CommonToolInfo;
import com.synopsys.integration.polaris.common.exception.PolarisIntegrationException;
import com.synopsys.integration.polaris.common.request.PolarisRequestFactory;
import com.synopsys.integration.polaris.common.service.CountService;
import com.synopsys.integration.polaris.common.service.JobService;
import com.synopsys.integration.stepworkflow.SubStepResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PolarisCliResponseUtility.class, PolarisRequestFactory.class })
public class GetTotalIssueCountTest {
    public static final Integer EXPECTED_ISSUE_COUNT = 5;
    public static final Integer VALID_TIMEOUT = 1;
    public static final Integer INVALID_TIMEOUT = -1;
    public static final String VALID_ISSUE_API_URL = "https://www.example.com/api/issues/";
    public static final String SUCCESSFUL_JOB_STATUS_URL = "https://www.example.com/api/jobs/successfuljob/";
    public static final String FAILING_JOB_STATUS_URL = "https://www.example.com/api/jobs/failingjob/";
    private CliCommonResponseModel mockedResponseModel;
    private SubStepResponse<String> mockedPreviousResponse;
    private JenkinsIntLogger mockedLogger;
    private CountService mockedCountService;
    private JobService mockedJobService;
    private CommonScanInfo mockedScanInfo;
    private CommonToolInfo successfulToolA;
    private CommonToolInfo successfulToolB;
    private CommonToolInfo failingTool;
    private CommonToolInfo invalidTool;

    @Before
    public void setUpMocks() throws IntegrationException, InterruptedException {
        mockedLogger = Mockito.mock(JenkinsIntLogger.class);
        mockedCountService = Mockito.mock(CountService.class);
        mockedJobService = Mockito.mock(JobService.class);

        final String mockJson = "Mock cli-scan.json";
        mockedPreviousResponse = Mockito.mock(SubStepResponse.class);
        Mockito.when(mockedPreviousResponse.isFailure()).thenReturn(false);
        Mockito.when(mockedPreviousResponse.hasData()).thenReturn(true);
        Mockito.when(mockedPreviousResponse.getData()).thenReturn(mockJson);

        PowerMockito.mockStatic(PolarisCliResponseUtility.class);
        final PolarisCliResponseUtility polarisCliResponseUtility = Mockito.mock(PolarisCliResponseUtility.class);
        mockedResponseModel = Mockito.mock(CliCommonResponseModel.class);
        Mockito.when(PolarisCliResponseUtility.defaultUtility(mockedLogger)).thenReturn(polarisCliResponseUtility);
        Mockito.when(polarisCliResponseUtility.getPolarisCliResponseModelFromString(mockJson)).thenReturn(mockedResponseModel);

        mockedScanInfo = Mockito.mock(CommonScanInfo.class);
        Mockito.when(mockedResponseModel.getScanInfo()).thenReturn(mockedScanInfo);

        final CommonToolInfo commonToolInfo = Mockito.mock(CommonToolInfo.class);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Collections.singletonList(commonToolInfo));

        Mockito.when(mockedCountService.getTotalIssueCountFromIssueApiUrl(VALID_ISSUE_API_URL)).thenReturn(EXPECTED_ISSUE_COUNT);

        successfulToolA = Mockito.mock(CommonToolInfo.class);
        Mockito.when(successfulToolA.getJobStatusUrl()).thenReturn(SUCCESSFUL_JOB_STATUS_URL);
        successfulToolB = Mockito.mock(CommonToolInfo.class);
        Mockito.when(successfulToolB.getJobStatusUrl()).thenReturn(SUCCESSFUL_JOB_STATUS_URL);
        failingTool = Mockito.mock(CommonToolInfo.class);
        Mockito.when(failingTool.getJobStatusUrl()).thenReturn(FAILING_JOB_STATUS_URL);
        invalidTool = Mockito.mock(CommonToolInfo.class);
        Mockito.when(invalidTool.getJobStatusUrl()).thenReturn(null);

        Mockito.doThrow(new IntegrationException()).when(mockedJobService).waitForJobStateIsCompletedOrDieByUrl(FAILING_JOB_STATUS_URL, VALID_TIMEOUT, JobService.DEFAULT_WAIT_INTERVAL);
    }

    @Test
    public void testGetCountFromIssueSummary() throws IOException, InterruptedException, PolarisIntegrationException {
        final CommonIssueSummary commonIssueSummary = Mockito.mock(CommonIssueSummary.class);
        Mockito.when(commonIssueSummary.getTotalIssueCount()).thenReturn(EXPECTED_ISSUE_COUNT);
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.of(commonIssueSummary));

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, VALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        Assert.assertTrue("Step response should be successful and contain data", response.isSuccess() && response.hasData());
        Assert.assertEquals(EXPECTED_ISSUE_COUNT, response.getData());
    }

    @Test
    public void testGetCountFromIssueSummaryWithInvalidTimeout() throws IOException, InterruptedException, PolarisIntegrationException {
        final CommonIssueSummary commonIssueSummary = Mockito.mock(CommonIssueSummary.class);
        Mockito.when(commonIssueSummary.getTotalIssueCount()).thenReturn(EXPECTED_ISSUE_COUNT);
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.of(commonIssueSummary));

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, INVALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        // Since no polling was done, the timeout shouldn't matter -- rotte APR 2020
        Assert.assertTrue("Step response should be successful and contain data", response.isSuccess() && response.hasData());
        Assert.assertEquals(EXPECTED_ISSUE_COUNT, response.getData());
    }

    @Test
    public void testGetCountFromPolaris() throws InterruptedException, IntegrationException {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, successfulToolB));

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, VALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        Mockito.verify(mockedJobService, Mockito.times(2)).waitForJobStateIsCompletedOrDieByUrl(SUCCESSFUL_JOB_STATUS_URL, VALID_TIMEOUT, JobService.DEFAULT_WAIT_INTERVAL);
        Assert.assertTrue("Step response should be successful and contain data", response.isSuccess() && response.hasData());
        Assert.assertEquals(EXPECTED_ISSUE_COUNT, response.getData());
    }

    @Test
    public void testGetCountFromPolarisWithInvalidTimeout() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Collections.emptyList());

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, INVALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        Assert.assertTrue("Step response should be failure", response.isFailure());
    }

    @Test
    public void testGetCountFromPolarisWithFailingTool() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, failingTool, successfulToolB));

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, INVALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        Assert.assertTrue("Step response should be failure", response.isFailure());
    }

    @Test
    public void testGetCountFromPolarisWithInvalidTool() {
        Mockito.when(mockedResponseModel.getIssueSummary()).thenReturn(Optional.empty());
        Mockito.when(mockedScanInfo.getIssueApiUrl()).thenReturn(VALID_ISSUE_API_URL);
        Mockito.when(mockedResponseModel.getTools()).thenReturn(Arrays.asList(successfulToolA, invalidTool, successfulToolB));

        final GetTotalIssueCount getTotalIssueCount = new GetTotalIssueCount(mockedLogger, mockedCountService, mockedJobService, INVALID_TIMEOUT);
        final SubStepResponse<Integer> response = getTotalIssueCount.run(mockedPreviousResponse);

        Assert.assertTrue("Step response should be failure", response.isFailure());
    }

}
