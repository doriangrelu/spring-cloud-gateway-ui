package dev.gatewayui.tester;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import dev.gatewayui.inspect.Definition;
import dev.gatewayui.inspect.FilterStep;
import dev.gatewayui.tester.TestResult.Simulation;
import dev.gatewayui.tester.TestResult.Step;
import dev.gatewayui.tester.TestResult.StepStatus;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

/**
 * Rejoue sur le papier les filtres de la route retenue qui modifient la requête transmise.
 * <p>
 * Les filtres ne sont jamais exécutés : seuls les filtres connus et sans effet de bord (chemin, en-têtes, paramètres)
 * sont simulés à partir de leur déclaration. Les autres sont listés comme non simulés.
 */
final class FilterSimulator {

    private FilterSimulator() {
    }

    static Simulation simulate(Route route, RouteDefinition routeDefinition, List<FilterDefinition> defaultFilters,
            ServerWebExchange exchange) {
        State state = new State(exchange);
        List<Definition> pending = new ArrayList<>();
        if (routeDefinition != null) {
            defaultFilters.forEach(definition -> pending.add(Definition.of(definition)));
            routeDefinition.getFilters().forEach(definition -> pending.add(Definition.of(definition)));
        }

        // route.getFilters() donne l'ordre réel ; les déclarations fournissent les arguments
        List<Step> steps = new ArrayList<>();
        for (GatewayFilter filter : route.getFilters()) {
            FilterStep described = FilterStep.ofRoute(filter);
            Definition definition = take(pending, described.name());
            if (definition == null) {
                String note = routeDefinition == null ? "Route définie en Java : effet inconnu"
                        : "Filtre personnalisé : effet non simulé";
                steps.add(new Step(described.name(), described.description(), StepStatus.NOT_SIMULATED, null, null, note));
            }
            else {
                steps.add(apply(definition, state));
            }
        }

        List<String> notes = new ArrayList<>();
        if (routeDefinition == null) {
            notes.add("Route définie en Java (DSL) : seuls ses prédicats peuvent être évalués, ses filtres ne sont pas simulés.");
        }
        notes.add("Les filtres globaux (X-Forwarded-*, load balancer, métriques...) ne sont pas simulés.");

        URI target = targetUri(route.getUri());
        String targetUrl = targetUrl(target, state);
        if (!state.preserveHost && target.getHost() != null) {
            state.headers.set(HttpHeaders.HOST, target.getPort() == -1 ? target.getHost() : target.getHost() + ":" + target.getPort());
        }
        if ("lb".equalsIgnoreCase(target.getScheme())) {
            notes.add("URI lb:// : l'hôte final est choisi par le load balancer parmi les instances de « " + target.getHost() + " ».");
        }

        List<Map.Entry<String, String>> headers = new ArrayList<>();
        state.headers.forEach((name, values) -> values.forEach(value -> headers.add(Map.entry(name, value))));
        return new Simulation(new LinkedHashMap<>(state.uriVariables), steps, targetUrl, headers, routeDefinition != null, notes);
    }

