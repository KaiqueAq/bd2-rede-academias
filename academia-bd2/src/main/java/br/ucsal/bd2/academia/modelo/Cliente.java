package br.ucsal.bd2.academia.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("CLIENTE")
@NamedQuery(name = "Cliente.porCidade",
        query = "SELECT c FROM Cliente c WHERE LOWER(TRIM(c.endereco.cidade)) = LOWER(:cidade) ORDER BY c.nome")
public class Cliente extends Pessoa {

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @OneToMany(mappedBy = "cliente")
    private List<Matricula> matriculas = new ArrayList<>();

    public LocalDate getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDate dataCadastro) { this.dataCadastro = dataCadastro; }
    public List<Matricula> getMatriculas() { return matriculas; }

    @Override
    public String toString() {
        return "Cliente #" + getId() + " - " + getNome() + " (" + getCpf() + ") - "
                + (getEndereco() == null ? "sem endereco" : getEndereco().getCidade() + "/" + getEndereco().getUf());
    }
}
