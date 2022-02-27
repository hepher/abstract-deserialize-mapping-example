package org.deserialize.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//public abstract class AbstractService<T, ID, DAO extends AbstractDAO, JpaDAO extends JpaRepository<T, ID>> {
public abstract class AbstractService<T, ID, DAO extends AbstractDAO> {

    @Autowired
    protected DAO dao;

//    @Transactional
//    protected <S extends T> T save(T entity) {
//        return jpaDao.save(entity);
//    }

//    @Transactional
//    protected <S extends T> T saveAndFlush(T entity) {
//        return jpaDao.saveAndFlush(entity);
//    }

//    @Transactional
//    protected <S extends T> List<T> saveAll(List<T> entities) {
//        return jpaDao.saveAll(entities);
//    }

//    @Transactional
//    protected <S extends T> List<T> saveAllAndFlush(List<T> entities) {
//        return jpaDao.saveAllAndFlush(entities);
//    }

//    @Transactional
//    protected void delete(T entity) {
//        jpaDao.delete(entity);
//    }
}
