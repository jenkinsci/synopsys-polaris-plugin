package com.synopsys.integration.polaris.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.polaris.common.api.PolarisPagedResourceResponse;
import com.synopsys.integration.polaris.common.api.PolarisPaginationMeta;
import com.synopsys.integration.polaris.common.api.PolarisResource;
import com.synopsys.integration.polaris.common.api.model.JobAttributes;
import com.synopsys.integration.polaris.common.request.PolarisRequestFactory;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClientTestIT;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.rest.support.AuthenticationSupport;

public class PolarisServiceTest {
    public static String PAGE_ONE_OFFSET = "0";
    public static String PAGE_TWO_OFFSET = "1";
    public static String PAGE_THREE_OFFSET = "2";
    public static String PAGE_FOUR_OFFSET = "3";

    private final HttpUrl BASE_URL = new HttpUrl("https://google.com");

    public PolarisServiceTest() throws IntegrationException {
    }

    private static Stream<Arguments> createGetAllMockData() {
        Map<String, String> getAllOnOnePageMap = new HashMap<>();
        getAllOnOnePageMap.put(PAGE_ONE_OFFSET, "jobs_all_on_one_page.json");

        Map<String, String> getAllMultiPageMap = new HashMap<>();
        getAllMultiPageMap.put(PAGE_ONE_OFFSET, "jobs_page_1_of_3.json");
        getAllMultiPageMap.put(PAGE_TWO_OFFSET, "jobs_page_2_of_3.json");
        getAllMultiPageMap.put(PAGE_THREE_OFFSET, "jobs_page_3_of_3.json");

        Map<String, String> lessThanExpectedMap = new HashMap<>(getAllMultiPageMap);
        lessThanExpectedMap.remove(PAGE_THREE_OFFSET);

        Map<String, String> moreThanExpectedMap = new HashMap<>(getAllMultiPageMap);
        moreThanExpectedMap.put(PAGE_TWO_OFFSET, "jobs_page_2_of_4.json");
        moreThanExpectedMap.put(PAGE_THREE_OFFSET, "jobs_page_3_of_4.json");
        moreThanExpectedMap.put(PAGE_FOUR_OFFSET, "jobs_page_4_of_4.json");

        Map<String, String> duplicatedDataMap = new HashMap<>(getAllMultiPageMap);
        duplicatedDataMap.put(PAGE_TWO_OFFSET, "jobs_page_1_of_3.json");

        return Stream.of(
            Arguments.of(getAllOnOnePageMap, 3, 3),
            Arguments.of(getAllMultiPageMap, 1, 3),
            Arguments.of(lessThanExpectedMap, 1, 2),
            Arguments.of(moreThanExpectedMap, 1, 3),
            Arguments.of(duplicatedDataMap, 1, 3)
        );
    }

    @Test
    public void createDefaultPolarisGetRequestTest() throws IntegrationException {
        Request request = PolarisRequestFactory.createDefaultPagedGetRequest(BASE_URL);
        assertNotNull(request);
    }

    @Test
    public void executeGetRequestTestIT() throws IntegrationException {
        String testPolarisUrl = System.getenv(AccessTokenPolarisHttpClientTestIT.ENV_POLARIS_URL);
        String accessToken = System.getenv(AccessTokenPolarisHttpClientTestIT.ENV_POLARIS_ACCESS_TOKEN);

        assumeTrue(StringUtils.isNotBlank(testPolarisUrl));
        assumeTrue(StringUtils.isNotBlank(accessToken));

        HttpUrl testPolarisHttpUrl = new HttpUrl(testPolarisUrl);
        Gson gson = new Gson();
        IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO);
        AccessTokenPolarisHttpClient httpClient = new AccessTokenPolarisHttpClient(logger, 100, ProxyInfo.NO_PROXY_INFO, new HttpUrl(testPolarisUrl), accessToken, gson, new AuthenticationSupport());

        PolarisService polarisService = new PolarisService(httpClient, new PolarisJsonTransformer(gson, logger), PolarisRequestFactory.DEFAULT_LIMIT);

