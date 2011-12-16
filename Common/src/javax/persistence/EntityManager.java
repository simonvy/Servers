package javax.persistence;

public interface EntityManager {
	
	void persist(Object entity);
	void remove(Object entity);
	void refresh(Object entity);
	<T> T find(Class<T> entityClass, Object entityId);
	
	void close();
	boolean isOpen();
}
