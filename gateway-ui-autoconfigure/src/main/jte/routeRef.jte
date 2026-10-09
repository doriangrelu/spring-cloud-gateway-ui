@import io.github.doriangrelu.gatewayui.internal.web.UiContext
@param UiContext ui
@param String id
@param boolean link
<%-- Identifiant d'une route : lien vers sa fiche, sauf dans l'éditeur où la route peut ne pas exister dans la Gateway --%>
@if(link)<a class="route-link" href="${ui.routeUrl(id)}">${id}</a>@else<span class="route-link mono">${id}</span>@endif
