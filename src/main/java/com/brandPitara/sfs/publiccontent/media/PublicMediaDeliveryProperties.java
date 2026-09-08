package com.brandPitara.sfs.publiccontent.media;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.cms.media.public-delivery")
public class PublicMediaDeliveryProperties {
    /** CloudFront/custom-domain origin, without an object key. */
    private String baseUrl;
}
