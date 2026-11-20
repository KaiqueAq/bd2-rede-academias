package br.ucsal.bd2.academia.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import br.ucsal.bd2.academia.dao.GenericDAO;
import br.ucsal.bd2.academia.modelo.Categoria;
import br.ucsal.bd2.academia.modelo.Cliente;
import br.ucsal.bd2.academia.modelo.Endereco;
import br.ucsal.bd2.academia.modelo.Item;
import br.ucsal.bd2.academia.modelo.Sexo;
import br.ucsal.bd2.academia.modelo.TipoItem;
import br.ucsal.bd2.academia.util.JPAUtil;

public class AppConsole {

    private final Scanner in = new Scanner(System.in, "UTF-8");
    private final EntityManager em = JPAUtil.getEntityManager();
    private final GenericDAO<Cliente, Long> clienteDAO = new GenericDAO<>(Cliente.class, em);
    private final GenericDAO<Item, Long> itemDAO = new GenericDAO<>(Item.class, em);
    private final GenericDAO<Categoria, Long> categoriaDAO = new GenericDAO<>(Categoria.class, em);

    public static void main(String[] args) {
        AppConsole app = new AppConsole();
        try {
            app.menuPrincipal();
        } finally {
            app.em.close();
            JPAUtil.fechar();
        }
    }

    private void menuPrincipal() {
        while (true) {
            System.out.println();
            System.out.println("===== REDE DE ACADEMIAS =====");
            System.out.println("1 - Clientes");
            System.out.println("2 - Itens (planos, servicos e produtos)");
            System.out.println("0 - Sair");
            String op = ler("Opcao");
            if (op.equals("1")) menuClientes();
            else if (op.equals("2")) menuItens();
            else if (op.equals("0")) return;
        }
    }

    private void menuClientes() {
        while (true) {
            System.out.println();
            System.out.println("--- CLIENTES (" + clienteDAO.contar() + " cadastrados) ---");
            System.out.println("1 - Listar (paginado)   2 - Buscar por id   3 - Inserir");
            System.out.println("4 - Alterar endereco/telefone   5 - Remover   0 - Voltar");
            String op = ler("Opcao");
            try {
                if (op.equals("1")) listarClientes();
                else if (op.equals("2")) System.out.println(buscarCliente());
                else if (op.equals("3")) inserirCliente();
                else if (op.equals("4")) alterarCliente();
                else if (op.equals("5")) removerCliente();
                else if (op.equals("0")) return;
            } catch (PersistenceException | IllegalArgumentException e) {
                em.clear();
                System.out.println("Operacao nao realizada: " + causaRaiz(e));
            }
        }
    }

    private void listarClientes() {
        int pagina = 0;
        while (true) {
            List<Cliente> lista = clienteDAO.listarPaginado(pagina, 10);
            for (Cliente c : lista) System.out.println(c);
            if (lista.size() < 10 || !ler("Proxima pagina? (s/n)").equalsIgnoreCase("s")) return;
            pagina++;
        }
    }

    private Cliente buscarCliente() {
        Cliente c = clienteDAO.buscarPorId(Long.valueOf(ler("Id do cliente")));
        if (c == null) throw new IllegalArgumentException("cliente nao encontrado");
        return c;
    }

    private void inserirCliente() {
        Cliente c = new Cliente();
        c.setNome(ler("Nome"));
        c.setCpf(ler("CPF (000.000.000-00)"));
        c.setEmail(lerOpcional("E-mail"));
        c.setTelefone(lerOpcional("Telefone"));
        String nasc = lerOpcional("Data de nascimento (AAAA-MM-DD)");
        c.setDataNascimento(nasc == null ? null : LocalDate.parse(nasc));
        String sexo = lerOpcional("Sexo (M/F)");
        c.setSexo(sexo == null ? null : (sexo.equalsIgnoreCase("F") ? Sexo.FEMININO : Sexo.MASCULINO));
        c.setEndereco(lerEndereco(new Endereco()));
        c.setDataCadastro(LocalDate.now());
        clienteDAO.salvar(c);
        System.out.println("Cliente salvo com id " + c.getId());
    }

    private void alterarCliente() {
        Cliente c = buscarCliente();
        System.out.println(c);
        String tel = lerOpcional("Novo telefone (Enter mantem)");
        if (tel != null) c.setTelefone(tel);
        Endereco e = c.getEndereco() == null ? new Endereco() : c.getEndereco();
        c.setEndereco(lerEndereco(e));
        clienteDAO.atualizar(c);
        System.out.println("Cliente atualizado: " + c);
    }

