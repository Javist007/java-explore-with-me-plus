package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.dto.event.Location;

import java.time.LocalDateTime;

/**
 * Сущность события.
 * Представляет событие в системе с полной информацией о его параметрах и статусе.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    /**
     * Уникальный идентификатор события.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Краткое описание события (аннотация).
     */
    @Column(nullable = false)
    private String annotation;

    /**
     * Категория события.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Количество подтвержденных заявок на участие в событии.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long confirmedRequests = 0L;

    /**
     * Дата и время создания события.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    /**
     * Полное описание события.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Дата и время проведения события.
     */
    @Column(nullable = false)
    private LocalDateTime eventDate;

    /**
     * Пользователь-инициатор события.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    private User initiator;

    /**
     * Место проведения события (географические координаты).
     */
    @Embedded
    private Location location;

    /**
     * Флаг, указывающий является ли событие платным.
     */
    @Column(nullable = false)
    private Boolean paid = false;

    /**
     * Лимит количества участников события.
     */
    @Column(nullable = false)
    private Integer participantLimit = 0;

    /**
     * Дата и время публикации события.
     */
    private LocalDateTime publishedOn;

    /**
     * Флаг, указывающий нужна ли модерация заявок на участие.
     */
    @Column(nullable = false)
    private Boolean requestModeration = true;

    /**
     * Статус события.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventState state = EventState.PENDING;

    /**
     * Заголовок события.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Количество просмотров события.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;
}
