package no.novari.instance.gateway.codelist.links

import no.fint.model.resource.FintLinks
import no.fint.model.resource.Link

object ResourceLinkUtil {

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
