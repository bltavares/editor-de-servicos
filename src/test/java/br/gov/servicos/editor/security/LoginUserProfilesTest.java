package br.gov.servicos.editor.security;

import br.gov.servicos.editor.usuarios.Usuario;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.omg.CORBA.Object;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginUserProfilesTest {

    private static final String EMAIL = "email@institucional.gov.br";
    public static final String NOME = "Editor de Serviço";
    @Mock
    private GerenciadorPermissoes gerenciadorPermissoes;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoginUserProfiles userProfiles;

    @Before
    public void setUp() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void deveEnviarUserProfileVazioCasoUsuarioNaoEstejaLogado() {
        Object dummyObject = mock(Object.class);
        when(authentication.getPrincipal()).thenReturn(dummyObject);
        assertThat(userProfiles.get(), equalTo(new UserProfile()));
    }

    @Test
    public void deveEnviarUserProfileComDadosDeUsuarioLogado() {
        Usuario usuario = new Usuario().withEmailPrimario(EMAIL).withNome(NOME);
        when(authentication.getPrincipal()).thenReturn(usuario);
        UserProfile actual = userProfiles.get();
        assertThat(actual.getEmail(), equalTo(EMAIL));
        assertThat(actual.getName(), equalTo(NOME));
        assertThat(actual.getId(), equalTo(EMAIL));
    }

}