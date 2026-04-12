package com.brandPitara.sfs.appcontent.service.impl;

import com.brandPitara.sfs.appcontent.dto.AppContentPageResponse;
import com.brandPitara.sfs.appcontent.dto.AppShareLinksResponse;
import com.brandPitara.sfs.appcontent.dto.ContactUsResponse;
import com.brandPitara.sfs.appcontent.dto.ProfileAppContentResponse;
import com.brandPitara.sfs.appcontent.entity.AppContentPage;
import com.brandPitara.sfs.appcontent.entity.AppSetting;
import com.brandPitara.sfs.appcontent.repository.AppContentPageRepository;
import com.brandPitara.sfs.appcontent.repository.AppSettingRepository;
import com.brandPitara.sfs.appcontent.service.AppContentService;
import com.brandPitara.sfs.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppContentServiceImpl implements AppContentService {

    private final AppContentPageRepository appContentPageRepository;
    private final AppSettingRepository appSettingRepository;

    @Override
    public AppContentPageResponse getPageBySlug(String slug) {
        AppContentPage page = appContentPageRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Page not found: " + slug));

        return AppContentPageResponse.builder()
                .slug(page.getSlug())
                .title(page.getTitle())
                .content(page.getContent())
                .contentType(page.getContentType())
                .build();
    }

    @Override
    public AppShareLinksResponse getShareLinks() {
        Map<String, AppSetting> settings = getSettingsMap(List.of(
                "share_android_url",
                "share_ios_url"
        ));

        return AppShareLinksResponse.builder()
                .androidUrl(getSettingValue(settings, "share_android_url"))
                .iosUrl(getSettingValue(settings, "share_ios_url"))
                .build();
    }

    @Override
    public ContactUsResponse getContactUs() {
        Map<String, AppSetting> settings = getSettingsMap(List.of(
                "contact_email",
                "contact_phone",
                "contact_whatsapp"
        ));

        return ContactUsResponse.builder()
                .email(getSettingValue(settings, "contact_email"))
                .phone(getSettingValue(settings, "contact_phone"))
                .whatsapp(getSettingValue(settings, "contact_whatsapp"))
                .build();
    }

    @Override
    public ProfileAppContentResponse getProfileAppContent() {
        return ProfileAppContentResponse.builder()
                .shareLinks(getShareLinks())
                .contactUs(getContactUs())
                .build();
    }

    private Map<String, AppSetting> getSettingsMap(List<String> keys) {
        return appSettingRepository.findBySettingKeyInAndActiveTrue(keys)
                .stream()
                .collect(Collectors.toMap(AppSetting::getSettingKey, Function.identity()));
    }

    private String getSettingValue(Map<String, AppSetting> settings, String key) {
        AppSetting setting = settings.get(key);
        return setting != null ? setting.getSettingValue() : null;
    }
}