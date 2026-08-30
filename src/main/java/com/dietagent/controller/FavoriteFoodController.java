package com.dietagent.controller;

import com.dietagent.dto.request.FavoriteFoodRequest;
import com.dietagent.entity.FavoriteFood;
import com.dietagent.service.FavoriteFoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteFoodController {

    private final FavoriteFoodService favoriteFoodService;

    @GetMapping
    public ResponseEntity<List<FavoriteFood>> list(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(favoriteFoodService.list(userId));
    }

    @PostMapping
    public ResponseEntity<FavoriteFood> add(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FavoriteFoodRequest request) {
        return ResponseEntity.ok(favoriteFoodService.add(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FavoriteFood> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody FavoriteFoodRequest request) {
        return ResponseEntity.ok(favoriteFoodService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        favoriteFoodService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
