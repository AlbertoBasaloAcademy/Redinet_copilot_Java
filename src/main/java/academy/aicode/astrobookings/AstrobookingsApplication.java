package academy.aicode.astrobookings;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import academy.aicode.astrobookings.presentation.RocketHandler;

/**
 * Punto de entrada de la aplicación. Arranca un servidor HTTP embebido en el
 * puerto 8080 y registra el contexto `/rockets`.
 */
public class AstrobookingsApplication {

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/rockets", new RocketHandler());
    server.setExecutor(null); // Use default executor
    server.start();
    System.out.println("Server started at http://localhost:8080");
  }

}
