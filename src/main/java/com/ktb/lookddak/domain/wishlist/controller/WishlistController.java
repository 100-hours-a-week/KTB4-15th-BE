package com.ktb.lookddak.domain.wishlist.controller;

import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateRequest;
import com.ktb.lookddak.domain.wishlist.dto.WishlistCreateResponse;
import com.ktb.lookddak.domain.wishlist.service.WishlistService;
import com.ktb.lookddak.global.response.ApiResponse;
import com.ktb.lookddak.global.response.SuccessCode;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistCreateResponse>> createWishlist(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody WishlistCreateRequest request
    ) {
        WishlistCreateResponse response = wishlistService.createWishlist(
                principal.getMemberId(),
                request
        );

        return ResponseEntity
                .status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<ApiResponse<Void>> deleteWishlist(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Positive @PathVariable Long wishlistId
    ) {
        wishlistService.deleteWishlist(principal.getMemberId(), wishlistId);

        return ResponseEntity
                .status(SuccessCode.OK.getStatus())
                .body(ApiResponse.success(SuccessCode.OK, null));
    }
}
