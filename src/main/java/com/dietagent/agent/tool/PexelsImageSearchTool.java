package com.dietagent.agent.tool;

import com.dietagent.service.PexelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class PexelsImageSearchTool implements Function<PexelsImageSearchTool.Request, PexelsImageSearchTool.Response> {

    private final PexelsService pexelsService;

    @Override
    public Response apply(Request request) {
        String query = request.query;
        int perPage = request.perPage != null ? request.perPage : 5;

        List<PexelsService.PexelsImage> images = pexelsService.searchImages(query, perPage);

        StringBuilder sb = new StringBuilder();
        if (images.isEmpty()) {
            sb.append("未找到相关图片");
        } else {
            sb.append("<div class='pexels-images'>");
            sb.append("<p>找到 ").append(images.size()).append(" 张相关图片：</p>");
            for (PexelsService.PexelsImage img : images) {
                sb.append("<div class='pexels-image-item'>");
                sb.append("<img src='").append(img.getMediumUrl()).append("' alt='").append(img.getAlt() != null ? img.getAlt() : query).append("' loading='lazy' />");
                sb.append("<p class='pexels-credit'>摄影师：").append(img.getPhotographer()).append(" | <a href='").append(img.getPhotographerUrl()).append("' target='_blank'>来源</a></p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        return new Response(sb.toString());
    }

    public record Request(String query, Integer perPage) {}
    public record Response(String imageResults) {}
}
