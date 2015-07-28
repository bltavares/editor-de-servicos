package br.gov.servicos.editor.servicos;

import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.*;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;
import static org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.NOTRACK;
import static org.eclipse.jgit.lib.Constants.*;

@Slf4j
@Component
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class Cartas {

    File repositorioCartasLocal;
    boolean fazerPush;

    @Autowired
    public Cartas(File repositorioCartasLocal, @Value("${flags.git.push}") boolean fazerPush) {
        this.repositorioCartasLocal = repositorioCartasLocal;
        this.fazerPush = fazerPush;
    }

    @SneakyThrows
    public Optional<String> conteudoServicoV2(String id) {
        return conteudoServico(id, leitorDeConteudo(id, "v2"));
    }

    @SneakyThrows
    public Optional<String> conteudoServicoV3(String id) {
        return conteudoServico(id, leitorDeConteudo(id, "v3"));
    }

    public Optional<String> conteudoServico(String id, Supplier<Optional<String>> leitor) {
        return executaNoBranchDoServico(id, leitor);
    }

    public Supplier<Optional<String>> leitorDeConteudo(String id, String versao) {
        return () -> {
            File arquivo = caminhoRelativo(id, versao).toFile();
            if (arquivo.exists()) {
                log.info("Arquivo {} encontrado", arquivo);
                return ler(arquivo);
            }

            log.info("Arquivo {} não encontrado", arquivo);
            return empty();
        };
    }

    public Optional<Metadados> ultimaRevisaoV2(String id) {
        return ultimaRevisao(id, "v2");
    }

    public Optional<Metadados> ultimaRevisaoV3(String id) {
        return ultimaRevisao(id, "v3");
    }

    private Optional<Metadados> ultimaRevisao(final String id, final String versao) {
        return comRepositorioAberto(new Function<Git, Optional<Metadados>>() {

            @Override
            @SneakyThrows
            public Optional<Metadados> apply(Git git) {
                LogCommand revs;
                Ref branchRef = git.getRepository().getRef(R_HEADS + id);

                if (branchRef != null) {
                    // temos uma branch para o servico
                    revs = git.log().add(branchRef.getObjectId());

                } else {
                    // pegamos o ultimo commit no master
                    revs = git.log().addPath(caminhoRelativo(caminhoRelativo(id, versao)));
                }

                for (RevCommit commit : revs.setMaxCount(1).call()) {

                    return of(new Metadados()
                                    .withRevisao(commit.getId().getName())
                                    .withAutor(commit.getAuthorIdent().getName())
                                    .withHorario(commit.getAuthorIdent().getWhen())
                    );
                }
                return empty();
            }
        });
    }

    @SneakyThrows
    private Optional<String> ler(File arquivo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), defaultCharset()))) {
            return of(reader.lines().collect(joining("\n")));
        }
    }

    @SneakyThrows
    public void salvarServicoV2(String id, Document doc, User usuario) {
        comRepositorioAberto(git -> {

            pull(git);

            try {
                return executaNoBranchDoServico(id, () -> {
                    Path caminho = caminhoRelativo(id, "v2");
                    Path dir = caminho.getParent();

                    if (dir.toFile().mkdirs()) {
                        log.debug("Diretório {} não existia e foi criado", dir);
                    } else {
                        log.debug("Diretório {} já existia e não precisou ser criado", dir);
                    }

                    String mensagem = format("%s '%s'", caminho.toFile().exists() ? "Altera" : "Cria", id);

                    escreve(doc, caminho);
                    add(git, caminho);
                    commit(git, mensagem, usuario);

                    return null;
                });

            } finally {
                push(git, id);
            }
        });
    }

    @SneakyThrows
    public void salvarServicoV3(String id, String doc, User usuario) {
        comRepositorioAberto(git -> {

            pull(git);

            try {
                return executaNoBranchDoServico(id, () -> {
                    Path caminho = caminhoRelativo(id, "v3");
                    Path dir = caminho.getParent();

                    if (dir.toFile().mkdirs()) {
                        log.debug("Diretório {} não existia e foi criado", dir);
                    } else {
                        log.debug("Diretório {} já existia e não precisou ser criado", dir);
                    }

                    String mensagem = format("%s '%s'", caminho.toFile().exists() ? "Altera" : "Cria", id);

                    escreveV3(doc, caminho);
                    add(git, caminho);
                    commit(git, mensagem, usuario);

                    return null;
                });

            } finally {
                push(git, id);
            }
        });
    }

    @SneakyThrows
    private void push(Git git, String id) {
        log.info("git push: {} ({})", git.getRepository().getBranch(), git.getRepository().getRepositoryState());
        if (fazerPush && !id.equals("novo")) {
            git.push()
                    .setRemote(DEFAULT_REMOTE_NAME)
                    .setRefSpecs(new RefSpec(id + ":" + id))
                    .setProgressMonitor(new TextProgressMonitor())
                    .call();
        } else {
            log.info("Envio de alterações ao Github desligado (FLAGS_GIT_PUSH=false)");
        }
    }

    @SneakyThrows
    private void pull(Git git) {
        log.info("git pull: {} ({})", git.getRepository().getBranch(), git.getRepository().getRepositoryState());
        git.pull()
                .setRebase(true)
                .setStrategy(MergeStrategy.THEIRS)
                .setProgressMonitor(new TextProgressMonitor())
                .call();
    }

    @SneakyThrows
    private void commit(Git git, String mensagem, User usuario) {
        PersonIdent ident = new PersonIdent(usuario.getUsername(), "servicos@planejamento.gov.br");
        log.debug("git commit: {} ({}): '{}', {}",
                git.getRepository().getBranch(),
                git.getRepository().getRepositoryState(),
                mensagem,
                ident
        );

        git.commit()
                .setMessage(mensagem)
                .setCommitter(ident)
                .setAuthor(ident)
                .call();
    }

    @SneakyThrows
    private void add(Git git, Path path) {
        String pattern = caminhoRelativo(path);
        log.debug("git add: {} ({})", git.getRepository().getBranch(), git.getRepository().getRepositoryState(), pattern);

        git.add()
                .addFilepattern(pattern)
                .call();
    }

    private String caminhoRelativo(Path path) {
        return repositorioCartasLocal.toPath().relativize(path).toString();
    }

    @SneakyThrows
    private <T> T comRepositorioAberto(Function<Git, T> fn) {
        try (Git git = Git.open(repositorioCartasLocal)) {
            synchronized (Cartas.class) {
                return fn.apply(git);
            }
        }
    }

    @SneakyThrows
    private <T> T executaNoBranchDoServico(String id, Supplier<T> supplier) {
        return comRepositorioAberto(git -> {
            checkout(git, id);
            try {
                return supplier.get();
            } finally {
                checkoutMaster(git);
            }
        });
    }

    @SneakyThrows
    private void checkoutMaster(Git git) {
        log.debug("git checkout master: {} ({})", git.getRepository().getBranch(), git.getRepository().getRepositoryState());
        git.checkout().setName(MASTER).call();
    }

    @SneakyThrows
    private void checkout(Git git, String id) {
        log.debug("git checkout: {} ({})", git.getRepository().getBranch(), git.getRepository().getRepositoryState(), id);

        git.checkout()
                .setName(id)
                .setStartPoint(R_HEADS + MASTER)
                .setUpstreamMode(NOTRACK)
                .setCreateBranch(!branchExiste(git, id))
                .call();
    }

    @SneakyThrows
    private boolean branchExiste(Git git, String id) {
        boolean resultado = git
                .branchList()
                .call()
                .stream()
                .anyMatch(b -> b.getName().equals(R_HEADS + id));

        log.debug("git branch {} já existe? {}", id, resultado);
        return resultado;
    }

    private Path caminhoRelativo(String id, String versao) {
        return Paths.get(repositorioCartasLocal.getAbsolutePath(), "cartas-servico", versao, "servicos", id + ".xml");
    }

    @SneakyThrows
    private void escreve(Document document, Path arquivo) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(arquivo.toFile()), "UTF-8")) {
            writer.write(document.toString().replace("xsi:schemalocation", "xsi:schemaLocation")); // TODO: Jsoup faz lowercase de atributos :(
        }
        log.debug("Arquivo '{}' modificado", arquivo.getFileName());
    }

    @SneakyThrows
    private void escreveV3(String document, Path arquivo) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(arquivo.toFile()), "UTF-8")) {
            writer.write(document);
        }
        log.debug("Arquivo '{}' modificado", arquivo.getFileName());
    }

    @SneakyThrows
    private Optional<ReflogEntry> reflogMaisRecente(Git git, String id) {
        return ofNullable(git.getRepository()
                .getReflogReader(id))
                .map(new Function<ReflogReader, ReflogEntry>() {
                    @Override
                    @SneakyThrows
                    public ReflogEntry apply(ReflogReader reflogReader) {
                        return reflogReader.getLastEntry();
                    }
                });
    }

}
