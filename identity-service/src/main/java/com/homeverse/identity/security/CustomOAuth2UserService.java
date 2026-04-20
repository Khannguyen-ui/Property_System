package com.homeverse.identity.security;

import com.homeverse.identity.entity.UserCredential;
import com.homeverse.identity.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserCredentialRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        String fbId = oAuth2User.getAttribute("id");

        log.info("Xử lý OAuth2 từ [{}]. Email: {}, FB_ID: {}", registrationId, email, fbId);

        String searchKey = (email != null) ? email : fbId + "@facebook.com";

        // Tìm hoặc tạo mới User
        UserCredential user = userRepository.findByEmail(searchKey)
                .map(existingUser -> {
                    existingUser.setFullName(fullName);
                    return existingUser;
                })
                .orElseGet(() -> {
                    UserCredential.UserCredentialBuilder builder = UserCredential.builder()
                            .fullName(fullName)
                            .password(null)
                            .role(UserCredential.Role.USER)
                            .isActive(true);

                    if (email != null) {
                        builder.email(email);
                    } else {
                        builder.email(searchKey);
                    }
                    return builder.build();
                });

        // LƯU USER ĐỂ SINH ID (NẾU MỚI) HOẶC LẤY ID HIỆN TẠI
        UserCredential savedUser = userRepository.saveAndFlush(user);
        
        log.info("Đã đồng bộ User vào DB. ID: {}", savedUser.getId());

        // QUAN TRỌNG: Trả về CustomOAuth2User thay vì oAuth2User mặc định
        return new CustomOAuth2User(oAuth2User, savedUser.getId());
    }
}