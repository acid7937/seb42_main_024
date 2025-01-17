package com.main.server.auth.filter;

import com.main.server.auth.jwt.JwtTokenizer;
import com.main.server.auth.util.CustomAuthorityUtils;
import com.main.server.member.entity.Member;
import com.main.server.member.repository.MemberRepository;
import com.main.server.member.service.MemberService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter {

    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils authorityUtils;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        try {
            Map<String, Object> claims = verifyJws(request, response);
            setAuthenticationToContext(claims);
        } catch (Exception e) {
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String authorization = request.getHeader("Authorization");
        String refresh = request.getHeader("Refresh");

        return (authorization == null || !authorization.startsWith("Bearer"))
                && refresh == null;
    }

    private Map<String, Object> verifyJws(HttpServletRequest request,
                                          HttpServletResponse response) {
        if (request.getHeader("Authorization") != null) {
            String jws = request.getHeader("Authorization").replace("Bearer", "");
            Claims claims = jwtTokenizer.getClaims(jws).getBody();
            return claims;
        }

        String jws = request.getHeader("Refresh");
        Member member = null;
//                memberRepository.findByEmail(jwtTokenizer.getUsername(jws))
//                .orElseThrow(() -> new BusinessLogicException(ExceptionCode.INVALID_TOKEN));

        String accessToken =
                jwtTokenizer.delegateAccessToken(
                        member.getEmail(),
                        authorityUtils.createRoles(member.getEmail()));
        response.setHeader("Authorization", "Bearer " + accessToken);

        Claims claims = jwtTokenizer.getClaims(accessToken).getBody();
        return claims;

    }

    private void setAuthenticationToContext(Map<String, Object> claims) {
        String username = (String)claims.get("username");
        List<GrantedAuthority> authorities = authorityUtils.createAuthorities((List)claims.get("roles"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
