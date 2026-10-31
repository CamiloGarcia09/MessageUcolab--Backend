package co.edu.uco.application.common.mapper.dto;

public interface DTOMapper<T,D> {
    D mapperDomain(T dto);
    T mapperDTO(D domain);
}