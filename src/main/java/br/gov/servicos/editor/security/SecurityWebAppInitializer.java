package br.gov.servicos.editor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import static br.gov.servicos.editor.security.TipoPermissao.*;
import static org.springframework.http.HttpMethod.*;

public class SecurityWebAppInitializer extends WebSecurityConfigurerAdapter {

    private static final String LOGIN_URL = "/editar/autenticar";
    private static final String API_DESPUBLICAR = "/editar/api/pagina/*/*/despublicar";
    private static final String API_DESCARTAR = "/editar/api/pagina/*/*/descartar";
    private static final String API_PAGINA = "/editar/api/pagina/**";
    private static final String API_NOVO_USUARIO = "/editar/usuarios/usuario";
    private DaoAuthenticationProvider daoAuthenticationProvider;
    private AuthenticationSuccessHandler successHandler;

    public SecurityWebAppInitializer(DaoAuthenticationProvider daoAuthenticationProvider,
                                     AuthenticationSuccessHandler successHandler) {
        this.daoAuthenticationProvider = daoAuthenticationProvider;
        this.successHandler = successHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler();
        accessDeniedHandler.setErrorPage("/editar/acessoNegado");

        http
                .httpBasic()
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_URL))
                .and()

                .formLogin()
                .loginPage("/editar/autenticar")
                .successHandler(successHandler)
                .permitAll()
                .and()

                .logout()
                .logoutUrl("/editar/sair")
                .logoutSuccessUrl("/editar/autenticar?sair")
                .deleteCookies("JSESSIONID", "SESSION")

                .and()
                .authorizeRequests()
                .antMatchers("/editar/autenticar", "/editar/api/ping", "/editar/recuperar-senha").permitAll()
                .antMatchers(POST, API_DESPUBLICAR).hasAnyAuthority(DESPUBLICAR.getNome(), DESPUBLICAR_ORGAO_ESPECIFICO.getNome())
                .antMatchers(POST, API_DESCARTAR).hasAnyAuthority(DESCARTAR.getNome(), DESCARTAR_ORGAO_ESPECIFICO.getNome())
                .antMatchers(DELETE, API_PAGINA).hasAnyAuthority(EXCLUIR.getNome(), EXCLUIR_ORGAO_ESPECIFICO.getNome())
                .antMatchers(PATCH, API_PAGINA).hasAnyAuthority(RENOMEAR.getNome(), RENOMEAR_ORGAO_ESPECIFICO.getNome())
                .antMatchers(PUT, API_PAGINA).hasAnyAuthority(PUBLICAR.getNome(), PUBLICAR_ORGAO_ESPECIFICO.getNome())
                .antMatchers(POST, API_PAGINA).hasAnyAuthority(SALVAR.getNome(), PUBLICAR.getNome(), DESPUBLICAR.getNome())
                .antMatchers(GET, API_NOVO_USUARIO).hasAuthority(CADASTRAR.getNome())
                .antMatchers(POST, API_NOVO_USUARIO).hasAuthority(CADASTRAR.getNome())

                .anyRequest().fullyAuthenticated()

                .and()
                    .exceptionHandling().accessDeniedHandler(accessDeniedHandler)

                .and()
                    .sessionManagement()
                    .invalidSessionUrl("/editar/autenticar?sessao");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(daoAuthenticationProvider);
    }

}
