package com.wallet.user.service;

import com.wallet.code.client.WalletServiceClient;
import com.wallet.code.dto.UserCreatedPayload;
import com.wallet.code.dto.WalletBalanceDto;
import com.wallet.user.dto.UserDto;
import com.wallet.user.dto.UserProfileDto;
import com.wallet.user.model.User;
import com.wallet.user.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepo userRepo;

    @Value("${user.created.topic}")
    private String usercreatedtopic;

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private RedisTemplate<String, UserDto> redisTemplate;
    @Autowired private WalletServiceClient walletServiceClient;

    @Transactional
    public Long createuser(UserDto userDto) throws ExecutionException, InterruptedException {
        // persist
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setKycNumber(userDto.getKycNumber());
        user = userRepo.save(user);
        // event
        UserCreatedPayload payload = new UserCreatedPayload();
        payload.setUserId(user.getId());
        payload.setUserName(user.getName());
        payload.setUserEmail(user.getEmail());
        payload.setRequestId(MDC.get("requestId"));
      // async send + proper logging (non-blocking, modern API)
        Future<SendResult<String,Object>> future= kafkaTemplate.send(usercreatedtopic, payload.getUserEmail(), payload);
        LOGGER.info("Pushed userCreatedPayload to kafka: {}",future.get());
        // cache the saved snapshot
        String key = "user:" + user.getId();
        UserDto snapshot = new UserDto();
        snapshot.setName(user.getName());
        snapshot.setEmail(user.getEmail());
        snapshot.setPhone(user.getPhone());
        snapshot.setKycNumber(user.getKycNumber());
        redisTemplate.opsForValue().set(key, snapshot);

        return user.getId();
    }

    public UserProfileDto getUserProfile(Long userId) {
        UserProfileDto profile = new UserProfileDto();
        String key = "user:" + userId;

        // cache-aside
        UserDto dto = redisTemplate.opsForValue().get(key);
        if (dto == null) {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for id=" + userId));
            dto = new UserDto();
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setPhone(user.getPhone());
            dto.setKycNumber(user.getKycNumber());
            redisTemplate.opsForValue().set(key, dto);
        }

        profile.setUserDetail(dto);

        // Feign call (404 -> null if decode404=true; 5xx -> exception caught)
        double balance = 0.0;
        try {
            WalletBalanceDto wb = walletServiceClient.getBalance(userId);
            if (wb != null && wb.getBalance() != null) {
                balance = wb.getBalance();
            } else {
                LOGGER.warn("Wallet not found or balance null for userId={}, defaulting to 0.0", userId);
            }
        } catch (feign.FeignException e) {
            LOGGER.warn("Wallet service error for userId={}, status={}", userId, e.status(), e);
        }
        profile.setWalletBalance(balance);

        return profile;
    }

}
