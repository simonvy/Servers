package database;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntityMeta {

	private Class<?> clazz;
	private String table;
	private Map<String, Field> columns;
	private Map<String, Method> setters;
	private Map<String, Method> getters;
	
	public EntityMeta(Class<?> entityClass) {
		this.clazz = entityClass;
		this.columns = new HashMap<String, Field>();
		this.setters = new HashMap<String, Method>();
		this.getters = new HashMap<String, Method>();
	}

	public void setTable(String table) {
		this.table = table;
	}
	
	public String getTable() {
		return this.table;
	}
	
	public void addColumn(String columnName, Field field) {
		if (columns.containsKey(columnName)) {
			throw new IllegalStateException("duplicate " + columnName + " " + clazz.getName());
		}
		columns.put(columnName, field);
	}
	
	public Collection<String> getColumn() {
		return columns.keySet();
	}
	
	public Field getColumnField(String columnName) {
		return columns.get(columnName);
	}
	
	public void setValue(Object host, String column, Object value) {
		if (!setters.containsKey(column)) {
			String setterName = "set" + column.substring(0, 1).toUpperCase() + column.substring(1);
			try {
				Method setter = clazz.getMethod(setterName, value.getClass());
				setters.put(column, setter);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		Method setter = setters.get(column);
		try {
			setter.invoke(host, value);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public Object getValue(Object host, String column) {
		if (!getters.containsKey(column)) {
			String getterName = "get" + column.substring(0, 1).toUpperCase() + column.substring(1);
			try {
				Method getter = clazz.getMethod(getterName);
				getters.put(column, getter);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		Method getter = getters.get(column);
		try {
			return getter.invoke(host);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