    private void removerCliente() {
        Cliente c = buscarCliente();
        if (!ler("Remover " + c.getNome() + "? (s/n)").equalsIgnoreCase("s")) return;
        clienteDAO.remover(c.getId());
        System.out.println("Cliente removido.");
    }

    private Endereco lerEndereco(Endereco e) {
        System.out.println("Endereco (Enter mantem o valor atual)");
        String v;
        if ((v = lerOpcional("Logradouro [" + e.getLogradouro() + "]")) != null) e.setLogradouro(v);
        if ((v = lerOpcional("Numero [" + e.getNumero() + "]")) != null) e.setNumero(v);
        if ((v = lerOpcional("Bairro [" + e.getBairro() + "]")) != null) e.setBairro(v);
        if ((v = lerOpcional("Cidade [" + e.getCidade() + "]")) != null) e.setCidade(v);
        if ((v = lerOpcional("UF [" + e.getUf() + "]")) != null) e.setUf(v.toUpperCase());
        if ((v = lerOpcional("CEP [" + e.getCep() + "]")) != null) e.setCep(v);
        return e;
    }

    private void menuItens() {
        while (true) {
            System.out.println();
            System.out.println("--- ITENS (" + itemDAO.contar() + " cadastrados) ---");
            System.out.println("1 - Listar   2 - Buscar por id   3 - Inserir");
            System.out.println("4 - Alterar preco/ativo   5 - Remover   0 - Voltar");
            String op = ler("Opcao");
            try {
                if (op.equals("1")) for (Item i : itemDAO.listarPaginado(0, 200)) System.out.println(i);
                else if (op.equals("2")) System.out.println(buscarItem());
                else if (op.equals("3")) inserirItem();
                else if (op.equals("4")) alterarItem();
                else if (op.equals("5")) removerItem();
                else if (op.equals("0")) return;
            } catch (PersistenceException | IllegalArgumentException e) {
                em.clear();
                System.out.println("Operacao nao realizada: " + causaRaiz(e));
            }
        }
    }

    private Item buscarItem() {
        Item i = itemDAO.buscarPorId(Long.valueOf(ler("Id do item")));
        if (i == null) throw new IllegalArgumentException("item nao encontrado");
        return i;
    }

    private void inserirItem() {
        for (Categoria c : categoriaDAO.listarTodos()) System.out.println(c.getId() + " - " + c.getNome());
        Categoria cat = categoriaDAO.buscarPorId(Long.valueOf(ler("Id da categoria")));
        if (cat == null) throw new IllegalArgumentException("categoria nao encontrada");
        TipoItem tipo = TipoItem.valueOf(ler("Tipo (PLANO, SERVICO, PRODUTO)").toUpperCase());
        String meses = tipo == TipoItem.PLANO ? ler("Duracao em meses") : null;
        Item i = new Item(ler("Codigo (ex.: PR050)"), ler("Nome"), tipo, new BigDecimal(ler("Preco (ex.: 49.90)")),
                meses == null ? null : Integer.valueOf(meses), cat);
        itemDAO.salvar(i);
        System.out.println("Item salvo: " + i);
    }

    private void alterarItem() {
        Item i = buscarItem();
        System.out.println(i);
        String preco = lerOpcional("Novo preco (Enter mantem)");
        if (preco != null) i.setPreco(new BigDecimal(preco));
        String ativo = lerOpcional("Ativo? (s/n, Enter mantem)");
        if (ativo != null) i.setAtivo(ativo.equalsIgnoreCase("s"));
        itemDAO.atualizar(i);
        System.out.println("Item atualizado: " + i);
    }

    private void removerItem() {
        Item i = buscarItem();
        if (!ler("Remover " + i.getNome() + "? (s/n)").equalsIgnoreCase("s")) return;
        itemDAO.remover(i.getId());
        System.out.println("Item removido.");
    }

    private String ler(String rotulo) {
        System.out.print(rotulo + ": ");
        return in.nextLine().trim();
    }

    private String lerOpcional(String rotulo) {
        String v = ler(rotulo);
        return v.isEmpty() ? null : v;
    }

    private static String causaRaiz(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        String msg = e.getMessage();
        if (msg != null && msg.contains("violates foreign key")) {
            return "o registro possui matriculas vinculadas e nao pode ser removido.";
        }
        return msg;
    }
}
