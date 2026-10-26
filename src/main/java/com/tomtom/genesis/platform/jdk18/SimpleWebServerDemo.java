package com.tomtom.genesis.platform.jdk18;

import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java 18 (JEP 408) - Simple Web Server.
 *
 * A minimal static file server, available both as the jwebserver command-line
 * tool and as the SimpleFileServer API. Intended for prototyping, testing and
 * ad-hoc file sharing - explicitly NOT for production.
 */
public class SimpleWebServerDemo {

    public static void main(String[] args) throws Exception {
        theCommandLineTool();
        theApi();
        limits();
    }

    private static void theCommandLineTool() {
        header("The jwebserver command");

        System.out.println("  jwebserver                              # serves . on localhost:8000");
        System.out.println("  jwebserver -p 9000                      # another port");
        System.out.println("  jwebserver -d /srv/files                # another directory");
        System.out.println("  jwebserver -b 0.0.0.0                   # listen on all interfaces");
        System.out.println("  jwebserver -o verbose                   # log every request");
        System.out.println();
        System.out.println("This replaces the reflex of reaching for 'python3 -m http.server' when");
        System.out.println("you need to serve a folder for two minutes. It ships inside the JDK,");
        System.out.println("so there is nothing to install.");
        System.out.println();
        System.out.println("It also serves as a ready-made target when you are learning HttpClient -");
        System.out.println("start jwebserver in one terminal and point the client at it.");
    }

    private static void theApi() throws Exception {
        header("The same thing from code");

        Path dir = Files.createTempDirectory("java18-webserver");
        try {
            Files.writeString(dir.resolve("hello.txt"), "served from a temporary directory\n");
            Files.createDirectory(dir.resolve("sub"));
            Files.writeString(dir.resolve("sub").resolve("nested.txt"), "a nested file\n");

            // Port 0 means "any free port", so this cannot clash with anything.
            HttpServer server = SimpleFileServer.createFileServer(
                    new InetSocketAddress("localhost", 0),
                    dir,
                    SimpleFileServer.OutputLevel.NONE);
            server.start();

            int port = server.getAddress().getPort();
            System.out.println("serving " + dir.getFileName() + " on port " + port);

            // Not try-with-resources: HttpClient only became AutoCloseable in Java 21,
            // and this file is meant to compile under --release 18.
            HttpClient client = HttpClient.newHttpClient();
            System.out.println();
            System.out.println("GET /hello.txt      -> " + get(client, port, "/hello.txt").trim());
            System.out.println("GET /sub/nested.txt -> " + get(client, port, "/sub/nested.txt").trim());

            HttpResponse<String> listing = fetch(client, port, "/");
            System.out.println("GET /               -> status " + listing.statusCode()
                    + ", " + listing.body().lines().count() + " lines of directory listing");

            HttpResponse<String> missing = fetch(client, port, "/nope.txt");
            System.out.println("GET /nope.txt       -> status " + missing.statusCode());

            server.stop(0);
            System.out.println();
            System.out.println("(server stopped)");
        } finally {
            try (var paths = Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
    }

    private static String get(HttpClient client, int port, String path) throws Exception {
        return fetch(client, port, path).body();
    }

    private static HttpResponse<String> fetch(HttpClient client, int port, String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void limits() {
        header("Deliberate limitations");

        System.out.println("  - GET and HEAD only. POST returns 501 Not Implemented.");
        System.out.println("  - HTTP/1.1 only - no HTTP/2.");
        System.out.println("  - No HTTPS, no authentication, no compression, no caching headers");
        System.out.println("    beyond Last-Modified.");
        System.out.println("  - Binds to the loopback interface by default, which is a safety");
        System.out.println("    decision: you have to opt in to exposing files on the network.");
        System.out.println();
        System.out.println("The JEP says outright that it is not intended for production. Treat it");
        System.out.println("as a development tool, in the same category as jshell.");
        System.out.println();
        System.out.println("The API is also pluggable: SimpleFileServer.createFileHandler(path) gives");
        System.out.println("you a handler you can mount on your own HttpServer next to other routes -");
        System.out.println("handy when a test needs both static files and a stub endpoint.");
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
