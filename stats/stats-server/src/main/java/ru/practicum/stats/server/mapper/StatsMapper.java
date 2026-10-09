package ru.practicum.stats.server.mapper;

import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.projection.ViewStatsProjection;

import java.util.ArrayList;
import java.util.List;

public class StatsMapper {

    /**
     * Преобразует список проекций из БД в список DTO.
     */
    public static List<ViewStatsDto> toViewStatsDtoList(List<ViewStatsProjection> projections) {
        List<ViewStatsDto> result = new ArrayList<>();
        for (ViewStatsProjection p : projections) {
            ViewStatsDto dto = new ViewStatsDto();
            dto.setApp(p.getApp());
            dto.setUri(p.getUri());
            dto.setHits(p.getHits());
            result.add(dto);
        }
        return result;
    }
}