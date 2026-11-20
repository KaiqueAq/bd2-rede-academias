package br.ucsal.bd2.academia.modelo;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "item_matricula")
public class ItemMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "desconto", precision = 10, scale = 2)
    private BigDecimal desconto;

    public ItemMatricula() {
    }

    public ItemMatricula(Matricula matricula, Item item, int quantidade, BigDecimal valorUnitario, BigDecimal desconto) {
        this.matricula = matricula;
        this.item = item;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.desconto = desconto;
    }

    public Long getId() { return id; }
    public Matricula getMatricula() { return matricula; }
    public void setMatricula(Matricula matricula) { this.matricula = matricula; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }

    public BigDecimal getValorLiquido() {
        BigDecimal bruto = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        return desconto == null ? bruto : bruto.subtract(desconto);
    }
}
