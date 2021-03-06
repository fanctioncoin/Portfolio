package com.example.work.service;

import com.example.work.domain.Role;
import com.example.work.domain.User;
import com.example.work.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;   //инжектим поле!
    // 2 способ без @Autowired
//    private final UserRepo userRepo;
//
//    public UserService(UserRepo userRepo) { Спринг видит что есть конструктор будет пытаться заинжектить все это
//        this.userRepo = userRepo;}
    @Autowired
    private MailSender mailSender;

    @Value(" ${hostname}")
    private String hostname;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,LockedException {
        User user= userRepo.findByUsername(username);
        if(user==null){
            throw new UsernameNotFoundException("User not found");
        }
        if (user.getActivationCode() != null ) {
            throw new LockedException("email not activated");
        }


        return user;
    }
    public boolean addUser(User user){
        User userFromDb = userRepo.findByUsername(user.getUsername());
        if(userFromDb !=null){
            return false;
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        userRepo.save(user);
        sendMessage(user);
        return true;
    }

    private void sendMessage(User user) {
        if (!StringUtils.isEmpty(user.getEmail())){
            String message =String.format(
                    "Hello, %s! \n"+
                            "Welcome to Journal STO, Please, visit next link: http://194.195.241.62:8095/activate/%s",
                    user.getUsername(),
//                    hostname,
                    user.getActivationCode()

            );
              mailSender.send(user.getEmail(),"Activation code",message);
        }
    }

    public boolean activateUser(String code) {
       User user = userRepo.findByActivationCode(code);

       if (user ==null){
           return false;
       }
       user.setActivationCode(null);

       userRepo.save(user);

        return true;
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public void saveUser(User user, String username, Map<String, String> form) {
        user.setUsername(username);
        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());
        user.getRoles().clear();
        for (String key : form.keySet()) {
            if(roles.contains(key)){
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepo.save(user);
    }

    public void updateProfile(User user, String password, String email) {
        String userEmail =user.getEmail();
      boolean isEmailChanged = (email !=null && !email.equals(userEmail)) ||
                (userEmail !=null && !userEmail.equals(email));
      if (isEmailChanged){
          user.setEmail(email);
          if(!StringUtils.isEmpty(email)){
              user.setActivationCode(UUID.randomUUID().toString());
          }
      }
      if(!StringUtils.isEmpty(password)){
          user.setPassword(password);
      }
      userRepo.save(user);
      if (isEmailChanged)
      sendMessage(user);
    }
}
