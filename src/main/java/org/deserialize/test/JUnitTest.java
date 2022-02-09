package org.deserialize.test;

import java.util.ArrayList;
import java.util.List;

public class JUnitTest {

    private List<JUnitTestBean> list = new ArrayList<>();

    public List<JUnitTestBean> getAll() {
        return list;
    }

    public void add(JUnitTestBean testBean) {
        list.add(testBean);
    }

    public void add(String id, String name, Integer age) {
        list.add(new JUnitTestBean(id, name, age));
    }

    public void remove(String id) {
        // do nothing for now
    }

    public List<JUnitTestBean> getListExample() {
        List<JUnitTestBean> result = new ArrayList<>();
        result.add(new JUnitTestBean("1", "pluto", 33));
        result.add(new JUnitTestBean("2", "paperino", 22));
        result.add(new JUnitTestBean("3", "pippo", 44));

        return result;
    }

}
