package no.novari.flyt.acos.instance.gateway.codelist.links

import no.fint.model.resource.FintLinks
import no.fint.model.resource.Link

object ResourceLinkUtil {
    fun getFirstSelfLink(resource: FintLinks): String {
        return resource.selfLinks
            .firstOrNull()
            ?.href
            ?: throw NoSuchLinkException.noSelfLink(resource)
    }

    fun getSelfLinks(resource: FintLinks): List<String> {
        return resource.selfLinks.map(Link::getHref)
    }

    fun getFirstLink(
        linkProducer: () -> List<Link>?,
        resource: FintLinks,
        linkedResourceName: String,
    ): String {
        return linkProducer()
            ?.firstOrNull()
            ?.href
            ?: throw NoSuchLinkException.noLink(resource, linkedResourceName)
    }
}
