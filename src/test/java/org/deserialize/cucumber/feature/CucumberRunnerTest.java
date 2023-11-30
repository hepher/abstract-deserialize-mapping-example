import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.annotation.DirtiesContext;

@Suite
@IncludeEngines("cucumber")
//@DataMongoTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CucumberRunnerTest {

//    @Autowired
//    private UserService userService;

    @BeforeAll
    public static void beforeAll() {
//        User user = new User();
//
//        user.setId(UUID.randomUUID().toString());
//        user.setEmail("prova.prova@gmail.com");
//        user.setPhone("8889998877");
//
//        userService.save(user);
    }
}
