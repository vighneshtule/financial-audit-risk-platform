package config;

public class DatabaseConfig {

        public static final String URL = System.getenv("DB_URL");

        public static final String USER = System.getenv("DB_USERNAME");

        public static final String PASSWORD = System.getenv("DB_PASSWORD");
}