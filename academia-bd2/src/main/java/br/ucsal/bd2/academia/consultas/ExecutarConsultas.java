package br.ucsal.bd2.academia.consultas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import br.ucsal.bd2.academia.modelo.Cargo;
import br.ucsal.bd2.academia.modelo.Cliente;
import br.ucsal.bd2.academia.modelo.Item;
import br.ucsal.bd2.academia.modelo.ItemMatricula;
import br.ucsal.bd2.academia.modelo.Matricula;
import br.ucsal.bd2.academia.modelo.StatusMatricula;
import br.ucsal.bd2.academia.modelo.TipoItem;
import br.ucsal.bd2.academia.util.JPAUtil;

public class ExecutarConsultas {

    public static void main(String[] args) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            ConsultasJPQL j = new ConsultasJPQL(em);
            ConsultasCriteria c = new ConsultasCriteria(em);

            titulo("Q1 - Juncao: ultimas matriculas em unidades de Salvador");
            imprimirLinhas(j.q1MatriculasPorCidadeDaUnidade("Salvador"));

            titulo("Q2 - JOIN FETCH: matriculas do cliente 1 com itens");
            for (Matricula m : j.q2MatriculasComItensDoCliente(1L)) {
                StringBuilder sb = new StringBuilder();
                for (ItemMatricula im : m.getItens()) {
                    sb.append(im.getItem().getNome()).append("; ");
                }
                System.out.println("Matricula " + m.getId() + " em " + m.getDataMatricula() + ": " + sb);
            }

            titulo("Q3 - GROUP BY/HAVING: faturamento por unidade acima de R$ 100.000");
            imprimirLinhas(j.q3FaturamentoPorUnidade(new BigDecimal("100000")));

            titulo("Q4 - Subconsulta: itens acima da media de preco do seu tipo (5 primeiros)");
            List<Item> itens = j.q4ItensAcimaDaMediaDoTipo();
            for (Item i : itens.subList(0, Math.min(5, itens.size()))) {
                System.out.println(i);
            }
            System.out.println("... total: " + itens.size());

            titulo("Q5 - Parametros nomeados: matriculas canceladas no 1o semestre de 2025");
            System.out.println("Quantidade: " + j.q5MatriculasPorPeriodoEStatus(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30), StatusMatricula.CANCELADA).size());

            titulo("Q6 - Paginacao: pagina 2 de clientes (5 por pagina)");
            for (Cliente cl : j.q6ClientesPaginados(1, 5)) {
                System.out.println(cl);
            }

            titulo("Q7 - @NamedQuery Cliente.porCidade: clientes de Aracaju");
            System.out.println("Quantidade: " + j.q7ClientesPorCidade("Aracaju").size());

            titulo("Q8 - @NamedQuery Item.maisVendidos (top 10)");
            imprimirLinhas(j.q8ItensMaisVendidos());

            titulo("Q9 - Recebimentos por forma de pagamento (sem canceladas)");
            imprimirLinhas(j.q9RecebimentosPorFormaPagamento());

            titulo("Q10 - Ranking de atendentes por numero de matriculas");
            imprimirLinhas(j.q10RankingVendedores(Cargo.ATENDENTE));

            titulo("C1 - Criteria: clientes da UF SE");
            System.out.println("Quantidade: " + c.c1ClientesPorUf("SE").size());

            titulo("C2 - Criteria: faturamento bruto por categoria");
            imprimirLinhas(c.c2FaturamentoPorCategoria());

            titulo("C3 - Criteria com filtros dinamicos");
            System.out.println("So status ATIVA ............................: "
                    + c.c3BuscarMatriculas(null, StatusMatricula.ATIVA, null, null, null).size());
            System.out.println("Unidade 1 + tipo SERVICO + ano 2025 .........: "
                    + c.c3BuscarMatriculas(1L, null, TipoItem.SERVICO,
                            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).size());
            System.out.println("Sem nenhum filtro (todas) ...................: "
                    + c.c3BuscarMatriculas(null, null, null, null, null).size());
        } finally {
            em.close();
            JPAUtil.fechar();
        }
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("=== " + t + " ===");
    }

    private static void imprimirLinhas(List<Object[]> linhas) {
        for (Object[] l : linhas) {
            System.out.println(Arrays.toString(l));
        }
    }
}
