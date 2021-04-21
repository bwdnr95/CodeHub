package chat10;

public interface IConnect {
	
	String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	String ORACLE_URL = "jdbc:oracle:thin://@localhost:1521:xe";
	
	void connect(String user, String pass);
	void execute();
	void close();

	String scanValue(String title);

}
