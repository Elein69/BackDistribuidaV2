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

    // Configuración de la base de datos
    private static final String DB_URL = "jdbc:mysql://18.217.100.110:3306/agenda";
    private static final String DB_USER = "adminMysql";
    private static final String DB_PASSWORD = "12345"; 

    public static void main(String[] args) throws Exception {
        // Crear servidor en el puerto 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Ruta GET /contactos
        server.createContext("/contactos", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<String> contactos = getContactosDesdeDB();
                    String response = String.join(",", contactos);
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // Ruta POST /addContacto
        server.createContext("/addContacto", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String nuevoContacto = new String(is.readAllBytes()).trim();
                    boolean agregado = agregarContactoDB(nuevoContacto);
                    String response = agregado ? "Contacto agregado con éxito" : "Error al agregar contacto";
                    exchange.sendResponseHeaders(agregado ? 200 : 500, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // Iniciar servidor
        server.start();
        System.out.println("Servidor corriendo en http://localhost:8080");
    }

    // Obtener contactos desde la base de datos
    private static List<String> getContactosDesdeDB() {
        List<String> contactos = new ArrayList<>();
        String query = "SELECT nombre FROM contactos";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                contactos.add(rs.getString("nombre"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contactos;
    }

    // Agregar contacto a la base de datos
    private static boolean agregarContactoDB(String nombre) {
        String query = "INSERT INTO contactos (nombre) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nombre);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
