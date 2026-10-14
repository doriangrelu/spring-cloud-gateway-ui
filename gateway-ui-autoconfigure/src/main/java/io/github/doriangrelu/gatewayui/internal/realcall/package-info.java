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
/**
 * Mode « appel réel » du testeur (ADR 0015) : la requête saisie est envoyée à la Gateway elle-même, en boucle locale,
 * comme le ferait un vrai client. Désactivé par défaut ; c'est le seul paquet, hors {@code web}, qui émet du HTTP.
 *
 * <p>Interne : sans garantie de compatibilité d'une version à l'autre (ADR 0008).
 */
package io.github.doriangrelu.gatewayui.internal.realcall;
