package org.deserialize.test;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class Test2 {

    private UUID uuid;
    private Date date;
    private String id;
    private String name;
    private Integer integer;
    private Long longer;
    private LocalDateTime localDateTime;
    private ZonedDateTime zonedDateTime;
    private Instant instant;
    private BigInteger bigInteger;
    private InnerTest innerTest;
    private InnerTest innerTest2;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InnerTest getInnerTest() {
        return innerTest;
    }

    public void setInnerTest(InnerTest innerTest) {
        this.innerTest = innerTest;
    }

    public InnerTest getInnerTest2() {
        return innerTest2;
    }

    public void setInnerTest2(InnerTest innerTest2) {
        this.innerTest2 = innerTest2;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public Long getLonger() {
        return longer;
    }

    public void setLonger(Long longer) {
        this.longer = longer;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }
}
