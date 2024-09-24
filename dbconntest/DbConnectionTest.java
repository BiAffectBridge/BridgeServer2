import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectionTest {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://db:3306/bridgeserverdb";
        String username = "bridgeserver";
        String password = "bridgeserverpassword";

        try {
            System.out.println("Connecting to database...");
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            System.out.println("Connection successful!");
            connection.close();
        } catch (SQLException e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }
}
