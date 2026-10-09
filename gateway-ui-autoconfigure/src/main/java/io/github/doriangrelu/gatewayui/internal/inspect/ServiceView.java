/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.doriangrelu.gatewayui.internal.inspect;

import java.util.List;

/**
 * Service cible de la Gateway et routes qui y mènent.
 *
 * @param name nom technique du service (clé de configuration, ou nom déduit du host)
 * @param displayName libellé affiché
 * @param baseUrl {@code scheme://host:port}, ou {@code null} pour un service déclaré uniquement par {@code route-ids}
 * @param declared {@code true} si le service vient de {@code gateway.ui.services}, {@code false} s'il est déduit des routes
 * @param routes routes qui mènent au service, dans l'ordre d'évaluation
 */
public record ServiceView(String name, String displayName, String baseUrl, boolean declared, List<RouteView> routes) {
}
