package com.dataplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceContact {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Phone")
    private String phone;

    @JsonProperty("MailingStreet")
    private String mailingStreet;

    @JsonProperty("MailingCity")
    private String mailingCity;

    @JsonProperty("MailingState")
    private String mailingState;

    @JsonProperty("MailingPostalCode")
    private String mailingPostalCode;

    @JsonProperty("LastModifiedDate")
    private String lastModifiedDate;
}
