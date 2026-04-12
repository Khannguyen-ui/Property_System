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

import java.util.Optional;

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
        String fbId = oAuth2User.getAttribute("id"); // ID duy nhất của FB

        log.info("Xử lý OAuth2 từ [{}]. Email: {}, FB_ID: {}", registrationId, email, fbId);

        // LOGIC MỚI: Tìm user dựa trên Email (nếu có) hoặc dựa trên ID nhà cung cấp
        // Ở đây mình tạm thời dùng email ảo nếu không có email để khớp với cấu trúc DB hiện tại của bạn
        String searchKey = (email != null) ? email : fbId + "@facebook.com";

        UserCredential user = userRepository.findByEmail(searchKey)
                .map(existingUser -> {
                    existingUser.setFullName(fullName);
                    // Nếu login lần này có phone (FB trả về) thì cập nhật vào
                    // Lưu ý: Facebook chỉ trả phone nếu bạn được duyệt quyền đặc biệt
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
                        // Nếu dùng SĐT: Để trống email (null) và điền vào cột phone
                        // Giả định bạn đã có cột phone trong Entity
                        builder.email(searchKey); // Vẫn cần một định danh để findByEmail không lỗi
                        // builder.phone(fbId); // Bạn có thể lưu ID FB vào cột phone nếu muốn
                    }
                    
                    return builder.build();
                });

        userRepository.saveAndFlush(user);
        return oAuth2User;
    }
}