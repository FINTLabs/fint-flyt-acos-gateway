package no.novari.instance.gateway.codelist.links

import no.fint.model.resource.FintLinks

class NoSuchLinkException(
    message: String,
) : RuntimeException(message) {
    companion object {
        fun noSelfLink(resource: FintLinks): NoSuchLinkException {
            return NoSuchLinkException("No self link in resource ${resource.javaClass.simpleName}")
        }

        fun noLink(
            resource: FintLinks,
            linkedResourceName: String,
        ): NoSuchLinkException {
            return NoSuchLinkException("No link for $linkedResourceName in resource ${resource.javaClass.simpleName}")
        }
    }
}
