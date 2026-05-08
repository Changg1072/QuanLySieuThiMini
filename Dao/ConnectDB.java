package Dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDB {
    public static Connection con = null;
    public static ConnectDB instance = new ConnectDB();

    private ConnectDB() {
        connect(); 
    }

    public static ConnectDB getInstance() {
        return instance;
    }

    public void connect() {
        String url = "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=PBL3;encrypt=true;trustServerCertificate=true;";
        
        String user = "sa"; 
        String password = "190496"; 

        try {
            con = DriverManager.getConnection(url, user, password);
            System.out.println("Chúc mừng! Đã kết nối Database PBL3 thành công!");
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối rồi: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return con;
    }
    
    public static void main(String[] args) {
        ConnectDB.getInstance().connect();
    }
}