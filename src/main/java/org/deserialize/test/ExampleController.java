package org.deserialize.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

    @PostMapping("test/json/object")
    public ResponseEntity<Test> test(@RequestBody Test test) {
        return new ResponseEntity<>(test, HttpStatus.OK);
    }
}
