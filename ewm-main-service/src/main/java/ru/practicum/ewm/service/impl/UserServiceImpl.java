package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.user.request.NewUserRequest;
import ru.practicum.ewm.dto.user.request.UserSearchRequest;
import ru.practicum.ewm.dto.user.response.UserDto;
import ru.practicum.ewm.exception.model.ConflictException;
import ru.practicum.ewm.exception.model.NotFoundException;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.repository.spec.UserSpecifications;
import ru.practicum.ewm.service.UserService;
import ru.practicum.ewm.service.mapper.UserMapper;

import java.util.List;

/**
 * Реализация сервиса пользователей.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto registerUser(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "Пользователь с email '" + request.getEmail() + "' уже существует"
            );
        }

        User user = UserMapper.toEntity(request);
        User saved = userRepository.save(user);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(UserSearchRequest request) {
        int page = request.getFrom() / request.getSize();
        PageRequest pageable = PageRequest.of(page, request.getSize());

        Specification<User> spec = UserSpecifications.hasIds(request.getIds());

        return userRepository.findAll(spec, pageable).getContent().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }
}
