package org.deserialize.dbconnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SimpleEntityService {

    @Autowired
    private SimpleEntityRepository dao;


}
