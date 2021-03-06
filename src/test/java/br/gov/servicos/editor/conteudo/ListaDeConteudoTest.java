package br.gov.servicos.editor.conteudo;

import br.gov.servicos.editor.fixtures.RepositorioCartasBuilder;
import br.gov.servicos.editor.fixtures.RepositorioConfigParaTeste;
import br.gov.servicos.editor.git.Importador;
import br.gov.servicos.editor.git.Metadados;
import br.gov.servicos.editor.git.RepositorioGit;
import com.google.common.cache.Cache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCache;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

import static br.gov.servicos.editor.config.CacheConfig.METADADOS;
import static br.gov.servicos.editor.conteudo.TipoPagina.*;
import static java.util.Locale.getDefault;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
@RunWith(MockitoJUnitRunner.class)
public class ListaDeConteudoTest {

    @Mock
    RepositorioGit repositorioGit;

    public RepositorioConfigParaTeste repo = new RepositorioConfigParaTeste();

    @Mock
    ConteudoVersionadoFactory factory;

    @Mock
    Importador importador;

    @Mock
    ConteudoVersionado carta;

    @Mock
    ConteudoVersionado paginaVersionada;

    @Mock
    CacheManager cacheManager;

    ListaDeConteudo listaDeConteudo;

    @Before
    public void setUp() throws Exception {
        repo.reset();

        new RepositorioCartasBuilder(repo.getLocalCloneRepositorio())
                .touchCarta("id-qualquer")
                .touchOrgao("outro-id-qualquer")
                .touchPaginaTematica("pg-tematica")
                .buildSemGit();

        given(repositorioGit.getCaminhoAbsoluto()).willReturn(repo.getLocalCloneRepositorio());
        given(factory.pagina(anyString(), Matchers.any(TipoPagina.class)))
                .willReturn(paginaVersionada);
        given(factory.pagina(anyString(), eq(SERVICO))).willReturn(carta);

        listaDeConteudo = new ListaDeConteudo(importador, repositorioGit, factory, cacheManager, true);
    }

    @Test
    public void deveListarDiretorioDeCartas() throws Exception {
        Metadados m1 = new Metadados().withId("id-qualquer");

        given(repositorioGit.branches()).will(i -> Stream.empty());
        given(paginaVersionada.getMetadados()).willReturn(new Metadados());
        given(carta.getMetadados()).willReturn(m1);

        Collection<Metadados> metadados = listaDeConteudo.listar();

        assertThat(metadados, hasItem(m1));
    }

    @Test(expected = FileNotFoundException.class)
    public void jogaExcecaoCasoDiretorioDeCartasNaoExista() throws Exception {
        given(repositorioGit.getCaminhoAbsoluto()).willReturn(Paths.get("/caminho/nao/existente"));

        listaDeConteudo.listar();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void forcaAtualizacaoDoCacheAoInicializar() throws Exception {
        Cache cache = mock(Cache.class);

        Metadados m1 = new Metadados().withId("id-qualquer");
        Metadados m2 = new Metadados().withId("outro-id-qualquer");

        given(importador.isImportadoComSucesso()).willReturn(true);
        given(repositorioGit.branches()).will(i -> Stream.empty());
        given(carta.getMetadados()).willReturn(m1);
        given(paginaVersionada.getMetadados()).willReturn(m2);

        given(cacheManager.getCache(METADADOS)).willReturn(new GuavaCache(METADADOS, cache));

        listaDeConteudo.esquentarCacheDeMetadados();

        verify(cache).put("id-qualquer", m1);
        verify(cache).put("outro-id-qualquer", m2);
    }
}