    private static Definition take(List<Definition> pending, String name) {
        Iterator<Definition> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Definition definition = iterator.next();
            if (definition.name().equals(name)) {
                iterator.remove();
                return definition;
            }
        }
        return null;
    }

    private static Step apply(Definition definition, State state) {
        String before = state.path;
        try {
            String note = switch (definition.name()) {
                case "StripPrefix" -> {
                    String parts = definition.arg("parts", 0);
                    state.path = stripPrefix(state.path, parts == null ? 1 : Integer.parseInt(parts.trim()));
                    yield null;
                }
                case "PrefixPath" -> {
                    state.path = expand(definition.arg("prefix", 0), state) + state.path;
                    yield null;
                }
                case "RewritePath" -> {
                    String replacement = definition.arg("replacement", 1).replace("$\\", "$");
                    state.path = Pattern.compile(definition.arg("regexp", 0)).matcher(state.path).replaceAll(replacement);
                    yield null;
                }
                case "SetPath" -> {
                    state.path = expand(definition.arg("template", 0), state);
                    yield null;
                }
                case "AddRequestHeader" -> {
                    String name = definition.arg("name", 0);
                    String value = ServerWebExchangeUtils.expand(state.exchange, definition.arg("value", 1));
                    state.headers.add(name, value);
                    yield "En-tête ajouté : " + name + ": " + value;
                }
                case "SetRequestHeader" -> {
                    String name = definition.arg("name", 0);
                    String value = ServerWebExchangeUtils.expand(state.exchange, definition.arg("value", 1));
                    state.headers.set(name, value);
                    yield "En-tête positionné : " + name + ": " + value;
                }
                case "RemoveRequestHeader" -> {
                    String name = definition.arg("name", 0);
                    state.headers.remove(name);
                    yield "En-tête supprimé : " + name;
                }
                case "AddRequestParameter" -> {
                    String name = definition.arg("name", 0);
                    String value = ServerWebExchangeUtils.expand(state.exchange, definition.arg("value", 1));
                    String parameter = UriUtils.encodeQueryParam(name, StandardCharsets.UTF_8) + "="
                            + UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
                    state.query = StringUtils.hasText(state.query) ? state.query + "&" + parameter : parameter;
                    yield "Paramètre ajouté : " + name + "=" + value;
                }
                case "SetRequestHostHeader" -> {
                    String host = definition.arg("host", 0);
                    state.headers.set(HttpHeaders.HOST, host);
                    state.preserveHost = true;
                    yield "Host forcé à " + host;
                }
                case "PreserveHostHeader" -> {
                    state.preserveHost = true;
                    yield "Le Host d'origine est transmis au service";
                }
                default -> null;
            };
            if (note == null && before.equals(state.path) && !isPathFilter(definition.name())) {
                StepStatus status = definition.name().contains("Response") ? StepStatus.RESPONSE_ONLY : StepStatus.NOT_SIMULATED;
                String text = status == StepStatus.RESPONSE_ONLY ? "Agit sur la réponse" : "Effet non simulé";
                return new Step(definition.name(), definition.argsText(), status, null, null, text);
            }
            return new Step(definition.name(), definition.argsText(), StepStatus.APPLIED, before, state.path, note);
        }
        catch (RuntimeException ex) {
            return new Step(definition.name(), definition.argsText(), StepStatus.NOT_SIMULATED, null, null,
                    "Simulation impossible : " + ex.getMessage());
        }
    }

    private static boolean isPathFilter(String name) {
        return switch (name) {
            case "StripPrefix", "PrefixPath", "RewritePath", "SetPath" -> true;
            default -> false;
        };
    }

    /** Même algorithme que {@code StripPrefixGatewayFilterFactory}. */
    static String stripPrefix(String path, int parts) {
        String[] originalParts = StringUtils.tokenizeToStringArray(path, "/");
        StringBuilder newPath = new StringBuilder("/");
        for (int i = parts; i < originalParts.length; i++) {
            if (newPath.length() > 1) {
                newPath.append('/');
            }
            newPath.append(originalParts[i]);
        }
        if (newPath.length() > 1 && path.endsWith("/")) {
            newPath.append('/');
        }
        return newPath.toString();
    }

    private static String expand(String template, State state) {
        return new UriTemplate(template).expand(state.uriVariables).getRawPath();
    }

    /** Une URI comme {@code lb:ws://service} porte son vrai schéma après le préfixe. */
    private static URI targetUri(URI routeUri) {
        if (routeUri.getHost() == null && routeUri.getSchemeSpecificPart() != null
                && routeUri.getSchemeSpecificPart().contains("://")) {
            return URI.create(routeUri.getSchemeSpecificPart());
        }
        return routeUri;
    }

    private static String targetUrl(URI target, State state) {
        String query = StringUtils.hasText(state.query) ? "?" + state.query : "";
        String scheme = target.getScheme() == null ? "" : target.getScheme();
        return switch (scheme) {
            case "forward" -> "forward:" + state.path + query;
            case "no" -> "no://op (aucun appel, la réponse est produite par les filtres)";
            default -> scheme + "://" + target.getHost() + (target.getPort() != -1 ? ":" + target.getPort() : "")
                    + state.path + query;
        };
    }

    private static final class State {

        final ServerWebExchange exchange;

        final Map<String, String> uriVariables;

        final HttpHeaders headers = new HttpHeaders();

        String path;

        String query;

        boolean preserveHost;

        State(ServerWebExchange exchange) {
            this.exchange = exchange;
            this.uriVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
            this.headers.addAll(exchange.getRequest().getHeaders());
            this.path = exchange.getRequest().getURI().getRawPath();
            this.query = exchange.getRequest().getURI().getRawQuery();
        }
    }
}
