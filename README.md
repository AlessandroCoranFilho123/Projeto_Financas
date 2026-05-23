# Smart Finance

Smart Finance é um aplicativo desktop para controle financeiro pessoal, feito em JavaFX com persistência local em SQLite. O projeto foi desenvolvido para fins de estudo, explorando interface desktop, organização em camadas, acesso a banco de dados, exportação de dados, testes automatizados e primeiras práticas de segurança.

## Funcionalidades

- Cadastro, edição e exclusão de transações.
- Separação entre entradas e saídas.
- Categorias financeiras com regras específicas para metas.
- Comentários em transações.
- Busca de transações por descrição, comentário, categoria, tipo e data.
- Filtros por tipo, categoria e período.
- Exportação de transações em `.csv` e `.txt`.
- Criação e acompanhamento de metas financeiras.
- Registro de compras parceladas no cartão de crédito, com valor mensal calculado e alerta de vencimento.
- Anotações de empréstimos separadas entre "Emprestei pra alguém" e "Pedi emprestado".
- Dashboard com saldo disponível, receitas do mês, despesas do mês, transações recentes e metas.
- Interface com tema claro e escuro.
- Persistência de dados em SQLite.
- Otimizações de banco com índices e busca textual FTS5.
- Base inicial de segurança com criptografia AES-GCM e chave fora do código/banco pelo Windows Credential Manager.

## Instalação

Disponível somente para Windows.

Baixe o instalador `SmartFinance-2.1.0.exe` na área de releases do repositório:

<div align="center">

[![GitHub](https://img.shields.io/badge/GitHub-%23121011.svg?logo=github&logoColor=white)](https://github.com/AlessandroCoranFilho123/smart-finance)
</div>
Depois, siga o assistente de instalação:

<p align="center">
  <img src="./src/main/resources/readme/instalacao-p1.png" width="800" alt="Primeira etapa da instalação">
  <img src="./src/main/resources/readme/instalacao-p2.png" width="800" alt="Segunda etapa da instalação">
  <img src="./src/main/resources/readme/instalacao-p3.png" width="800" alt="Terceira etapa da instalação">
</p>

## Interface

<p align="center">
<h6 align="center">Tela inicial</h6>
  <img src="./src/main/resources/readme/tela-inicial.png" width="800" alt="Tela Inicial">
  <h6 align="center">Tela de transações</h6>
  <img src="./src/main/resources/readme/transacoes.png" width="800" alt="Tela de transações">
  <h6 align="center">Tela de metas</h6>
  <img src="./src/main/resources/readme/metas.png" width="800" alt="Tela de metas">
  <h6 align="center">Janela de Nova Transação</h6>
  <img src="./src/main/resources/readme/nova-transacao.png" width="800" alt="Janela de nova transação">
  <h6 align="center">Janela de Nova Meta</h6>
  <img src="./src/main/resources/readme/nova-meta.png" width="800" alt="Janela de nova meta">
</p>

## Dados e segurança

O banco de dados é local. Em ambiente instalado, os dados ficam em:

```text
%APPDATA%\SmartFinance\financas.db
```

O projeto usa `PreparedStatement` nas operações de banco, `PRAGMA foreign_keys`, índices para consultas frequentes e FTS5 para busca textual. Compras no cartão e empréstimos ficam em tabelas próprias, sem transformar automaticamente esses registros em transações. Também há uma base de criptografia com AES-256-GCM e gerenciamento de chave pelo Windows Credential Manager.

## Desenvolvimento

Para executar o projeto em ambiente de desenvolvimento:

```bash
mvn javafx:run
```

Para rodar os testes:

```bash
mvn test
```

Para empacotar dependências usadas no instalador:

```bash
mvn package
```

## Tecnologias utilizadas

- Java 21
- JavaFX 21.0.9
- CSS
- Maven
- SQLite JDBC 3.45.3.0
- JNA 5.14.0
- JUnit 5.10.1
- Mockito 5.8.0
- jlink / jpackage

## Status do projeto

Projeto acadêmico em evolução. A versão atual foca em controle de transações, metas, cartão de crédito, empréstimos, persistência local, melhorias visuais, exportação de dados, otimizações de banco e fundação inicial de segurança.
