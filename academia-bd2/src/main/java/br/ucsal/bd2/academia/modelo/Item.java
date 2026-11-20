package br.ucsal.bd2.academia.modelo;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "item")
@NamedQuery(name = "Item.maisVendidos",
        query = "SELECT im.item.nome, SUM(im.quantidade) FROM ItemMatricula im "
              + "GROUP BY im.item.nome ORDER BY SUM(im.quantidade) DESC")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoItem tipo;

    @Column(name = "preco", nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "duracao_meses")
    private Integer duracaoMeses;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Item() {
    }

    public Item(String codigo, String nome, TipoItem tipo, BigDecimal preco, Integer duracaoMeses, Categoria categoria) {
        this.codigo = codigo;
        this.nome = nome;
        this.tipo = tipo;
        this.preco = preco;
        this.duracaoMeses = duracaoMeses;
        this.categoria = categoria;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoItem getTipo() { return tipo; }
    public void setTipo(TipoItem tipo) { this.tipo = tipo; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getDuracaoMeses() { return duracaoMeses; }
    public void setDuracaoMeses(Integer duracaoMeses) { this.duracaoMeses = duracaoMeses; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    @Override
    public String toString() {
        return "Item #" + id + " [" + codigo + "] " + nome + " - " + tipo + " - R$ " + preco + (ativo ? "" : " (inativo)");
    }
}
