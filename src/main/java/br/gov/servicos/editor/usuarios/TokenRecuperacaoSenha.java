package br.gov.servicos.editor.usuarios;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Wither
@Table(name="Tokens")
@EqualsAndHashCode
public class TokenRecuperacaoSenha implements Serializable {

    @Id
    @Column(unique = true)
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private Integer tentativasSobrando;

    public TokenRecuperacaoSenha decrementarTentativasSobrando() {
        return this.tentativasSobrando == 0 ? this : this.withTentativasSobrando(this.tentativasSobrando-1);
    }
}