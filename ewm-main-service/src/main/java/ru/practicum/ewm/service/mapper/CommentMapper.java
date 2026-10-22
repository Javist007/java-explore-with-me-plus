package ru.practicum.ewm.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.comment.request.NewCommentDto;
import ru.practicum.ewm.dto.comment.response.CommentDto;
import ru.practicum.ewm.dto.user.response.UserShortDto;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;

import java.util.List;

/**
 * Класс для преобразования комментариев между entity и DTO.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentMapper {

    /**
     * Преобразует комментарий в DTO. Автор и событие должны быть загружены вместе с комментарием.
     */
    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .author(new UserShortDto(comment.getAuthor().getId(), comment.getAuthor().getName()))
                .status(comment.getStatus())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }

    /**
     * Преобразует список комментариев в список DTO.
     */
    public static List<CommentDto> toDtoList(List<Comment> comments) {
        return comments.stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    /**
     * Создаёт комментарий из запроса. Статус и дату создания задаёт сервис.
     */
    public static Comment toEntity(NewCommentDto request, User author, Event event) {
        return Comment.builder()
                .text(request.getText())
                .author(author)
                .event(event)
                .build();
    }
}
