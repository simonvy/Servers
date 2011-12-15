package database;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SqlSessionFactory {

	private Properties properties = new Properties();
	private Map<String, String> namedQueries = new HashMap<String, String>();
	
	private Map<Class<?>, EntityMeta> entityMeta = new HashMap<Class<?>, EntityMeta>();
	private Connection connection;
	
	public SqlSession openSession() {
		if (connection == null) {
			connect();
		}
		return new SqlSession(this);
	}
	
	private void connect() {
		String driver = properties.getProperty("driver");
		String url = properties.getProperty("url");
		String user = properties.getProperty("user");
		String password = properties.getProperty("password");
		
		try {
			Class.forName(driver);
			this.connection = DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void close() throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}
	
	public EntityMeta getEntityMeta(Class<?> entityClass) {
		if (!entityMeta.containsKey(entityClass)) {
			EntityMeta meta = new EntityMeta(entityClass);
			
			Table table = entityClass.getAnnotation(Table.class);
			meta.setTable(table.name());	
			Field[] fields = entityClass.getDeclaredFields();
			for (Field field : fields) {
				Column column = field.getAnnotation(Column.class);
				if (column != null) {
					String columnName = column.name();
					if (columnName.length() == 0) {
						columnName = field.getName();
					}
					meta.addColumn(columnName, field);
				}
			}
			
			entityMeta.put(entityClass, meta);
		}
		return entityMeta.get(entityClass);
	}
	
	public PreparedStatement getStatement(String queryName) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement(namedQueries.get(queryName));
		return stmt;
	}
	
	public PreparedStatement getSqlStatement(String sql) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement(sql);
		return stmt;
	}
	
	public void setupDefault() {
		ClassLoader loader = SqlSessionFactory.class.getClassLoader();
		InputStream properties = loader.getResourceAsStream("META-INF/persistence.xml");
		try {
			setup(properties, "default");
		} finally {
			if (properties != null) {
				try {
					properties.close();
				} catch(Exception e) {// swallowed
				}
			}
		}
	}
	
	private void setup(InputStream input, String unit) {
		Document document = null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = builder.parse(input);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		NodeList units = document.getElementsByTagName("persistence-unit");
		Element targetUnit = null;
		for (int i = 0; i < units.getLength(); i++) {
			Element u = (Element)units.item(i);

			String name = u.getAttributes().getNamedItem("name").getTextContent();
			if (unit.equals(name)) {
				targetUnit = u;
				break;
			}
		}
				
		NodeList queries = targetUnit.getElementsByTagName("query");
		for (int i = 0; i < queries.getLength(); i++) {
			Element u = (Element)queries.item(i);
			String key = u.getAttributes().getNamedItem("name").getTextContent();
			String value = u.getTextContent().trim();
			namedQueries.put(key, value);
		}
		
		NodeList props = targetUnit.getElementsByTagName("property");
		for (int i = 0; i < props.getLength(); i++) {
			Element u = (Element)props.item(i);
			String key = u.getAttributes().getNamedItem("name").getTextContent();
			String value = u.getAttributes().getNamedItem("value").getTextContent();
			properties.setProperty(key, value);
		}
	}
}
