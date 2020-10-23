package org.deserialize.test;

import java.util.List;

public class ArrayContainer {

    private String[] vetString;
    private ArrayObject[] arrayObjects;
    private List<ArrayObject> listObject;

    public String[] getVetString() {
        return vetString;
    }

    public void setVetString(String[] vetString) {
        this.vetString = vetString;
    }

    public ArrayObject[] getArrayObjects() {
        return arrayObjects;
    }

    public void setArrayObjects(ArrayObject[] arrayObjects) {
        this.arrayObjects = arrayObjects;
    }

    public List<ArrayObject> getListObject() {
        return listObject;
    }

    public void setListObject(List<ArrayObject> listObject) {
        this.listObject = listObject;
    }
}
