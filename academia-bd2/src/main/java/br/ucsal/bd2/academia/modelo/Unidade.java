package br.ucsal.bd2.academia.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    @Column(name = "cnpj", nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "data_inauguracao")
    private LocalDate dataInauguracao;

    @Embedded
    private Endereco endereco;

    @OneToMany(mappedBy = "unidade")
    private List<Matricula> matriculas = new ArrayList<>();

    public Unidade() {
    }

    public Unidade(String nome, String cnpj, LocalDate dataInauguracao, Endereco endereco) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.dataInauguracao = dataInauguracao;
        this.endereco = endereco;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public LocalDate getDataInauguracao() { return dataInauguracao; }
    public void setDataInauguracao(LocalDate dataInauguracao) { this.dataInauguracao = dataInauguracao; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public List<Matricula> getMatriculas() { return matriculas; }

    public void adicionarMatricula(Matricula m) {
        matriculas.add(m);
        m.setUnidade(this);
    }
}
