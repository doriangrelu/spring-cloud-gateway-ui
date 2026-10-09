package dev.gatewayui.tester;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Requête saisie dans le testeur de routes.
 *
 * @param pathAndQuery chemin et query string, par ex. {@code /api/orders/42?expand=true}
 * @param host valeur de l'en-tête {@code Host} et de l'hôte de l'URI
 * @param headers en-têtes supplémentaires, un par ligne au format {@code Nom: valeur}
 * @param remoteAddress IP du client, utilisée par le prédicat {@code RemoteAddr}
 */
public record TestRequest(String method, String pathAndQuery, String host, String headers, String remoteAddress) {

    static final String DEFAULT_HOST = "localhost";

    static final String DEFAULT_REMOTE_ADDRESS = "127.0.0.1";

    public TestRequest {
        method = blankTo(method, "GET").toUpperCase(Locale.ROOT);
        pathAndQuery = blankTo(pathAndQuery, "/");
        if (!pathAndQuery.startsWith("/")) {
            pathAndQuery = "/" + pathAndQuery;
        }
        host = blankTo(host, DEFAULT_HOST);
        headers = headers == null ? "" : headers;
        remoteAddress = blankTo(remoteAddress, DEFAULT_REMOTE_ADDRESS);
    }

    public static TestRequest from(MultiValueMap<String, String> params) {
        return new TestRequest(params.getFirst("method"), params.getFirst("path"), params.getFirst("host"),
                params.getFirst("headers"), params.getFirst("remoteAddress"));
    }

    /** Formulaire vide : aucune requête à tester. */
    public static boolean isEmpty(MultiValueMap<String, String> params) {
        String path = params.getFirst("path");
        return path == null || path.isBlank();
    }

    /**
     * @return le message d'erreur à afficher, ou {@code null} si la requête peut être testée
     */
    public String validate() {
        if (!method.matches("[A-Z]+")) {
            return "Méthode HTTP invalide : " + method;
        }
        try {
            URI uri = uri();
            if (uri.getHost() == null) {
                return "Hôte invalide : " + host;
            }
            return null;
        }
        catch (IllegalArgumentException ex) {
            return "Chemin ou hôte invalide : " + ex.getMessage();
        }
    }

    HttpMethod httpMethod() {
        return HttpMethod.valueOf(method);
    }

    URI uri() {
        return UriComponentsBuilder.fromUriString("http://" + host + pathAndQuery).encode().build().toUri();
    }

    InetSocketAddress inetRemoteAddress() {
        // Uniquement des IP littérales : pas de résolution DNS déclenchée depuis l'UI
        if (remoteAddress.matches("\\d{1,3}(\\.\\d{1,3}){3}") || remoteAddress.matches("[0-9a-fA-F:]*:[0-9a-fA-F:.]*")) {
            try {
                return new InetSocketAddress(InetAddress.getByName(remoteAddress), 0);
            }
            catch (UnknownHostException ex) {
                // IP invalide : traitée comme non résolue
            }
        }
        return InetSocketAddress.createUnresolved(remoteAddress, 0);
    }

    HttpHeaders httpHeaders() {
        HttpHeaders result = new HttpHeaders();
        for (String line : headers.split("\\R")) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                result.add(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        if (!result.containsHeader(HttpHeaders.HOST)) {
            result.set(HttpHeaders.HOST, host);
        }
        return result;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
