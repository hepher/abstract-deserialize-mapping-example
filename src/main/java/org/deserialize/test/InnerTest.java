package org.deserialize.test;

public class InnerTest {

    private String id;
    private String name;
    private Boolean myBoolean;
    private InnerTest inner;

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

    public InnerTest getInner() {
        return inner;
    }

    public void setInner(InnerTest inner) {
        this.inner = inner;
    }

    public Boolean getMyBoolean() {
        return myBoolean;
    }

    public void setMyBoolean(Boolean myBoolean) {
        this.myBoolean = myBoolean;
    }
}
