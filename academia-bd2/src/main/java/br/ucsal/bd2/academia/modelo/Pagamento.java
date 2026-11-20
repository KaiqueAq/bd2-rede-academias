package br.ucsal.bd2.academia.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false, unique = true)
    private Matricula matricula;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 15)
    private FormaPagamento formaPagamento;

    @Column(name = "parcelas", nullable = false)
    private int parcelas = 1;

    public Pagamento() {
    }

    public Pagamento(LocalDate dataPagamento, BigDecimal valorPago, FormaPagamento formaPagamento, int parcelas) {
        this.dataPagamento = dataPagamento;
        this.valorPago = valorPago;
        this.formaPagamento = formaPagamento;
        this.parcelas = parcelas;
    }

    public Long getId() { return id; }
    public Matricula getMatricula() { return matricula; }
    public void setMatricula(Matricula matricula) { this.matricula = matricula; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public int getParcelas() { return parcelas; }
    public void setParcelas(int parcelas) { this.parcelas = parcelas; }
}
