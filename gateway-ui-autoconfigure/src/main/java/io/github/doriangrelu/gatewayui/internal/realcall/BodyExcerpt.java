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
package io.github.doriangrelu.gatewayui.internal.realcall;

/**
 * Extrait borné du corps d'une réponse.
 *
 * @param text texte de l'extrait, vide si le contenu est binaire
 * @param receivedBytes nombre d'octets lus ; au-delà de la limite, la lecture s'arrête
 * @param truncated le corps dépasse la limite : seul son début est affiché
 * @param binary le contenu n'est pas textuel et n'est pas affiché
 */
public record BodyExcerpt(String text, long receivedBytes, boolean truncated, boolean binary) {
}
