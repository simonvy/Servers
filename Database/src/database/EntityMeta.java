package database;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

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
		
		setTable(this.clazz);
		addColumns(this.clazz);
	}

	private void setTable(Class<?> clazz) {
		Table t = clazz.getAnnotation(Table.class);
		if (t == null) {
			throw new IllegalStateException("entity is not mapped with table.");
		}
		table = t.name();
	}
	
	private void addColumns(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			Column c = field.getAnnotation(Column.class);
			if (c == null) {
				continue;
			}
			String columnName = c.name();
			if (c.name().length() == 0) {
				columnName = field.getName();
			}
			if (columns.containsKey(columnName)) {
				throw new IllegalStateException("duplicate " + columnName + " " + clazz.getName());
			}
			columns.put(columnName, field);
		}
	}
	
	public String getTable() {
		return this.table;
	}
	
	public Collection<String> getColumns() {
		return columns.keySet();
	}
	
	public Class<?> getColumnType(String column) {
		if (!columns.containsKey(column)) {
			return null;
		}
		return columns.get(column).getType();
	}
	
	public boolean isGeneratedColumn(String column) {
		if (!columns.containsKey(column)) {
			return true;
		}
		return columns.get(column).getAnnotation(GeneratedValue.class) != null;
	}
	
	public void setValue(Object host, String column, Object value) {
		if (!columns.containsKey(column)) {
			return;
		}
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
		if (!columns.containsKey(column)) {
			return null;
		}
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
