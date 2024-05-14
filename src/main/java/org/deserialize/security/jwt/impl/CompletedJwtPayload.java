package com.enelx.bfw.framework.security.jwt.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletedJwtPayload extends JwtPayload {

    @JsonProperty("uniqueid")
    private String uniqueId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("guest")
    private Boolean guest;

    @JsonProperty("family_name")
    private String lastname;

    @JsonProperty("given_name")
    private String firstname;

    @JsonProperty("country")
    private String country;

    @JsonProperty("language")
    private String language;

    @JsonProperty("deviceid")
    private String deviceId;

    @JsonProperty("create_datetime")
    private Date createDatetime;

    @JsonProperty("update_datetime")
    private Date updateDatetime;
}
