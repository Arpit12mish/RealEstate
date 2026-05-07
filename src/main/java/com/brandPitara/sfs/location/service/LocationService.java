package com.brandPitara.sfs.location.service;

import com.brandPitara.sfs.location.dto.LocationResolveRequest;
import com.brandPitara.sfs.location.dto.LocationResolveResponse;

public interface LocationService {

    LocationResolveResponse resolve(LocationResolveRequest request);
}