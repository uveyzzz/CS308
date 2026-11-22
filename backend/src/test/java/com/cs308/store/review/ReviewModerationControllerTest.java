package com.cs308.store.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.PageResponse;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;

@WebMvcTest(ReviewModerationController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class ReviewModerationControllerTest {

    private static final long MANAGER_ID = 2L;
    private static final String QUEUE = "/api/admin/products/reviews";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewModerationService moderationService;

    private static RequestPostProcessor loggedIn(Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(MANAGER_ID, null,
                List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    @Test
    void productManagerSeesThePendingQueue() throws Exception {
        given(moderationService.pendingComments(0, 20)).willReturn(new PageResponse<>(List.of(
                new ModerationItem(12L, 3L, "Basic Tee", "basic-tee", 4, "Great fit", CommentStatus.PENDING,
                        "Ada Lovelace", Instant.now())), 0, 20, 1, 1));

        mockMvc.perform(get(QUEUE).with(loggedIn(Role.PRODUCT_MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Basic Tee"))
                .andExpect(jsonPath("$.content[0].comment").value("Great fit"));
    }

    @Test
    void customerAndOtherStaffCannotModerate() throws Exception {
        for (Role role : List.of(Role.CUSTOMER, Role.SALES_MANAGER, Role.SUPPORT_AGENT)) {
            mockMvc.perform(get(QUEUE).with(loggedIn(role))).andExpect(status().isForbidden());
            mockMvc.perform(patch(QUEUE + "/12").with(loggedIn(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"APPROVED\"}"))
                    .andExpect(status().isForbidden());
        }
        verify(moderationService, never()).decide(anyLong(), anyLong(), any());
    }

    @Test
    void guestGets401() throws Exception {
        mockMvc.perform(get(QUEUE)).andExpect(status().isUnauthorized());
    }

    @Test
    void approveRecordsTheManagerFromTheToken() throws Exception {
        Review review = new Review(3L, 7L, 4, "Great fit");
        review.moderate(CommentStatus.APPROVED, MANAGER_ID);
        given(moderationService.decide(12L, MANAGER_ID, ModerationRequest.Decision.APPROVED)).willReturn(review);

        mockMvc.perform(patch(QUEUE + "/12").with(loggedIn(Role.PRODUCT_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentStatus").value("APPROVED"));

        verify(moderationService).decide(12L, MANAGER_ID, ModerationRequest.Decision.APPROVED);
    }

    @Test
    void decisionPendingIsNotAccepted() throws Exception {
        mockMvc.perform(patch(QUEUE + "/12").with(loggedIn(Role.PRODUCT_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\": \"PENDING\"}"))
                .andExpect(status().isBadRequest());
        verify(moderationService, never()).decide(anyLong(), anyLong(), any());
    }

    @Test
    void missingDecisionReturns400() throws Exception {
        mockMvc.perform(patch(QUEUE + "/12").with(loggedIn(Role.PRODUCT_MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.decision").value("Decision is required"));
    }
}
