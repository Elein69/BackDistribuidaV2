package mycompany.agendasimplebackend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleServer {

    // Configuración desde variables de entorno
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String FRONTEND_URL = System.getenv("FRONTEND_URL");
    private static final String PORT = System.getenv("PORT");

    public static void main(String[] args) throws Exception {
        // Configuración del puerto (Railway lo provee)
        int serverPort = PORT != null ? Integer.parseInt(PORT) : 8080;
        
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Endpoint GET /contactos
        server.createContext("/contactos", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleCorsPreflight(exchange);
                
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        List<String> contactos = getContactosDesdeDB();
                        String response = String.join(",", contactos);
                        sendResponse(exchange, 200, response);
                    } catch (SQLException e) {
                        sendResponse(exchange, 500, "Error al obtener contactos");
                    }
                } else {
                    sendResponse(exchange, 405, "Método no permitido");
                }
            }
        });

        // Endpoint POST /addContacto
        server.createContext("/addContacto", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                handleCorsPreflight(exchange);
                
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try {
                        InputStream is = exchange.getRequestBody();
                        String nuevoContacto = new String(is.readAllBytes()).trim();
                        boolean agregado = agregarContactoDB(nuevoContacto);
                        String response = agregado ? "Contacto agregado" : "Error al agregar";
                        sendResponse(exchange, agregado ? 200 : 500, response);
                    } catch (SQLException e) {
                        sendResponse(exchange, 500, "Error de base de datos");
                    }
                } else {
                    sendResponse(exchange, 405, "Método no permitido");
                }
            }
        });

        server.start();
        System.out.println("Servidor iniciado en puerto: " + serverPort);
    }

    // ------ Métodos auxiliares ------
    private static void handleCorsPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", FRONTEND_URL != null ? FRONTEND_URL : "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
    }

    // ------ Operaciones con DB ------
    private static List<String> getContactosDesdeDB() throws SQLException {
        List<String> contactos = new ArrayList<>();
        String query = "SELECT nombre FROM contactos";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                contactos.add(rs.getString("nombre"));
            }
        }
        return contactos;
    }

    private static boolean agregarContactoDB(String nombre) throws SQLException {
        String query = "INSERT INTO contactos (nombre) VALUES (?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            return stmt.executeUpdate() > 0;
        }
    }
}