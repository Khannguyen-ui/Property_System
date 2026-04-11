package com.homeverse.identity.mapper;

import com.homeverse.identity.dto.request.UserRegisterDTO;
import com.homeverse.identity.dto.response.UserResponseDTO;
import com.homeverse.identity.entity.UserCredential;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private ModelMapper modelMapper;

    public UserCredential toEntity(UserRegisterDTO dto) {
                return modelMapper.map(dto, UserCredential.class);
    }

    public UserResponseDTO toResponse(UserCredential entity) {
        return modelMapper.map(entity, UserResponseDTO.class);
    }
}