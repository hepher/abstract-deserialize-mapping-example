package org.deserialize.config;

//import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
//@EnableAdminServer
//@EnableJpaRepositories(basePackages="org.deserialize.dbconnection")
@EnableAutoConfiguration(exclude = HibernateJpaAutoConfiguration.class)
public class BeanConfiguration {

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);
        return factory;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory, DataSource dataSource) {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory);
        txManager.setDataSource(dataSource);
        return txManager;
    }

}