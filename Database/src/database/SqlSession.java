package database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

public class SqlSession implements EntityManager {

	private SqlSessionFactory factory;
	
	public SqlSession(SqlSessionFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public <T> T find(Class<T> entityClass, Object oid) {
		return null;
	}
	
	@Override
	public void remove(Object entity) {
		
	}
	
	public void update(Object entity) {
		
	}
	
	@Override
	public void refresh(Object entity) {
		
	}
	
	@Override
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
		for (String column : meta.getColumns()) {
			if (!meta.isGeneratedColumn(column)) {
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
				parameterize(stmt, valueObjects.toArray());
				stmt.execute();
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public <T> List<T> find(String namedQuery, Class<T> entityClass, Object...params) {
		List<T> result = new ArrayList<T>();
		EntityMeta meta = factory.getEntityMeta(entityClass);
		
		try {
			PreparedStatement stmt = factory.getNamedStatement(namedQuery);
			parameterize(stmt, params);
			try {
				ResultSet rs = stmt.executeQuery();
				ResultSetMetaData rsMeta = rs.getMetaData();
				while (rs.next()) {
					T host = entityClass.newInstance();
					for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
						String column = rsMeta.getColumnName(i);
						if (meta.hasColumn(column)) {
							Object value = rs.getObject(i);
							int columnType = rsMeta.getColumnType(i);
							Class<?> expectedType = meta.getColumnType(column);
							value = castValue(value, columnType, expectedType);
							meta.setValue(host, column, value);
						}
					}
					result.add(host);
				}
			} finally {
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
	
	private void parameterize(PreparedStatement stmt, Object[] params) throws SQLException {
		for (int i = 1; i <= params.length; i++) {
			Object param = params[i - 1];
			if (param instanceof Integer) {
				stmt.setInt(i, (Integer)param);
			} else if (param instanceof String) {
				stmt.setString(i, (String)param);
			} else if (param == null) {
				stmt.setNull(i, Types.OTHER);
			} else {
				System.err.println("unknown parameter: " + param);
			}
		}
	}
	
	private Object castValue(Object value, int type, Class<?> expectedType) {
		switch(type) {
		case Types.NULL:
			return null;
		case Types.INTEGER:
			if (value instanceof Long && expectedType == Integer.class) {
				value = ((Long)value).intValue();
			}
			return value;
		default:
			return value;
		}
	}
	
	@Override
	public void close() {
		this.factory = null;
	}

	@Override
	public boolean isOpen() {
		return this.factory != null;
	}
}
