# 0012. Modèle d'édition et export YAML

- Statut : Accepté
- Date : 2026-10-09
- Tâche : #24 (spike #28)

## Contexte

L'éditeur fait évoluer une configuration, pas une route isolée : il faut distinguer les routes existantes des nouvelles, conserver le travail en cours, et produire un YAML que le développeur reporte dans sa configuration. La Gateway ne connaît que les valeurs résolues : un placeholder `${ORDERS_URL}` y apparaît sous forme d'URL, et un secret en clair.

## Options envisagées

Pour le travail en cours : stockage côté serveur, ou dans le navigateur.

Pour les valeurs résolues :

- exporter tel quel avec un avertissement : risque d'exposer un secret ;
- masquer systématiquement les valeurs suspectes : casse des valeurs légitimes ;
- rétablir les placeholders à partir des valeurs brutes de la configuration Spring, et signaler les secrets restants.

## Décision

- L'espace de travail est conservé dans le navigateur (`localStorage`). Le serveur ne stocke rien et l'éditeur ne modifie jamais la Gateway (ADR 0004).
- Chaque route a un statut : inchangée, modifiée, nouvelle, retirée, ou Java (lecture seule, non exportable).
- Trois exports : route courante, configuration complète, modifications seulement.
- Le YAML utilise la forme raccourcie des prédicats et filtres, le préfixe `spring.cloud.gateway.server.webflux.routes`, et écrit `$\{` tout `${` littéral.
- Les placeholders sont rétablis à partir des valeurs brutes des sources de propriétés Spring pour les routes déclarées en configuration. Quand la valeur d'origine n'est pas disponible, un conseil signale les valeurs qui ressemblent à un secret.

## Conséquences

- Le travail reste local au navigateur : il n'est ni partagé ni sauvegardé côté serveur.
- Le YAML exporté conserve les placeholders d'origine dans la majorité des cas.
- Les routes ajoutées autrement que par la configuration (API d'actuator, Java) ne bénéficient pas du rétablissement des placeholders.
