package org.deserialize.test;

import com.deserialize.mapping.main.AbstractDeserializeMapping;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
public class TestDeserialize extends AbstractDeserializeMapping<Test> {

    public TestDeserialize() throws IOException, URISyntaxException {
        super(Test.class, null, null);
    }
}
