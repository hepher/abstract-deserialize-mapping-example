package org.deserialize.test;

import org.deserialize.dbconnection.GenericService;
import org.deserialize.dbconnection.SimpleEntity;
import org.deserialize.dbconnection.SimpleEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExampleController {

    @Autowired
    private GenericService genericService;

    @PostMapping("test/json/object")
    public ResponseEntity<Test> test(@RequestBody Test test) {
        return new ResponseEntity<>(test, HttpStatus.OK);
    }

    @PostMapping("find/session-factory-from-jpa/simpledto")
    public ResponseEntity<List<SimpleEntity>> findAllSessionFactoryFromJPA() {
        return new ResponseEntity<>(genericService.findAllSessionFactoryFromJPA(), HttpStatus.OK);
    }

    @PostMapping("find/jpa/simpledto")
    public ResponseEntity<List<SimpleEntity>> findAllJpaSimpleDTO() {
        return new ResponseEntity<>(genericService.findAllJpaSimpleDTO(), HttpStatus.OK);
    }

    @PostMapping("find/jpa-native-query/simpledto")
    public ResponseEntity<List<SimpleEntity>> getAllNativeQueryJPA() {
        return new ResponseEntity<>(genericService.getAllNativeQueryJPA(), HttpStatus.OK);
    }

    @PostMapping("find/session-factory/simpledto")
    public ResponseEntity<List<SimpleEntity>> findAllSessionFactory() {
        return new ResponseEntity<>(genericService.findAllSessionFactory(), HttpStatus.OK);
    }

    @PostMapping("update/account")
    public ResponseEntity<String> updateAccount() {
        genericService.updateAccount();
        return new ResponseEntity<>("operazione ok", HttpStatus.OK);
    }
}
