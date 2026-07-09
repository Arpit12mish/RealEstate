package com.brandPitara.sfs.company.service;

import com.brandPitara.sfs.company.dto.ConnectedBrandDto;

import java.util.List;

public interface CompanyConnectedBrandPublicService {

  List<ConnectedBrandDto> getConnectedBrands(Long companyId);
}
