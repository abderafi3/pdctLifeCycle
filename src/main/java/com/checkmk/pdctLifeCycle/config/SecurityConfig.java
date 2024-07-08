//@Configuration
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .csrf().ignoringAntMatchers("/import/hosts", "/api/hosts/**") // Disable CSRF for specific endpoints
//                .and()
//                .authorizeRequests()
//                .antMatchers("/css/**", "/js/**", "/images/**", "/import/hosts", "/api/hosts/**").permitAll() // Allow access to static resources and API endpoints
//                .anyRequest().authenticated()
//                .and()
//                .formLogin()
//                .loginPage("/login")
//                .permitAll()
//                .and()
//                .logout()
//                .permitAll();
//    }
//}
