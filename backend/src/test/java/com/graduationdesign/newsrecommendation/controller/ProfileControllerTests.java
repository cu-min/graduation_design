package com.graduationdesign.newsrecommendation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.JwtTokenProvider;
import com.graduationdesign.newsrecommendation.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldUploadAvatarForAuthenticatedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.png",
            MediaType.IMAGE_PNG_VALUE,
            "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/profile/avatar")
                .file(file)
                .header("Authorization", bearerTokenForReader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("/uploads/avatars/")));
    }

    @Test
    void shouldUpdateAndReturnCurrentUserInterests() throws Exception {
        mockMvc.perform(put("/api/profile/interests")
                .header("Authorization", bearerTokenForReader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tagIds":[1,2]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/profile/interests")
                .header("Authorization", bearerTokenForReader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").isNumber());
    }

    private String bearerTokenForReader() {
        User reader = userService.getById(2L);
        return "Bearer " + jwtTokenProvider.generateToken(reader);
    }
}
