package com.brandPitara.sfs.cdn.gateway;

import java.util.Collection;

public interface CdnInvalidationGateway {
    void invalidate(Collection<String> paths);
}
