package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.dto.request.RegisterUserRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateUserRequest;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.exception.DuplicateEmailException;
import com.srikrishnanethi.agamotto.exception.InvalidCredentialsException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.mapper.UserMapper;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.jwt.JwtService;
import com.srikrishnanethi.agamotto.service.UserService;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           PasswordEncoder passwordEncoder,
                           UserMapper userMapper,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthenticatedUser register(RegisterUserRequest request) {
        String email = request.email().trim().toLowerCase();
        boolean exists = this.userRepository.existsByEmail(email);
        if (exists) throw new DuplicateEmailException(email);

        User user = this.userMapper.toEntity(request);
        String hashPass = this.passwordEncoder.encode(request.password());
        user.setPasswordHash(hashPass);

        User savedUser = this.userRepository.save(user);
        initializeProfile(savedUser);
        String token = jwtService.issueToken(savedUser.getId(), savedUser.getEmail());
        return new AuthenticatedUser(savedUser, token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser login(String email, String password) {
        Optional<User> foundUser = this.userRepository.findByEmail(email.trim().toLowerCase());
        if (foundUser.isEmpty()) throw new InvalidCredentialsException();

        User user = foundUser.get();
        boolean goodPassword = this.passwordEncoder.matches(password, user.getPasswordHash());
        if (!goodPassword) throw new InvalidCredentialsException();

        initializeProfile(user);
        String token = jwtService.issueToken(user.getId(), user.getEmail());
        return new AuthenticatedUser(user, token);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(String userId) {
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        initializeProfile(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(String userId, UpdateUserRequest request) {
        User user = this.getById(userId);

        UserProfile profile = user.getProfile();
        if (profile == null) {
            var profileOpt = userProfileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                profile = profileOpt.get();
            } else {
                profile = new UserProfile();
                profile.setUser(user);
                user.setProfile(profile);
            }
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.preferredStart() != null) {
            profile.setPreferredStart(request.preferredStart());
        }
        if (request.preferredEnd() != null) {
            profile.setPreferredEnd(request.preferredEnd());
        }
        if (request.includeWeekends() != null) {
            profile.setIncludeWeekends(request.includeWeekends());
        }
        if (request.weightPriority() != null) {
            profile.setWeightPriority(request.weightPriority());
        }
        if (request.weightUrgency() != null) {
            profile.setWeightUrgency(request.weightUrgency());
        }
        if (request.weightDuration() != null) {
            profile.setWeightDuration(request.weightDuration());
        }

        if (!profile.getPreferredEnd().isAfter(profile.getPreferredStart())) {
            throw new IllegalArgumentException("preferredEnd must be after preferredStart");
        }

        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);
        User saved = userRepository.save(user);
        initializeProfile(saved);
        return saved;
    }

    private static void initializeProfile(User user) {
        if (user.getProfile() != null) {
            Hibernate.initialize(user.getProfile());
        }
    }
}
