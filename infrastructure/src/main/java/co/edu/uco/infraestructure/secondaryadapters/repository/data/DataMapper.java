package co.edu.uco.infraestructure.secondaryadapters.repository.data;

public interface DataMapper <D, A> {
    D mapperData(A model);
    A mapperModel(D data);
}