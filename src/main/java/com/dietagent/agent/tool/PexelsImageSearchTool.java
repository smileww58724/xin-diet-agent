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
                sb.append("<img src='").append(esc(img.getMediumUrl()))
                  .append("' alt='").append(esc(img.getAlt() != null && !img.getAlt().isEmpty() ? img.getAlt() : query))
                  .append("' loading='lazy' />");
                sb.append("<p class='pexels-credit'>摄影师：").append(esc(img.getPhotographer()))
                  .append(" | <a href='").append(esc(img.getPhotographerUrl()))
                  .append("' target='_blank' rel='noopener'>来源</a></p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        return new Response(sb.toString());
    }

    /**
     * HTML 转义：alt/摄影师名/URL 都是第三方 API 返回的外部数据，
     * 直接拼进 HTML 属性会被前端 v-html 渲染，构成注入面，必须先转义
     */
    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record Request(String query, Integer perPage) {}
    public record Response(String imageResults) {}
}
