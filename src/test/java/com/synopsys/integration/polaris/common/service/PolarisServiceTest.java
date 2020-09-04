package com.synopsys.integration.polaris.common.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.polaris.common.request.PolarisRequestFactory;
import com.synopsys.integration.polaris.common.rest.AccessTokenPolarisHttpClient;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.rest.support.UrlSupport;

public class PolarisServiceTest {
    public static String PAGE_ONE_OFFSET = "0";
    public static String PAGE_TWO_OFFSET = "25";
    public static String PAGE_THREE_OFFSET = "50";

    private HttpUrl BASE_URL = new HttpUrl("https://google.com");
    private UrlSupport urlSupport = new UrlSupport();

    public PolarisServiceTest() throws IntegrationException {
    }

    private static Stream<Arguments> createGetAllMockData() {
        Map<String, String> getAllOnOnePageMap = new HashMap<>();
        getAllOnOnePageMap.put(PAGE_ONE_OFFSET, "projects_all_on_one_page.json");

        Map<String, String> getAllMultiPageMap = new HashMap<>();
        getAllMultiPageMap.put(PAGE_ONE_OFFSET, "projects_page_1_of_3.json");
        getAllMultiPageMap.put(PAGE_TWO_OFFSET, "projects_page_2_of_3.json");
        getAllMultiPageMap.put(PAGE_THREE_OFFSET, "projects_page_3_of_3.json");

        Map<String, String> lessProjectsThanExpectedMap = new HashMap<>(getAllMultiPageMap);
        lessProjectsThanExpectedMap.remove(PAGE_THREE_OFFSET);

        Map<String, String> changingTotalMap = new HashMap<>(getAllMultiPageMap);
        changingTotalMap.put(PAGE_TWO_OFFSET, "projects_page_2_of_2.json");

        Map<String, String> duplicatedDataMap = new HashMap<>(getAllMultiPageMap);
        duplicatedDataMap.put(PAGE_TWO_OFFSET, "projects_page_1_of_3.json");

        return Stream.of(
            Arguments.of(getAllOnOnePageMap, 16),
            Arguments.of(getAllMultiPageMap, 66),
            Arguments.of(lessProjectsThanExpectedMap, 50),
            Arguments.of(changingTotalMap, 66),
            Arguments.of(duplicatedDataMap, 66)
        );
    }

    @Test
    public void createDefaultPolarisGetRequestTest() throws IntegrationException {
        Request request = PolarisRequestFactory.createDefaultCommonPolarisPagedGetRequest(BASE_URL.string());
        assertNotNull(request);
    }

    // TODO: Replace with an api we use
    /*
    @Test
    public void executeGetRequestTestIT() throws IntegrationException {
        String baseUrl = System.getenv(AccessTokenPolarisHttpClientTestIT.ENV_POLARIS_URL);
        String accessToken = System.getenv(AccessTokenPolarisHttpClientTestIT.ENV_POLARIS_ACCESS_TOKEN);

        assumeTrue(StringUtils.isNotBlank(baseUrl));
        assumeTrue(StringUtils.isNotBlank(accessToken));

        Gson gson = new Gson();
        IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO);
        AccessTokenPolarisHttpClient httpClient = new AccessTokenPolarisHttpClient(logger, 100, true, ProxyInfo.NO_PROXY_INFO, new HttpUrl(baseUrl), accessToken, gson, urlSupport, new AuthenticationSupport(urlSupport));

        PolarisService polarisService = new PolarisService(httpClient, new PolarisJsonTransformer(gson, logger), PolarisRequestFactory.DEFAULT_LIMIT);

        String requestUri = baseUrl + "/api/common/v0/branches";
        Request request = PolarisRequestFactory.createDefaultCommonPolarisPagedGetRequest(requestUri);

        PolarisPagedResourceResponse<BranchV0Resource> branchV0Resources = polarisService.get(BranchV0Resources.class, request);
        List<BranchV0Resource> branchV0ResourceList = branchV0Resources.getData();
        assertNotNull(branchV0ResourceList);
        PolarisPaginationMeta meta = branchV0Resources.getMeta();
        assertNotNull(meta);
    }
 */

    // TODO: Replace with an api we use
    /*
       @ParameterizedTest
       @MethodSource("createGetAllMockData")
       public void testGetAll(Map<String, String> offsetsToResults, int expectedTotal) throws IntegrationException {
           HttpUrl requestUri = urlSupport.appendRelativeUrl(BASE_URL, PolarisService.PROJECT_API_SPEC);

           AccessTokenPolarisHttpClient polarisHttpClient = Mockito.mock(AccessTokenPolarisHttpClient.class);
           mockClientBehavior(polarisHttpClient, requestUri, offsetsToResults, "projects_no_more_results.json");

           PolarisJsonTransformer polarisJsonTransformer = new PolarisJsonTransformer(PolarisServicesFactory.createDefaultGson(), new PrintStreamIntLogger(System.out, LogLevel.INFO));

           PolarisPagedRequestCreator requestCreator = (limit, offset) -> PolarisRequestFactory.createDefaultPagedRequestBuilder(limit, offset)
                                                                              .url(requestUri)
                                                                              .build();

           PolarisPagedRequestWrapper pagedRequestWrapper = new PolarisPagedRequestWrapper(requestCreator, ProjectV0Resources.class);

           PolarisService polarisService = new PolarisService(polarisHttpClient, polarisJsonTransformer, PolarisRequestFactory.DEFAULT_LIMIT);
           try {
               List<ProjectV0Resource> allPagesResponse = polarisService.getAllResponses(pagedRequestWrapper);
               assertEquals(expectedTotal, allPagesResponse.size());
           } catch (IntegrationException e) {
               fail("Mocked response caused PolarisService::GetAllResponses to throw an unexpected IntegrationException, which should never happen in this test.", e);
           }
       }
   */
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
