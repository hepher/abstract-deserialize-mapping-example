package org.deserialize.test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@JsonDeserialize(using = TestDeserialize.class)
public class Test {

    private UUID uuid;
    private String id;
    private String name;
    private Integer littleNumber;
    private Double doubleNumber;
    private Float littleFloat;
    private InnerTest innerTest;
    private InnerTest innerTest2;
    private Date date;
    private Date longDate;
    private Instant instant = Instant.now();
    private LocalDate localDate;
    private LocalDateTime localDateTime;
    private ZonedDateTime zonedDateTime;
    private Integer[] vetInteger;
    private String[] vetString;
    private ArrayContainer arrayContainer;
    private ArrayObject[] arrayObjects;
    private LinkedList<Integer> listInteger;
    private ArrayList<Long> arrayListInteger;
    private Set<ArrayObject> setArrayObject;
    private Set<Integer> setInteger;

    private List<List<Integer>> listOfLists;
    private Integer[][] arrayOfArrayInteger;

    private String unmappedProperty;
    private Integer unmappedPropertyInteger;
    private UnmappedObject unmappedObject;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

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

    public Integer getLittleNumber() {
        return littleNumber;
    }

    public void setLittleNumber(Integer littleNumber) {
        this.littleNumber = littleNumber;
    }

    public Double getDoubleNumber() {
        return doubleNumber;
    }

    public void setDoubleNumber(Double doubleNumber) {
        this.doubleNumber = doubleNumber;
    }

    public Float getLittleFloat() {
        return littleFloat;
    }

    public void setLittleFloat(Float littleFloat) {
        this.littleFloat = littleFloat;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getLongDate() {
        return longDate;
    }

    public void setLongDate(Date longDate) {
        this.longDate = longDate;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
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

    public Integer[] getVetInteger() {
        return vetInteger;
    }

    public void setVetInteger(Integer[] vetInteger) {
        this.vetInteger = vetInteger;
    }

    public String[] getVetString() {
        return vetString;
    }

    public void setVetString(String[] vetString) {
        this.vetString = vetString;
    }

    public ArrayContainer getArrayContainer() {
        return arrayContainer;
    }

    public void setArrayContainer(ArrayContainer arrayContainer) {
        this.arrayContainer = arrayContainer;
    }

    public ArrayObject[] getArrayObjects() {
        return arrayObjects;
    }

    public void setArrayObjects(ArrayObject[] arrayObjects) {
        this.arrayObjects = arrayObjects;
    }

    public LinkedList<Integer> getListInteger() {
        return listInteger;
    }

    public void setListInteger(LinkedList<Integer> listInteger) {
        this.listInteger = listInteger;
    }

    public ArrayList<Long> getArrayListInteger() {
        return arrayListInteger;
    }

    public void setArrayListInteger(ArrayList<Long> arrayListInteger) {
        this.arrayListInteger = arrayListInteger;
    }

    public Set<ArrayObject> getSetArrayObject() {
        return setArrayObject;
    }

    public void setSetArrayObject(Set<ArrayObject> setArrayObject) {
        this.setArrayObject = setArrayObject;
    }

    public Set<Integer> getSetInteger() {
        return setInteger;
    }

    public void setSetInteger(Set<Integer> setInteger) {
        this.setInteger = setInteger;
    }

    public List<List<Integer>> getListOfLists() {
        return listOfLists;
    }

    public void setListOfLists(List<List<Integer>> listOfLists) {
        this.listOfLists = listOfLists;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public String getUnmappedProperty() {
        return unmappedProperty;
    }

    public void setUnmappedProperty(String unmappedProperty) {
        this.unmappedProperty = unmappedProperty;
    }

    public Integer getUnmappedPropertyInteger() {
        return unmappedPropertyInteger;
    }

    public void setUnmappedPropertyInteger(Integer unmappedPropertyInteger) {
        this.unmappedPropertyInteger = unmappedPropertyInteger;
    }

    public UnmappedObject getUnmappedObject() {
        return unmappedObject;
    }

    public void setUnmappedObject(UnmappedObject unmappedObject) {
        this.unmappedObject = unmappedObject;
    }

    public Integer[][] getArrayOfArrayInteger() {
        return arrayOfArrayInteger;
    }

    public void setArrayOfArrayInteger(Integer[][] arrayOfArrayInteger) {
        this.arrayOfArrayInteger = arrayOfArrayInteger;
    }
}
