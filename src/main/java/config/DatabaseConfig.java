package config;

public class DatabaseConfig {

    public static final String URL = System.getenv("DB_URL") != null
            ? System.getenv("DB_URL")
            : "jdbc:postgresql://localhost:5432/financial_audit";

    public static final String USER = System.getenv("DB_USERNAME") != null
            ? System.getenv("DB_USERNAME")
            : "postgres";

    public static final String PASSWORD = System.getenv("DB_PASSWORD") != null
            ? System.getenv("DB_PASSWORD")
            : "postgres";
}