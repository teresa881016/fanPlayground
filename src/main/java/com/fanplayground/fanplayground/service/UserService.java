package com.fanplayground.fanplayground.service;


import com.fanplayground.fanplayground.dto.SignupRequestDto;
import com.fanplayground.fanplayground.dto.UserUpdateRequestDto;
import com.fanplayground.fanplayground.dto.UserUpdateResponseDto;
import com.fanplayground.fanplayground.dto.MessageDto;
import com.fanplayground.fanplayground.entity.User;
import com.fanplayground.fanplayground.entity.UserRoleEnum;
import com.fanplayground.fanplayground.exception.DuplicateUsernameException;
import com.fanplayground.fanplayground.jwt.SecurityUtil;
import com.fanplayground.fanplayground.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ADMIN_TOKEN
    private final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";

    public ResponseEntity<MessageDto> signup(SignupRequestDto requestDto) {

        String username = requestDto.getUsername();
        String nickname = requestDto.getNickName();

        if(!requestDto.getPassword1().equals(requestDto.getPassword2())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String password = passwordEncoder.encode(requestDto.getPassword1());
        //String email = requestDto.getEmail();

        // username 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username);
        if(checkUsername.isPresent()){
            throw new DuplicateUsernameException("중복된 username 입니다.");
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if(requestDto.isAdmin()){
            if(!ADMIN_TOKEN.equals(requestDto.getAdminToken())){
                throw new IllegalArgumentException("관리자 암호가 틀려 등록이 불가능 합니다.");
            }
            role = UserRoleEnum.ADMIN;
        }

        // 사용자 등록
        User user = new User(username, password, role,nickname);
        userRepository.save(user);

        MessageDto msg = new MessageDto("회원가입 성공");
        return new ResponseEntity<>(msg, null, HttpStatus.OK);
    }
    public ResponseEntity<MessageDto> escape(){

        String username = SecurityUtil.getPrincipal().get().getUsername();

        userRepository.delete(userRepository.findByUsername(username).orElse(null));
        MessageDto msg = new MessageDto("회원 삭제 성공");

        // 해당 사용자(username)가 작성한 게시글인지 확인
        // setSubject(username)
        //post 삭제
        return new ResponseEntity<>(msg, null, HttpStatus.OK);
    }

    @Transactional
    public UserUpdateResponseDto update(UserUpdateRequestDto requestDto) {

            Long user_id = SecurityUtil.getPrincipal().get().getId();

            User user = userRepository.findById(user_id).orElseThrow(() ->
                    new IllegalArgumentException("회원을 찾을 수 없습니다.")
            );
            // 사용자 ROLE 확인
            UserRoleEnum role = UserRoleEnum.USER;
            if(user.getRole().equals(UserRoleEnum.ADMIN))
                throw new IllegalArgumentException("사용자 닉네임을 업데이트 할 수 있는 권한인 없습니다.");

            user.setNickName(requestDto.getNickName());

            return new UserUpdateResponseDto(user.getNickName(),"회원정보가 수정되었습니다");


    }
}
