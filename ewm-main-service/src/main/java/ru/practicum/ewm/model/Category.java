package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность категории событий в базе данных.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}