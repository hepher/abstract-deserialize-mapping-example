package org.deserialize.main.jpa;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AbstractService<E, I, R extends JpaRepository<E, I>> {

    @PersistenceContext
    protected EntityManager entityManager;

    private Class<E> klass;
    private CriteriaBuilder criteriaBuilder;
    protected final R repository;

    private final static BiFunction<Root<?>, String, Path<Object>> computePathFunction = (entity, property) -> {
        Path<Object> path = null;
        for (String prop : property.split("\\.")) {
            path = path != null ? path.get(prop) : entity.get(prop);
        }
        return path;
    };

    protected AbstractService(R repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initialize() {
        klass = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    @Transactional
    public E save(E entity) {
        return repository.save(entity);
    }

    @Transactional
    public void delete(I entityId) {
        repository.deleteById(entityId);
    }

    @Transactional(readOnly = true)
    public List<E> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<E> findAll(Example<E> example) {
        return repository.findAll(example);
    }

    @Transactional(readOnly = true)
    public Optional<E> findById(I id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(I id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsBy(Example<E> example) {
        return repository.exists(example);
    }

    @Transactional(readOnly = true)
    public List<E> findByCriteria(SpecificationCriteria<E>... predicates) {
        return findByCriteria(Arrays.stream(predicates).collect(Collectors.toList()), null, null);
    }

    @Transactional(readOnly = true)
    public List<E> findByCriteria(List<SpecificationCriteria<E>> predicateList, List<SpecificationOrder<E>> orders, Map<String, JoinType> fetchMap) {
        CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(klass);

        Root<E> entity = criteriaQuery.from(klass);

        if (fetchMap != null && !fetchMap.isEmpty()) {
            fetchMap.forEach(entity::fetch);
        }

        criteriaQuery.select(entity);

        if (predicateList != null) {
            criteriaQuery.where(predicateList
                    .stream()
                    .filter(Objects::nonNull)
                    .map(predicateFunction -> predicateFunction.apply(entity, criteriaBuilder))
                    .toArray(Predicate[]::new)
            );
        }

        if (orders != null) {
            criteriaQuery.orderBy(orders
                    .stream()
                    .filter(Objects::nonNull)
                    .map(orderFunction -> orderFunction.apply(entity, criteriaBuilder))
                    .toArray(Order[]::new));
        }

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public static <T> SpecificationCriteria<T> createSpecificationCriteria(BiFunction<Root<T>, CriteriaBuilder, Predicate> criteriaBuilderFunction) {
        return new SpecificationCriteria<>(criteriaBuilderFunction);
    }

    public static <T> SpecificationCriteria<T> createEqualsCriteria(String property, Object value) {
        return new SpecificationCriteria<>((entity, builder) -> builder.equal(computePathFunction.apply(entity, property), value));
    }

    public static <T> SpecificationCriteria<T> createInCriteria(String property, List<Object> values) {
        return new SpecificationCriteria<>((entity, builder) -> computePathFunction.apply(entity, property).in(values));
    }

    public static <T> SpecificationCriteria<T> createNotInCriteria(String property, List<Object> values) {
        return new SpecificationCriteria<>((entity, builder) -> builder.not(computePathFunction.apply(entity, property).in(values)));
    }

    public static <T> SpecificationOrder<T> createSpecificationOrder(BiFunction<Root<T>, CriteriaBuilder, Order> orderBuilderFunction) {
        return new SpecificationOrder<>(orderBuilderFunction);
    }

    public static <T> SpecificationOrder<T> createAscOrder(String property) {
        return new SpecificationOrder<>((entity, builder) -> builder.asc(computePathFunction.apply(entity, property)));
    }

    public static <T> SpecificationOrder<T> createDescOrder(String property) {
        return new SpecificationOrder<>((entity, builder) -> builder.desc(computePathFunction.apply(entity, property)));
    }

    public static class SpecificationCriteria<T> {

        private final BiFunction<Root<T>, CriteriaBuilder, Predicate> criteriaBuilderFunction;

        public SpecificationCriteria(BiFunction<Root<T>, CriteriaBuilder, Predicate> criteriaBuilderFunction) {
            this.criteriaBuilderFunction = criteriaBuilderFunction;
        }

        public Predicate apply(Root<T> entityRoot, CriteriaBuilder builder) {
            return criteriaBuilderFunction.apply(entityRoot, builder);
        }
    }

    public static class SpecificationOrder<T> {

        private final BiFunction<Root<T>, CriteriaBuilder, Order> criteriaBuilderFunction;

        public SpecificationOrder(BiFunction<Root<T>, CriteriaBuilder, Order> criteriaBuilderFunction) {
            this.criteriaBuilderFunction = criteriaBuilderFunction;
        }

        public Order apply(Root<T> entityRoot, CriteriaBuilder builder) {
            return criteriaBuilderFunction.apply(entityRoot, builder);
        }
    }
}
