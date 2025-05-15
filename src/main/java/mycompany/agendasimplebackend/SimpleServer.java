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
        System.out.println("Iniciando servidor en puerto: " + serverPort);
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Controlador JDBC cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar el controlador JDBC: " + e.getMessage());
            System.exit(1);
        }

        // Endpoint GET /contactos
        server.createContext("/contactos", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        setCorsHeaders(exchange);
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        List<String> contactos = getContactosDesdeDB();
                        String response = String.join(",", contactos);
                        sendResponse(exchange, 200, response);
                    } else {
                        sendResponse(exchange, 405, "Método no permitido");
                    }
                } catch (Exception e) {
                    System.err.println("Error en GET /contactos: " + e.getMessage());
                    sendResponse(exchange, 500, "Error interno del servidor");
                }
            }
        });

        // Endpoint POST /addContacto
        server.createContext("/addContacto", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        setCorsHeaders(exchange);
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        InputStream is = exchange.getRequestBody();
                        String nuevoContacto = new String(is.readAllBytes()).trim();
                        
                        if (nuevoContacto.isEmpty()) {
                            sendResponse(exchange, 400, "Nombre de contacto vacío");
                            return;
                        }

                        boolean agregado = agregarContactoDB(nuevoContacto);
                        String response = agregado ? "Contacto agregado" : "Error al agregar";
                        sendResponse(exchange, agregado ? 200 : 500, response);
                    } else {
                        sendResponse(exchange, 405, "Método no permitido");
                    }
                } catch (Exception e) {
                    System.err.println("Error en POST /addContacto: " + e.getMessage());
                    sendResponse(exchange, 500, "Error interno del servidor");
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Servidor iniciado correctamente");
    }

    // ------ Métodos auxiliares ------
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", 
            FRONTEND_URL != null ? FRONTEND_URL : "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept");
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
        } catch (SQLException e) {
            System.err.println("Error al obtener contactos: " + e.getMessage());
            throw e;
        }
        return contactos;
    }

    private static boolean agregarContactoDB(String nombre) throws SQLException {
        String query = "INSERT INTO contactos (nombre) VALUES (?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nombre);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error al agregar contacto: " + e.getMessage());
            throw e;
        }
    }
}