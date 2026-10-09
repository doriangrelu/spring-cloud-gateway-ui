/*
 * SPIKE #28 : catalogue pédagogique des prédicats et filtres de Spring Cloud Gateway.
 * Rédigé avec nos propres mots. Pour le spike, une douzaine de fabriques sont documentées ; les autres ont une catégorie.
 */
const Catalog = (() => {
    const DOC = 'https://docs.spring.io/spring-cloud-gateway/reference/';

    const categories = {
        predicate: {
            'Chemin et hôte': ['Path', 'Host'],
            'Requête': ['Method', 'Header', 'Query', 'Cookie', 'ReadBody'],
            'Date': ['After', 'Before', 'Between'],
            'Client': ['RemoteAddr', 'XForwardedRemoteAddr'],
            'Répartition': ['Weight', 'CloudFoundryRouteService']
        },
        filter: {
            'Chemin': ['StripPrefix', 'PrefixPath', 'RewritePath', 'SetPath'],
            'En-têtes de requête': ['AddRequestHeader', 'AddRequestHeadersIfNotPresent', 'SetRequestHeader', 'RemoveRequestHeader',
                'MapRequestHeader', 'PreserveHostHeader', 'SetRequestHostHeader', 'RequestHeaderToRequestUri', 'RequestHeaderSize'],
            'En-têtes de réponse': ['AddResponseHeader', 'SetResponseHeader', 'RemoveResponseHeader', 'RewriteResponseHeader',
                'DedupeResponseHeader', 'RewriteLocationResponseHeader', 'SecureHeaders', 'RemoveJsonAttributesResponseBody'],
            'Paramètres': ['AddRequestParameter', 'RemoveRequestParameter', 'RewriteRequestParameter'],
            'Résilience et quotas': ['CircuitBreaker', 'Retry', 'RequestRateLimiter', 'FallbackHeaders', 'RequestSize'],
            'Corps': ['ModifyRequestBody', 'ModifyResponseBody', 'CacheRequestBody', 'JsonToGrpc'],
            'Routage et réponse': ['RedirectTo', 'SetStatus', 'SaveSession', 'TokenRelay', 'LocalResponseCache']
        }
    };

    const docs = {
        predicate: {
            Path: {
                summary: 'Retient la requête si son chemin correspond à l\'un des motifs.',
                details: '`*` remplace un segment, `**` tout le reste du chemin, `{id}` capture un segment réutilisable par les filtres (SetPath, PrefixPath…).',
                args: { patterns: 'Un ou plusieurs motifs, séparés par des virgules. Ex. : /api/orders/**', matchTrailingSlash: 'true par défaut : /a/ correspond aussi à /a.' },
                example: '- Path=/api/users/{id}, /legacy/users/{id}'
            },
            Method: {
                summary: 'Retient la requête si sa méthode HTTP fait partie de la liste.',
                args: { methods: 'Méthodes séparées par des virgules. Ex. : GET, POST' },
                example: '- Method=GET, HEAD'
            },
            Host: {
                summary: 'Retient la requête selon son en-tête Host.',
                details: 'Accepte les motifs, par ex. **.example.com, et capture des variables : {sub}.example.com.',
                args: { patterns: 'Un ou plusieurs motifs d\'hôte, séparés par des virgules.' },
                example: '- Host=api.example.com, {tenant}.example.com'
            },
            Header: {
                summary: 'Retient la requête si un en-tête est présent et que sa valeur correspond à une expression régulière.',
                args: { header: 'Nom de l\'en-tête.', regexp: 'Expression régulière que la valeur doit respecter. Ex. : \\d+' },
                example: '- Header=X-Request-Id, \\d+'
            },
            Query: {
                summary: 'Retient la requête si un paramètre est présent, et éventuellement s\'il correspond à une expression régulière.',
                args: { param: 'Nom du paramètre.', regexp: 'Facultatif : expression régulière sur la valeur.' },
                example: '- Query=version, v[12]'
            }
        },
        filter: {
            StripPrefix: {
                summary: 'Retire les premiers segments du chemin avant d\'appeler le service.',
                details: 'StripPrefix=1 sur /api/orders/42 transmet /orders/42 au service.',
                args: { parts: 'Nombre de segments à retirer, en partant du début. 1 par défaut.' },
                example: '- StripPrefix=1'
            },
            PrefixPath: {
                summary: 'Ajoute un préfixe devant le chemin transmis au service.',
                args: { prefix: 'Préfixe à ajouter. Ex. : /v2' },
                example: '- PrefixPath=/v2'
            },
            RewritePath: {
                summary: 'Réécrit le chemin avec une expression régulière.',
                details: 'Les groupes nommés (?<nom>...) se réutilisent dans le remplacement. En YAML, écrivez $\\{nom} : ${...} serait lu comme une propriété Spring.',
                args: { regexp: 'Expression régulière appliquée au chemin. Ex. : /api/users/(?<segment>.*)', replacement: 'Chemin de remplacement. Ex. : /users/$\\{segment}' },
                example: '- RewritePath=/api/users/(?<segment>.*), /users/$\\{segment}'
            },
            SetPath: {
                summary: 'Remplace entièrement le chemin, avec les variables capturées par le prédicat Path.',
                args: { template: 'Nouveau chemin, avec des variables. Ex. : /products/{id}' },
                example: '- SetPath=/products/{id}'
            },
            AddRequestHeader: {
                summary: 'Ajoute un en-tête à la requête transmise au service.',
                args: { name: 'Nom de l\'en-tête.', value: 'Valeur ; peut reprendre une variable du prédicat Path : {id}.' },
                example: '- AddRequestHeader=X-Request-Source, gateway'
            },
            RemoveRequestHeader: {
                summary: 'Retire un en-tête de la requête avant de l\'envoyer au service.',
                details: 'Utile pour ne pas propager un en-tête interne ou un cookie.',
                args: { name: 'Nom de l\'en-tête à retirer.' },
                example: '- RemoveRequestHeader=Cookie'
            },
            AddResponseHeader: {
                summary: 'Ajoute un en-tête à la réponse renvoyée au client.',
                args: { name: 'Nom de l\'en-tête.', value: 'Valeur.', override: 'true pour remplacer un en-tête déjà présent.' },
                example: '- AddResponseHeader=X-Served-By, users'
            },
            Retry: {
                summary: 'Rejoue la requête vers le service en cas d\'échec.',
                details: 'Attention aux requêtes non idempotentes (POST) : par défaut, seul GET est rejoué.',
                args: { retries: 'Nombre de nouvelles tentatives.', statuses: 'Codes HTTP qui déclenchent une nouvelle tentative.', methods: 'Méthodes rejouables.' },
                example: '- name: Retry\n  args:\n    retries: 3\n    statuses: BAD_GATEWAY'
            },
            CircuitBreaker: {
                summary: 'Coupe les appels vers un service défaillant et bascule éventuellement vers une solution de repli.',
                args: { name: 'Nom du circuit breaker (configuration Resilience4j).', fallbackUri: 'Facultatif : URI de repli, ex. forward:/fallback.' },
                example: '- CircuitBreaker=orders, forward:/fallback'
            },
            RequestRateLimiter: {
                summary: 'Limite le nombre de requêtes par client (nécessite Redis par défaut).',
                details: 'Configuration souvent en forme développée : replenishRate, burstCapacity et une clé (KeyResolver).',
                args: {},
                example: '- name: RequestRateLimiter\n  args:\n    redis-rate-limiter.replenishRate: 10'
            }
        }
    };

    function category(kind, name) {
        const entry = Object.entries(categories[kind]).find(([, names]) => names.includes(name));
        return entry ? entry[0] : 'Autres';
    }

    function describe(kind, name) {
        const doc = docs[kind][name];
        return { name, category: category(kind, name), documented: Boolean(doc), doc: DOC, ...(doc || { summary: 'Pas encore documenté.', args: {} }) };
    }

    return { describe };
})();
