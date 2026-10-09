package dev.gatewayui.inspect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.support.NameUtils;

/**
 * Prédicat ou filtre tel que déclaré dans la configuration ({@code Path=/api/**}, {@code StripPrefix=1}...).
 */
public record Definition(String name, Map<String, String> args) {

    public Definition {
        args = args == null ? Map.of() : new LinkedHashMap<>(args);
    }

    public static Definition of(PredicateDefinition definition) {
        return new Definition(definition.getName(), definition.getArgs());
    }

    public static Definition of(FilterDefinition definition) {
        return new Definition(definition.getName(), definition.getArgs());
    }

    /**
     * Prédicats d'une route sous la forme {@code Path=/api/** && Method=GET}.
     */
    public static String summary(List<Definition> predicates) {
        return predicates.stream()
                .map(definition -> definition.name() + "=" + definition.argsText())
                .collect(Collectors.joining(" && "));
    }

    /**
     * Retourne un argument par son nom (forme développée) ou par sa position (forme raccourcie).
     */
    public String arg(String key, int index) {
        String value = args.get(key);
        if (value != null) {
            return value;
        }
        value = args.get(NameUtils.generateName(index));
        if (value != null) {
            return value;
        }
        List<String> values = new ArrayList<>(args.values());
        return index < values.size() ? values.get(index) : null;
    }

    /**
     * Arguments rendus comme dans la forme raccourcie du YAML.
     */
    public String argsText() {
        boolean shortcut = args.keySet().stream().allMatch(key -> key.startsWith(NameUtils.GENERATED_NAME_PREFIX));
        if (shortcut) {
            return String.join(", ", args.values());
        }
        return args.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
