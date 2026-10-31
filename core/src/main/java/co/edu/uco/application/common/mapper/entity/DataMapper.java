package co.edu.uco.application.common.mapper.entity;

public interface DataMapper<E,D,T> {
    D mapperDomain(E entity);
    E mapperData(D data);
    T mapperDTO(E entity);
}