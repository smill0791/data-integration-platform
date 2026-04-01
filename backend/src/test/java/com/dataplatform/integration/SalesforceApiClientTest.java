package com.dataplatform.integration;

import com.dataplatform.dto.SalesforceContact;
import com.dataplatform.dto.SalesforceQueryResult;
import com.dataplatform.exception.IntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesforceApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SalesforceAuthService authService;

    private SalesforceApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new SalesforceApiClient(restTemplate, authService, "v59.0", 3);
    }

    @Test
    void fetchContacts_singlePage_returnsContacts() {
        when(authService.getInstanceUrl()).thenReturn("https://myorg.my.salesforce.com");
        when(authService.getAccessToken()).thenReturn("test-token");

        SalesforceContact contact = SalesforceContact.builder()
                .id("003ABC").firstName("Alice").lastName("Johnson").email("alice@test.com").build();
        SalesforceQueryResult result = SalesforceQueryResult.builder()
                .totalSize(1).done(true).records(List.of(contact)).build();

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(SalesforceQueryResult.class)))
                .thenReturn(new ResponseEntity<>(result, HttpStatus.OK));

        List<SalesforceContact> contacts = apiClient.fetchContacts();

        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).getFirstName()).isEqualTo("Alice");
    }

    @Test
    void fetchContacts_multiplePages_followsNextRecordsUrl() {
        when(authService.getInstanceUrl()).thenReturn("https://myorg.my.salesforce.com");
        when(authService.getAccessToken()).thenReturn("test-token");

        SalesforceContact c1 = SalesforceContact.builder().id("001").firstName("Alice").lastName("A").build();
        SalesforceContact c2 = SalesforceContact.builder().id("002").firstName("Bob").lastName("B").build();

        SalesforceQueryResult page1 = SalesforceQueryResult.builder()
                .totalSize(2).done(false).nextRecordsUrl("/services/data/v59.0/query/next-page")
                .records(List.of(c1)).build();
        SalesforceQueryResult page2 = SalesforceQueryResult.builder()
                .totalSize(2).done(true).records(List.of(c2)).build();

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(SalesforceQueryResult.class)))
                .thenReturn(new ResponseEntity<>(page1, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(page2, HttpStatus.OK));

        List<SalesforceContact> contacts = apiClient.fetchContacts();

        assertThat(contacts).hasSize(2);
        verify(restTemplate, times(2)).exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(SalesforceQueryResult.class));
    }

    @Test
    void fetchContacts_on401_refreshesTokenAndRetries() {
        when(authService.getInstanceUrl()).thenReturn("https://myorg.my.salesforce.com");
        when(authService.getAccessToken()).thenReturn("old-token", "new-token");

        SalesforceContact contact = SalesforceContact.builder().id("001").firstName("Test").lastName("User").build();
        SalesforceQueryResult result = SalesforceQueryResult.builder()
                .totalSize(1).done(true).records(List.of(contact)).build();

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(SalesforceQueryResult.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null))
                .thenReturn(new ResponseEntity<>(result, HttpStatus.OK));
        when(authService.refreshToken()).thenReturn("new-token");

        List<SalesforceContact> contacts = apiClient.fetchContacts();

        assertThat(contacts).hasSize(1);
        verify(authService).refreshToken();
    }

    @Test
    void fetchContacts_afterMaxRetries_throwsIntegrationException() {
        when(authService.getInstanceUrl()).thenReturn("https://myorg.my.salesforce.com");
        when(authService.getAccessToken()).thenReturn("test-token");

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(SalesforceQueryResult.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        assertThatThrownBy(() -> apiClient.fetchContacts())
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Failed to fetch Salesforce contacts after 3 attempts");
    }

    @Test
    void fetchContacts_nullResult_returnsEmptyList() {
        when(authService.getInstanceUrl()).thenReturn("https://myorg.my.salesforce.com");
        when(authService.getAccessToken()).thenReturn("test-token");

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(SalesforceQueryResult.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<SalesforceContact> contacts = apiClient.fetchContacts();

        assertThat(contacts).isEmpty();
    }
}
