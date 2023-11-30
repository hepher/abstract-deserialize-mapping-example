package com.enelx.bfw.framework.resolver;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationDetail {

    private String name;
    private boolean required;
    private ParameterType in;
    private String description;
    private String example;

    public OperationDetail(String name) {
        assert name != null;
        this.name = name;
        this.required = false;
    }

    public OperationDetail(String name, boolean required, ParameterType in, String description, String example) {
        this(name);
        this.required = required;
        this.in = in;
        this.description = description;
        this.example = example;
    }
}
