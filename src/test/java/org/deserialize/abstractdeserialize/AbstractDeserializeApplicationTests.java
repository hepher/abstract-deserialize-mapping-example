package org.deserialize.abstractdeserialize;

import org.deserialize.test.JUnitTest;
import org.deserialize.test.JUnitTestBean;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class AbstractDeserializeApplicationTests {

	JUnitTest test;

	@BeforeClass
	public static void beforeClass() {
//		test = new JUnitTest();
//		test.getListExample().forEach(elem -> test.add(elem));
//		System.out.println(test.getAll().size());
	}

	@Before
	public void configureList() {
		test = new JUnitTest();
		test.getListExample().forEach(elem -> test.add(elem));
		System.out.println("fille list");
	}

	@Test
	public void testDefaultElements() {
		System.out.println(test.getAll().size());
		List<JUnitTestBean> list = test.getListExample();
		assertEquals(3, list.size());
	}

	@Test
	public void testAddElements() {
		System.out.println(test.getAll().size());
		test.add("X", "picolla", 44);
		assertEquals(1, test.getAll().size());
	}

	@Test
	public void testRemoveElements() {
		System.out.println(test.getAll().size());
		test.add("X", "dsadsa", 11);
		test.remove("X");
		assertEquals(1, test.getAll().size());
	}

}
