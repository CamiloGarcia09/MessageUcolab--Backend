package co.edu.uco.application.common.catalog.strategy;

public abstract class MessageCatalog<K, T> {
    public abstract T getMessageById(K code);
    public abstract String getContent(String code);
    public abstract boolean isExist(K key);
}