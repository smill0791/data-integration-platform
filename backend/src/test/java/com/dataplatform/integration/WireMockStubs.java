package com.dataplatform.integration;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.PaginatedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockStubs {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int PAGE_SIZE = 5;

    public static void stubCustomers(WireMockServer server, List<CrmCustomerResponse> customers) {
        stubPaginatedEndpoint(server, "/api/customers", customers);
    }

    public static void stubCustomersFailure(WireMockServer server) {
        server.stubFor(WireMock.get(urlPathEqualTo("/api/customers"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
    }

    public static void stubProducts(WireMockServer server, List<ErpProductResponse> products) {
        stubPaginatedEndpoint(server, "/api/products", products);
    }

    public static void stubProductsFailure(WireMockServer server) {
        server.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
    }

    public static void stubInvoices(WireMockServer server, List<AccountingInvoiceResponse> invoices) {
        stubPaginatedEndpoint(server, "/api/invoices", invoices);
    }

    public static void stubInvoicesFailure(WireMockServer server) {
        server.stubFor(WireMock.get(urlPathEqualTo("/api/invoices"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));
    }

    private static <T> void stubPaginatedEndpoint(WireMockServer server, String path, List<T> items) {
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;

        for (int page = 0; page < totalPages; page++) {
            int fromIndex = page * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalElements);
            List<T> pageContent = items.subList(fromIndex, toIndex);

            PaginatedResponse<T> response = new PaginatedResponse<>(
                    pageContent, page, PAGE_SIZE, totalElements, totalPages
            );

            try {
                String json = objectMapper.writeValueAsString(response);
                server.stubFor(WireMock.get(urlPathEqualTo(path))
                        .withQueryParam("page", equalTo(String.valueOf(page)))
                        .withQueryParam("size", equalTo(String.valueOf(PAGE_SIZE)))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(json)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize stub response", e);
            }
        }
    }

    // Test data factory methods

    public static CrmCustomerResponse createCustomer(String id, String name, String email, String phone) {
        return CrmCustomerResponse.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .address(CrmCustomerResponse.Address.builder()
                        .street("123 Main St")
                        .city("Springfield")
                        .state("IL")
                        .zipCode("62701")
                        .build())
                .lastUpdated("2024-01-15T10:00:00")
                .build();
    }

    public static ErpProductResponse createProduct(String id, String sku, String name, double price, int quantity) {
        return ErpProductResponse.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description("Test product description")
                .category("Electronics")
                .unitPrice(price)
                .quantity(quantity)
                .warehouse("WH-001")
                .build();
    }

    public static AccountingInvoiceResponse createInvoice(String id, String invoiceNumber,
                                                           String customerName, double amount,
                                                           String currency, String status) {
        return AccountingInvoiceResponse.builder()
                .id(id)
                .invoiceNumber(invoiceNumber)
                .customerName(customerName)
                .amount(amount)
                .currency(currency)
                .status(status)
                .dueDate("2024-06-15")
                .lineItems(List.of())
                .build();
    }
}
