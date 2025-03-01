package com.example.demo.security.oauth.access;

import com.example.demo.base.code.exception.CustomException;
import com.example.demo.base.status.ErrorStatus;
import com.example.demo.domain.converter.TokenConverter;
import com.example.demo.entity.base.Token;
import com.example.demo.entity.base.User;
import com.example.demo.entity.enums.Role;
import com.example.demo.entity.enums.Status;
import com.example.demo.repository.TokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.jwt.JwtUtil;
import com.example.demo.security.oauth.info.NaverUserInfo;
import com.example.demo.security.oauth.info.GoogleUserInfo;
import com.example.demo.security.oauth.info.KakaoUserInfo;
import com.example.demo.security.oauth.info.UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class CommonOAuthHandler extends OncePerRequestFilter {

    private final String baseUrl;
    private final String redirectBaseUrl;
    private final String googleClientId;
    private final String googleClientSecret;
    private final String kakaoClientId;
    private final String kakaoClientSecret;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String googleTokenUri;
    private final String kakaoTokenUri;
    private final String naverTokenUri;
    private final String googleRedirectUri;
    private final String kakaoRedirectUri;
    private final String naverRedirectUri;
    private String provider;

    @Autowired
    private GoogleUserInfo googleUserInfo;

    @Autowired
    private KakaoUserInfo kakaoUserInfo;

    @Autowired
    private NaverUserInfo naverUserInfo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private TokenConverter tokenConverter;

    public CommonOAuthHandler(
            @Value("${base-url}") String baseUrl,
            @Value("${redirect-url}") String redirectBaseUrl,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String googleClientSecret,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}") String kakaoClientId,
            @Value("${spring.security.oauth2.client.registration.kakao.client-secret}") String kakaoClientSecret,
            @Value("${spring.security.oauth2.client.registration.naver.client-id}") String naverClientId,
            @Value("${spring.security.oauth2.client.registration.naver.client-secret}") String naverClientSecret,
            @Value("${spring.security.oauth2.client.provider.google.token-uri}") String googleTokenUri,
            @Value("${spring.security.oauth2.client.provider.kakao.token-uri}") String kakaoTokenUri,
            @Value("${spring.security.oauth2.client.provider.naver.token-uri}") String naverTokenUri,
            @Value("${spring.security.oauth2.client.google-path-uri}") String googleRedirectUri,
            @Value("${spring.security.oauth2.client.kakao-path-uri}") String kakaoRedirectUri,
            @Value("${spring.security.oauth2.client.naver-path-uri}") String naverRedirectUri
    ) {
        this.baseUrl = baseUrl;
        this.redirectBaseUrl = redirectBaseUrl;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.kakaoClientId = kakaoClientId;
        this.kakaoClientSecret = kakaoClientSecret;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.googleTokenUri = googleTokenUri;
        this.kakaoTokenUri = kakaoTokenUri;
        this.naverTokenUri = naverTokenUri;
        this.googleRedirectUri = googleRedirectUri;
        this.kakaoRedirectUri = kakaoRedirectUri;
        this.naverRedirectUri = naverRedirectUri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String tokenUri = null;

        if (path.equals(googleRedirectUri)) {
            tokenUri = googleTokenUri;
            provider = "google";
        } else if (path.equals(kakaoRedirectUri)) {
            tokenUri = kakaoTokenUri;
            provider = "kakao";
        } else if (path.equals(naverRedirectUri)) {
            tokenUri = naverTokenUri;
            provider = "naver";
        }

        if (tokenUri != null) {
            String code = request.getParameter("code");
            if (code == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Authorization code is missing.");
                return;
            }

            MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
            tokenRequest.add("code", code);
            tokenRequest.add("client_id", getClientId(path));
            tokenRequest.add("client_secret", getClientSecret(path));
            tokenRequest.add("redirect_uri", baseUrl + path);
            tokenRequest.add("grant_type", "authorization_code");

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                if (provider.equals("naver")) {
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                }

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(tokenRequest, headers);
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> tokenResponse = restTemplate.exchange(
                        tokenUri,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(tokenResponse.getBody());
                String accessToken = jsonNode.get("access_token").asText();

                UserInfo user = null;
                if (provider.equals("google")) {
                    user = googleUserInfo.getGoogleUserInfo(accessToken);
                } else if (provider.equals("kakao")) {
                    user = kakaoUserInfo.getKakaoUserInfo(accessToken);
                } else if (provider.equals("naver")) {
                    user = naverUserInfo.getNaverUserInfo(accessToken);
                }

                // 사용자 이메일로 회원가입 또는 로그인 처리
                String userName = "";
                if (userRepository.findByUserId(user.getEmail()).isPresent()) {

                    // 재로그인 ( 활성화 상태로 설정하고 새로운 토큰 생성, 만일 탈퇴 상태였다면 탈퇴 시간 삭제 )
                    Token token = tokenRepository.findByUser(userRepository.findByUserId(user.getEmail()).get())
                            .orElseThrow(() -> new CustomException(ErrorStatus.TOKEN_NOT_FOUND));
                    token.setAccessToken(jwtUtil.generateAccessToken(user.getEmail()));
                    token.setRefreshToken(jwtUtil.generateRefreshToken(user.getEmail()));
                    token.setUpdatedAt(LocalDateTime.now());

                    tokenRepository.save(token); // 업데이트 된 토큰 저장

                    token.getUser().activate(); // 활성화 상태로 설정
                    userRepository.save(token.getUser());

                    // 사용자 이름 조회
                    userName = token.getUser().getUserName();

                } else {
                    // 회원가입
                    userName = generateUserName(); // 고유한 이름 생성
                    String password = "myfirstpassword!"; // 초기 비밀번호는 "myfirstpassword!"으로 세팅

                    // 유저 객체 생성
                    User newUser = User.builder()
                            .userId(user.getEmail())
                            .password(password)
                            .userName(userName)
                            .provider(user.getProvider())
                            .role(Role.USER)  // 기본적으로 USER로 설정
                            .active(Status.ACTIVE)  // 기본적으로 ACTIVE로 설정
                            .build();

                    userRepository.save(newUser); // 유저 DB에 저장

                    accessToken = jwtUtil.generateAccessToken(user.getEmail());
                    String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

                    Token token = tokenConverter.toEntity(accessToken, refreshToken, newUser);
                    token.setCreatedAt(LocalDateTime.now());
                    token.setUpdatedAt(LocalDateTime.now());

                    tokenRepository.save(token); // 토큰 DB에 저장
                }

                // 프론트로 리다이렉트 -> 사용자의 고유한 이름 포함
                String redirectUrl = String.format("%s?name=%s", redirectBaseUrl, URLEncoder.encode(userName, "UTF-8"));
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", redirectUrl);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Failed to retrieve access token: " + e.getMessage());
            }
            return;
        }

        filterChain.doFilter(request, response);
    }


    private String generateUserName() {
        String[] namePatterns = {"Jpamaster", "Chasdw", "QQWR"};
        Random random = new Random();
        String name = namePatterns[random.nextInt(namePatterns.length)];
        int number = random.nextInt(100); // 번호는 0~99 사이
        return name + number;
    }

    private String getClientId(String path) {
        if (path.contains("google")) {
            return googleClientId;
        } else if (path.contains("kakao")) {
            return kakaoClientId;
        } else if (path.contains("naver")) {
            return naverClientId;
        }
        return "";
    }

    private String getClientSecret(String path) {
        if (path.contains("google")) {
            return googleClientSecret;
        } else if (path.contains("kakao")) {
            return kakaoClientSecret;
        } else if (path.contains("naver")) {
            return naverClientSecret;
        }
        return "";
    }
}