        HttpUrl apiHttpUrl = testPolarisHttpUrl.appendRelativeUrl("/api/jobs/jobs");

        PolarisPagedResourceResponse<PolarisResource<JobAttributes>> jobsResponse = polarisService.executePagedRequest(apiHttpUrl, JobAttributes.class, 0, 1);
        List<PolarisResource<JobAttributes>> jobsList = jobsResponse.getData();
        assertNotNull(jobsList);
        PolarisPaginationMeta meta = jobsResponse.getMeta();
        assertNotNull(meta);
    }

    @ParameterizedTest
    @MethodSource("createGetAllMockData")
    public void testGetAll(Map<String, String> offsetsToResults, int pageSize, int expectedTotal) throws IntegrationException {
        HttpUrl apiHttpUrl = BASE_URL.appendRelativeUrl("/api/jobs/jobs");

        AccessTokenPolarisHttpClient polarisHttpClient = Mockito.mock(AccessTokenPolarisHttpClient.class);
        mockClientBehavior(polarisHttpClient, apiHttpUrl, offsetsToResults, "jobs_no_more_results.json");

        PolarisJsonTransformer polarisJsonTransformer = new PolarisJsonTransformer(new Gson(), new PrintStreamIntLogger(System.out, LogLevel.INFO));
        PolarisService polarisService = new PolarisService(polarisHttpClient, polarisJsonTransformer, pageSize);
        try {
            List<PolarisResource<JobAttributes>> allPagesResponse = polarisService.getAll(apiHttpUrl, JobAttributes.class);
            assertEquals(expectedTotal, allPagesResponse.size());
        } catch (IntegrationException e) {
            fail("Mocked response caused PolarisService::GetAllResponses to throw an unexpected IntegrationException, which should never happen in this test.", e);
        }
    }

    private void mockClientBehavior(AccessTokenPolarisHttpClient polarisHttpClient, HttpUrl url, Map<String, String> offsetsToResults, String emptyResultsPage) {
        try {
            for (Map.Entry<String, String> entry : offsetsToResults.entrySet()) {
                Response response = Mockito.mock(Response.class);
                Mockito.when(response.getContentString()).thenReturn(getPreparedContentStringFrom(entry.getValue()));

                ArgumentMatcher<Request> isMockedRequest = request -> requestMatches(request, url, entry.getKey());
                Mockito.when(polarisHttpClient.execute(Mockito.argThat(isMockedRequest))).thenReturn(response);
            }

            Response emptyResponse = Mockito.mock(Response.class);
            Mockito.when(emptyResponse.getContentString()).thenReturn(getPreparedContentStringFrom(emptyResultsPage));
            ArgumentMatcher<Request> isOutOfBounds = request -> requestOffsetOutOfBounds(request, url, offsetsToResults);

            Mockito.when(polarisHttpClient.execute(Mockito.argThat(isOutOfBounds))).thenReturn(emptyResponse)
                .thenThrow(new AssertionFailedError("Client requested more pages after getting back a page of empty results."));
        } catch (IOException | IntegrationException e) {
            fail("Unexpected " + e.getClass() + " was thrown while mocking client behavior. Please check the test for errors.", e);
        }
    }

    private Boolean requestMatches(Request request, HttpUrl url, String offset) {
        if (null != request && request.getUrl().equals(url)) {
            return request.getQueryParameters()
                       .get(PolarisRequestFactory.OFFSET_PARAMETER)
                       .stream()
                       .allMatch(requestOffset -> requestOffset.equals(offset));
        }
        return false;
    }

    private Boolean requestOffsetOutOfBounds(Request request, HttpUrl url, Map<String, String> offsetsToResults) {
        if (null != request && request.getUrl().equals(url)) {
            return request.getQueryParameters()
                       .get(PolarisRequestFactory.OFFSET_PARAMETER)
                       .stream()
                       .noneMatch(offsetsToResults::containsKey);
        }
        return false;
    }

    private String getPreparedContentStringFrom(String resourceName) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("/PolarisService/" + resourceName), StandardCharsets.UTF_8);
    }

}
