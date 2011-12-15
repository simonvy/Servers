package database;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.GeneratedValue;

public class SqlSession {

	private SqlSessionFactory factory;
	
	public SqlSession(SqlSessionFactory factory) {
		this.factory = factory;
	}
	
	public <T> List<T> find(String queryName, Class<T> entityClass, Object... params) {
		List<T> result = new ArrayList<T>();
		EntityMeta meta = factory.getEntityMeta(entityClass);
		
		try {
			PreparedStatement stmt = factory.getNamedStatement(queryName);
			prepareParameters(stmt, params);
			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				while (rs.next()) {
					T host = entityClass.newInstance();
					for (String column : meta.getColumn()) {
						Class<?> type = meta.getColumnField(column).getType();
						Object value = getProperValue(rs, column, type);
						meta.setValue(host, column, value);
					}
					result.add(host);
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace(System.err);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return result;
	}
	
	public <T> T findOne(String queryName, Class<T> entityClass, Object... params) {
		List<T> objects = find(queryName, entityClass, params);
		if (objects != null && objects.size() > 0) {
			return objects.get(0);
		}
		return null;
	}
	
	public void persist(Object entity) {
		Class<?> entityClass = entity.getClass();
		EntityMeta meta = factory.getEntityMeta(entityClass);

		List<Object> valueObjects = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO ");
		sql.append(meta.getTable());
		sql.append("(");
		String values = "";
		
		boolean first = true;
		for (String column : meta.getColumn()) {
			Field field = meta.getColumnField(column);
			if (!field.isAnnotationPresent(GeneratedValue.class)) {
				if (!first) {
					sql.append(", ");
					values += ", ";
				}
				first = false;
				sql.append(column);
				valueObjects.add(meta.getValue(entity, column));
				values += "?";
			}
		}
		sql.append(") VALUES (");
		sql.append(values);
		sql.append(")");
		
		try {
			PreparedStatement stmt = factory.getSqlStatement(sql.toString());
			try {
				prepareParameters(stmt, valueObjects.toArray());
				stmt.execute();
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private static void prepareParameters(PreparedStatement stmt, Object[] params) throws SQLException {
		for (int i = 1; i <= params.length; i++) {
			Object param = params[i - 1];
			if (param instanceof Integer) {
				stmt.setInt(i, (Integer)param);
			} else if (param instanceof String) {
				stmt.setString(i, (String)param);
			} else if (param instanceof Double) {
				stmt.setDouble(i, (Double)param);
			} else if (param instanceof Date) {
				stmt.setDate(i, (Date)param);
			} else {
				System.err.println("unknown parameter: " + param);
			}
		}
	}
	
	private static Object getProperValue(ResultSet rs, String column, Class<?> type) throws SQLException {
		if (type == int.class || type == Integer.class) {
			return rs.getInt(column);
		} else if (type == String.class) {
			return rs.getString(column);
		} else if (type == double.class || type == Double.class) {
			return rs.getDouble(column);
		} else if (type == Date.class) {
			return rs.getDate(column);
		} else {
			System.err.println("unknown column type: " + type);
		}
		return null;
	}
	
	public void close() {
		this.factory = null;
	}
}
