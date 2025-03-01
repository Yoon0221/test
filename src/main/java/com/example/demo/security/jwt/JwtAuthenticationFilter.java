package com.example.demo.security.jwt;

import com.example.demo.entity.enums.Role;
import com.example.demo.base.code.exception.CustomException;
import com.example.demo.base.ApiResponse;
import com.example.demo.base.status.ErrorStatus;
import com.example.demo.entity.base.Token;
import com.example.demo.entity.base.User;
import com.example.demo.entity.enums.Status;
import com.example.demo.repository.TokenRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@WebFilter("/*")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, TokenRepository tokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.info("Incoming request URI: {}", requestURI);

        // 필터를 거치지 않는 경로
        if (isPermittedRequest(requestURI)) {
            logger.debug("Permitted request, skipping authentication filter.");
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}", authorizationHeader);

        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throwException(ErrorStatus.TOKEN_MISSING);
            }

            String token = jwtUtil.getTokenFromHeader(authorizationHeader);
            logger.debug("Extracted token: {}", token);

            // 잘못된 토큰 처리
            if (jwtUtil.isTokenExpired(token)) {
                throwException(ErrorStatus.TOKEN_INVALID_ACCESS_TOKEN);
            }

            String userId = jwtUtil.extractUserIdFromToken(token);

            // 유저가 존재하는지 확인
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> {
                        try {
                            throwException(ErrorStatus.USER_NOT_FOUND);
                        } catch (Exception e) {
                            throw new RuntimeException(e); // 예외 처리
                        }
                        return null; // 이 코드는 실행되지 않지만 구문상 필요
                    });

            // 사용자 상태 확인 -> 활성화 상태에서만 API 사용 가능 ( 필터를 거치지 않는 경로 제외 )
            if (user.getActive() == Status.INACTIVE) {
                throwException(ErrorStatus.USER_ALREADY_LOGOUT);
            }

            if (user.getActive() == Status.WITHDRAWN) {
                throwException(ErrorStatus.USER_ALREADY_WITHDRAWN);
            }

            // 사용 가능한 토큰 확인 ( 해당 사용자의 토큰이 맞는지 확인 )
            Optional<Token> tokenOptional = tokenRepository.findByUser(user);
            if (tokenOptional.isEmpty()) {
                throwException(ErrorStatus.TOKEN_MISSING);  // 토큰이 없거나 일치하지 않음
            }

            Token storedToken = tokenOptional.get();
            if (!storedToken.getAccessToken().equals(token)) {
                throwException(ErrorStatus.TOKEN_NOT_FOUND);  // 저장된 토큰과 현재 토큰이 일치하지 않음
            }

            // 관리자 경로일 경우만 checkAdminRole 메소드 실행
            if (requestURI.startsWith("/api/admin")) {

                if (user.getRole() != Role.ADMIN) {
                    throw new CustomException(ErrorStatus.ADMIN_UNAUTHORIZED_ACCESS);  // 관리자가 아닐 경우 예외 발생
                }
            }

            // 사용자 인증 처리
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            handleAuthenticationError(response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }


    private boolean isPermittedRequest(String requestURI) {
        return requestURI.startsWith("/swagger-ui/") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-resources") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/api/permit/") ||
                requestURI.equals("/favicon.ico") ||
                requestURI.equals("/login");
    }

    private void throwException(ErrorStatus errorStatus) throws Exception {
        throw new Exception(errorStatus.getMessage());
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        ApiResponse<Object> apiResponse = ApiResponse.onFailure(
                ErrorStatus.COMMON_UNAUTHORIZED,
                e.getMessage()
        );

        response.setStatus(ErrorStatus.COMMON_UNAUTHORIZED.getReasonHttpStatus().getHttpStatus().value());
        response.setContentType("application/json; charset=UTF-8");  // UTF-8 인코딩 설정

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }


}