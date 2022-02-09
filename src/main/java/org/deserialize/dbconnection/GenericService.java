package org.deserialize.dbconnection;

import org.deserialize.main.AbstractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenericService extends AbstractService<SimpleEntity, Integer, GenericDAO, SimpleEntityDAO> {

    @Transactional(readOnly = true)
    public List<SimpleEntity> findAllSessionFactoryFromJPA() {
        return dao.findAllSessionFactoryFromJPA();
    }

    @Transactional(readOnly = true)
    public List<SimpleEntity> findAllSessionFactory() {
        return dao.findAllSessionFactory();
    }

    @Transactional(readOnly = true)
    public List<SimpleEntity> getAllNativeQueryJPA() {
        return dao.getAllNativeQueryJPA();
    }

    @Transactional
    public void updateAccount() {
        SimpleEntity entity = new SimpleEntity();
        entity.setId(1);
        entity.setName("admin 2");
        entity.setOwner("EADMIN");
        jpaDao.save(entity);

        dao.updateAccount();

//        throw new RuntimeException("bla bla bla");
    }

    public List<SimpleEntity> findAllJpaSimpleDTO() {
        return jpaDao.findAll();
    }
}
