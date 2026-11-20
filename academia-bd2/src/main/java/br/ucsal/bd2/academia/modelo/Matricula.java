package br.ucsal.bd2.academia.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_matricula", nullable = false)
    private LocalDate dataMatricula;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StatusMatricula status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Funcionario vendedor;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemMatricula> itens = new ArrayList<>();

    @OneToOne(mappedBy = "matricula", cascade = CascadeType.ALL)
    private Pagamento pagamento;

    public Long getId() { return id; }
    public LocalDate getDataMatricula() { return dataMatricula; }
    public void setDataMatricula(LocalDate dataMatricula) { this.dataMatricula = dataMatricula; }
    public StatusMatricula getStatus() { return status; }
    public void setStatus(StatusMatricula status) { this.status = status; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Unidade getUnidade() { return unidade; }
    public void setUnidade(Unidade unidade) { this.unidade = unidade; }
    public Funcionario getVendedor() { return vendedor; }
    public void setVendedor(Funcionario vendedor) { this.vendedor = vendedor; }
    public List<ItemMatricula> getItens() { return itens; }
    public Pagamento getPagamento() { return pagamento; }

    public void adicionarItem(Item item, int quantidade, BigDecimal desconto) {
        ItemMatricula im = new ItemMatricula(this, item, quantidade, item.getPreco(), desconto);
        itens.add(im);
    }

    public void definirPagamento(Pagamento p) {
        p.setMatricula(this);
        this.pagamento = p;
    }

    public BigDecimal getValorTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemMatricula im : itens) {
            total = total.add(im.getValorLiquido());
        }
        return total;
    }
}
