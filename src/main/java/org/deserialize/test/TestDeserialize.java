package org.deserialize.test;

import org.deserialize.abstractdeserialize.AbstractDeserializeMapping;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TestDeserialize extends AbstractDeserializeMapping<Test> {

    public TestDeserialize() throws IOException {
        super(Test.class, null, null);
    }
}
