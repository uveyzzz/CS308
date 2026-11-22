package com.cs308.store.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.ForbiddenException;
import com.cs308.store.common.PageResponse;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JwtService.class)
class ReviewControllerTest {

    private static final long USER_ID = 7L;
    private static final String URL = "/api/products/3/reviews";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    /** Logs in the same way JwtAuthenticationFilter does: the principal is the user id. */
    private static RequestPostProcessor loggedIn(Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(USER_ID, null,
                List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    private ResultActions postReview(RequestPostProcessor login, String body) throws Exception {
        var request = post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(login == null ? request : request.with(login));
    }

    @Test
    void guestCannotPostAReview() throws Exception {
        postReview(null, """
                {"rating": 5}
                """).andExpect(status().isUnauthorized());
    }

    @Test
    void staffCannotPostAReview() throws Exception {
        postReview(loggedIn(Role.PRODUCT_MANAGER), """
                {"rating": 5}
                """).andExpect(status().isForbidden());
        verify(reviewService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    void customerPostsAReviewForThemselves() throws Exception {
        given(reviewService.create(eq(USER_ID), eq(3L), any()))
                .willReturn(new Review(3L, USER_ID, 4, "Great fit"));

        postReview(loggedIn(Role.CUSTOMER), """
                {"rating": 4, "comment": "Great fit"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.commentStatus").value("PENDING"));
    }

    @Test
    void ratingOutsideOneToFiveReturns400() throws Exception {
        postReview(loggedIn(Role.CUSTOMER), """
                {"rating": 6}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").value("Rating must be between 1 and 5"));
    }

    @Test
    void missingRatingReturns400() throws Exception {
        postReview(loggedIn(Role.CUSTOMER), """
                {"comment": "No stars"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }

    @Test
    void notDeliveredReturns403WithTheReason() throws Exception {
        given(reviewService.create(eq(USER_ID), eq(3L), any()))
                .willThrow(new ForbiddenException("You can review a product only after it has been delivered to you"));

        postReview(loggedIn(Role.CUSTOMER), """
                {"rating": 5}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can review a product only after it has been delivered to you"));
    }

    @Test
    void guestCanReadTheReviews() throws Exception {
        given(reviewService.listForProduct(3L, 0, 10)).willReturn(new PageResponse<>(
                List.of(new PublicReview(1L, 5, "Lovely", "Ada L.", null)), 0, 10, 1, 1));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorName").value("Ada L."))
                .andExpect(jsonPath("$.content[0].comment").value("Lovely"));
    }
}
