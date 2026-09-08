package com.brandPitara.sfs.publiccontent.media;

import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;

public interface PublicMediaUrlResolver {
    String resolve(CmsMediaAssetEntity asset);
}
