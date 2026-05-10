# EnergyTrack API — Plano de Desenvolvimento

> Projeto pessoal de prática e recuperação de memória muscular em Spring Boot, alinhado às tecnologias da vaga de Développeur Java na Conserto (Bordeaux).

---

## 1. Contexto e Objetivos

### Por que este projeto existe

Este é um projeto de **aprendizado deliberado**. O objetivo não é entregar um produto, mas:

1. **Reativar memória muscular** em Spring Boot — escrevendo cada linha à mão.
2. **Aprender e relembrar** aspectos importantes do ecossistema Spring moderno.
3. **Aprender Spring Modulith** do zero, praticando arquitetura modular.
4. **Praticar Gradle com Kotlin DSL** como build tool moderno.
5. **Praticar OpenAPI spec-first** como padrão de design de APIs em DSIs grandes.
6. **Construir uma referência pessoal** consultável no futuro.

### Vaga de referência

Vaga: **Développeur Java — Conserto Bordeaux**

Stack mencionada: Java, Spring (Boot, MVC, Batch, Security), Angular, JUnit, Cypress, Selenium, JMeter, AWS, Docker, Kubernetes, Scrum/SAFe. Setor do cliente: **energia**.

Diferenciais valorizados: TDD, autonomia técnica, qualidade de código.

### Caso de uso: EnergyTrack

API para gerenciamento de consumo energético de clientes industriais. Domínio escolhido por:

- Ser realista e aderente ao setor mencionado na vaga (energia).
- Ter regras de negócio interessantes (não é CRUD genérico).
- Permitir exercitar **todos** os pilares: MVC, Batch, Security, eventos, Modulith, OpenAPI.

**Funcionalidades principais:**

- Cadastro de clientes industriais e seus medidores (meters).
- Registro periódico de leituras de consumo.
- Cálculo mensal de faturas via Spring Batch.
- Geração de alertas quando o consumo ultrapassa thresholds.
- Autenticação JWT com perfis (ADMIN, OPERATOR, CLIENT).

---

## 2. Stack Técnica

| Camada | Escolha | Motivo |
|---|---|---|
| Linguagem | Java 21 (LTS) | Versão moderna, virtual threads, records maduros |
| Framework | Spring Boot 3.3+ | Última geração, Jakarta EE |
| Arquitetura | Spring Modulith 1.3+ | Aprendizado-chave do projeto |
| Build | Gradle 8.x + Kotlin DSL | Sintaxe moderna |
| Versionamento de deps | `libs.versions.toml` | Catálogo centralizado |
| **Design de API** | **OpenAPI 3.1 spec-first** | **Padrão em DSIs grandes; força pensar contrato antes do código** |
| Geração de código | `openapi-generator-gradle-plugin` | Gera interfaces + DTOs a partir do YAML |
| Persistência | PostgreSQL 16 + Flyway | Migrations versionadas desde o dia 1 |
| ORM | Spring Data JPA / Hibernate | Padrão da indústria |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers | Stack moderno; PostgreSQL real nos testes |
| Testes de contrato | Schemathesis ou validador OpenAPI | Validar implementação contra a spec |
| Segurança | Spring Security + JWT | Vaga menciona Spring Security |
| Batch | Spring Batch | Vaga menciona explicitamente |
| Observabilidade | Actuator + Micrometer + JSON logs | Padrão prod |
| Containerização | Docker (multi-stage) + docker-compose | Vaga menciona Docker |
| **CI** | **GitHub Actions** | **Hospedagem e CI no GitHub** |

---

## 3. Princípios de Trabalho com Claude Code

Para preservar o objetivo de **memória muscular**, o contrato com o Claude Code é:

1. **Eu digito 100% do código.** O Claude Code lê, comenta, sugere — nunca escreve arquivo no meu lugar.
2. **Antes de cada classe nova**, peço explicação dos conceitos envolvidos.
   - Exemplo: *"Explique a diferença entre `@Component`, `@Service` e `@Repository` antes de eu criar o `CustomerService`."*
3. **Após escrever**, peço revisão crítica como se fosse um Pull Request.
4. **TDD obrigatório** nas camadas de domínio e aplicação. Teste primeiro, sempre.
5. **Spec-first obrigatório** na camada web. YAML primeiro, controller depois.
6. **Ao final de cada fase**, peço um quiz de 5 perguntas técnicas para fixação.
7. **Quando travar**, peço dicas em vez de soluções (estilo socrático).

---

## 4. Estratégia OpenAPI Spec-First

Esta é uma decisão arquitetural central do projeto. Em vez de escrever controllers Spring e gerar a spec a partir das anotações (code-first), fazemos o inverso:

1. **Escreve-se o `openapi.yaml` à mão**, descrevendo o contrato.
2. O **plugin Gradle gera no build**: interfaces dos controllers, records dos DTOs, enums.
3. **Implemento as interfaces geradas** com a lógica Spring.
4. **Testes de contrato** validam que a implementação respeita a spec.

### Por que spec-first

- É o padrão em **grandes DSIs** francesas e europeias — exatamente o público da Conserto.
- Permite que back e front trabalhem em paralelo a partir do mesmo contrato (a equipe da vaga tem 3 backs e 1 front).
- O contrato vira a **fonte da verdade**, versionada em Git.
- Força a pensar em status codes, schemas, exemplos e erros **antes** de codar.
- Permite gerar cliente TypeScript/Angular automaticamente para um futuro front.
- Quebra o vício de "controller primeiro, contrato depois".

### Estrutura dos arquivos OpenAPI

```
api-spec/
├── openapi.yaml                    # entrada raiz, monta tudo via $ref
├── paths/
│   ├── customers.yaml
│   ├── meters.yaml
│   ├── readings.yaml
│   ├── invoices.yaml
│   └── auth.yaml
├── components/
│   ├── schemas/
│   │   ├── Customer.yaml
│   │   ├── Meter.yaml
│   │   ├── Reading.yaml
│   │   ├── Invoice.yaml
│   │   ├── ProblemDetail.yaml      # RFC 7807
│   │   └── PageMetadata.yaml
│   ├── parameters/
│   │   ├── PageNumber.yaml
│   │   └── PageSize.yaml
│   ├── responses/
│   │   ├── BadRequest.yaml
│   │   ├── Unauthorized.yaml
│   │   ├── NotFound.yaml
│   │   └── Conflict.yaml
│   └── securitySchemes/
│       └── BearerAuth.yaml
└── examples/
    ├── customer-create-request.json
    └── reading-record-request.json
```

**Por que arquivos separados:** um único `openapi.yaml` monolítico vira ingerenciável a partir de 5-6 endpoints. Splittar usando `$ref` é o padrão profissional e ainda exercita seu domínio sobre referências OpenAPI.

### Fluxo de trabalho por endpoint

Para cada endpoint novo, o ciclo é:

1. Escrever path + schemas no YAML.
2. Rodar `./gradlew openApiValidate` para garantir que a spec é válida.
3. Rodar `./gradlew openApiGenerate` para gerar interfaces e DTOs.
4. Escrever teste `@WebMvcTest` que falha (TDD de contrato).
5. Implementar controller que estende a interface gerada.
6. Implementar service e repository (com TDD).
7. Rodar `./gradlew test` — tudo verde.
8. Commit do YAML **e** do código no mesmo PR.

### Configuração-chave do generator

No `build.gradle.kts`:

```kotlin
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/api-spec/openapi.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("com.energytrack.api.generated")
    modelPackage.set("com.energytrack.api.generated.model")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",          // gera só interfaces, não controllers
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useTags" to "true",
        "skipDefaultInterface" to "true",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "useBeanValidation" to "true"
    ))
}
```

A flag `interfaceOnly = true` é crucial: o generator entrega **interfaces**, e eu implemento o controller à mão. Memória muscular preservada — o que é gerado é só o "shape" do contrato.

### Versionamento da spec

- Commits da spec separados de commits de implementação quando possível (ajuda em code review).
- Mudanças breaking na spec exigem bump de major na URL (`/api/v1` → `/api/v2`).
- O CI verifica diff de breaking changes com `openapi-diff`.

---

## 5. Arquitetura: Visão por Módulos

A modularização segue Spring Modulith. Cada módulo é um pacote de primeiro nível abaixo de `com.energytrack`, com sua própria API pública e internals.

```
com.energytrack
├── EnergyTrackApplication.java
├── customer/        # Clientes industriais e seus medidores
│   ├── package-info.java          (@ApplicationModule)
│   ├── api/                       (interfaces públicas + DTOs expostos)
│   └── internal/                  (entidades JPA, repos, services, controllers)
├── metering/        # Leituras de consumo
│   ├── package-info.java
│   ├── api/                       (eventos: ReadingRecorded)
│   └── internal/
├── billing/         # Faturamento (Spring Batch)
│   ├── package-info.java
│   ├── api/
│   └── internal/
├── alerting/        # Alertas de threshold
│   ├── package-info.java
│   ├── api/
│   └── internal/
└── iam/             # Autenticação e autorização
    ├── package-info.java
    ├── api/
    └── internal/
```

**Comunicação entre módulos:** via **eventos de domínio** (`@ApplicationModuleListener`) sempre que possível. Acesso direto a outro módulo só através da sua API pública.

**Eventos principais:**

- `ReadingRecorded` (emitido por `metering`, consumido por `billing` e `alerting`)
- `InvoiceGenerated` (emitido por `billing`, pode ser consumido por futuros módulos de notificação)
- `ThresholdExceeded` (emitido por `alerting`)

**Verificação:** o teste `ApplicationModules.of(EnergyTrackApplication.class).verify()` roda no CI e quebra o build se as fronteiras forem violadas.

---

## 6. Plano em Fases

### Fase 0 — Setup, Fundamentos e OpenAPI (2-3 dias)

**Objetivo:** Subir o projeto do zero, com pipeline OpenAPI funcionando antes de escrever o primeiro endpoint.

Tarefas:

- [x] Criar repositório no GitHub com `.gitignore` apropriado para Gradle/Java/IntelliJ.
- [x] Escrever `settings.gradle.kts` à mão.
- [x] Escrever `build.gradle.kts` com Kotlin DSL, à mão.
- [x] Configurar `gradle/libs.versions.toml` (version catalog).
- [x] Adicionar Spring Boot, Modulith, JPA, Web, Validation, Actuator. *(Security adiada para Fase 3 — declarar a dep agora bloqueia `/health` e `/swagger-ui` por default.)*
- [x] Adicionar `openapi-generator-gradle-plugin` e validador da spec.
- [x] Criar estrutura `api-spec/` com `openapi.yaml` mínimo (info + um path `/health`).
- [x] Configurar tasks Gradle: `openApiValidate`, `openApiGenerate`.
- [x] Garantir que `compileJava` depende de `openApiGenerate`.
- [ ] Configurar `application.yml` com profiles `dev`, `test`, `prod`.
- [ ] Subir PostgreSQL via `docker-compose.yml` (com pgAdmin opcional).
- [ ] Configurar Flyway com migration `V001__init.sql` vazia.
- [x] Criar classe principal `EnergyTrackApplication`. *(Gerada pelo Spring Initializr como `EnergyTrackerApplication` em `com.massari.energytracker`.)*
- [ ] Implementar `/health` próprio usando a interface gerada da spec.
- [ ] Configurar Swagger UI servindo a spec em `/swagger-ui.html` (springdoc só para servir a UI — sem usar para gerar a spec). *(Dep `springdoc-openapi-starter-webmvc-ui` já no classpath; falta apontar para spec estática.)*
- [ ] Validar: `./gradlew bootRun` sobe a aplicação, conecta no Postgres, `/swagger-ui.html` mostra a spec.

**Conceitos a revisar:** lifecycle do Gradle, autoconfiguração do Spring Boot, profiles, `DataSource`, Flyway baseline, OpenAPI 3.1 estrutura básica, `$ref`.

**Spike opcional antes de começar:** clonar `spring-modulith-examples`, rodar um exemplo, quebrar de propósito uma fronteira de módulo e observar o teste falhar.

---

### Fase 1 — Domínio e Persistência com TDD (3-4 dias)

**Objetivo:** Modelar entidades de negócio escrevendo testes primeiro. Esta fase é puramente backend, ainda sem expor endpoints.

Entidades e regras:

- **Customer**: razão social, CNPJ/SIRET, endereço, status (ACTIVE, SUSPENDED).
- **Meter**: identificador, tipo (kWh/MWh), customer dono, data de instalação.
- **Reading**: meter, valor, timestamp. Regra: nunca pode ser menor que a leitura anterior do mesmo medidor.
- **Invoice**: período, customer, total consumido, valor, status (DRAFT, ISSUED, PAID).

Tarefas:

- [ ] Para cada entidade: escrever teste de regra de negócio → fazer passar → refatorar.
- [ ] Repositories com Spring Data JPA.
- [ ] Migrations Flyway por entidade (`V002__customer.sql`, `V003__meter.sql`, etc.).
- [ ] Testes de integração com `@DataJpaTest` + Testcontainers (PostgreSQL real, **não H2**).
- [ ] Configurar `TestcontainersConfiguration` reutilizável.

**Conceitos a revisar:** JPA (`@Entity`, `@OneToMany`, `FetchType`), `@Transactional`, equals/hashCode em entidades, value objects, `@Embeddable`.

---

### Fase 1.5 — Modulith e Eventos de Domínio (2 dias)

**Objetivo:** Estabelecer fronteiras dos módulos e comunicação por eventos.

Tarefas:

- [ ] Mover entidades para seus módulos respectivos (`customer/internal`, `metering/internal`, etc.).
- [ ] Criar `package-info.java` em cada módulo com `@ApplicationModule(displayName = ...)`.
- [ ] Definir o que é API pública de cada módulo (classes em `api/`) vs. interno.
- [ ] Escrever o teste `ApplicationModules.of(Application.class).verify()` — vai quebrar.
- [ ] Iterar até passar (este é o aprendizado central).
- [ ] Modelar o evento `ReadingRecorded` (record imutável) no módulo `metering`.
- [ ] Implementar listener em `billing` e `alerting` com `@ApplicationModuleListener`.
- [ ] Configurar Event Publication Registry persistido (JPA).
- [ ] Gerar documentação automática com `Documenter` (PlantUML/AsciiDoc).
- [ ] Versionar o output da documentação em `docs/architecture/`.

**Conceitos a revisar:** bounded contexts, Domain Events, eventual consistency, transactional outbox.

---

### Fase 2 — REST API com Spring MVC e Spec-First (4 dias)

**Objetivo:** Camada web inteira derivada do contrato OpenAPI.

Tarefas para **cada recurso** (Customer, Meter, Reading, Invoice):

- [ ] Escrever os schemas em `api-spec/components/schemas/`.
- [ ] Escrever os paths em `api-spec/paths/`.
- [ ] Definir respostas de erro reaproveitando `ProblemDetail` (RFC 7807).
- [ ] Adicionar exemplos de requisição em `api-spec/examples/`.
- [ ] Validar a spec com `./gradlew openApiValidate`.
- [ ] Gerar código com `./gradlew openApiGenerate`.
- [ ] Escrever teste `@WebMvcTest` contra a interface gerada (TDD de contrato).
- [ ] Implementar o controller estendendo a interface gerada.
- [ ] Implementar mapeamento DTO ↔ Entidade (MapStruct opcional, mas excelente exercício).
- [ ] `@RestControllerAdvice` global devolvendo `ProblemDetail`.
- [ ] Paginação com `Pageable` e schema `Page<T>` na spec.
- [ ] Filtros via query params declarados na spec.

**Endpoints principais:**

```
POST   /api/v1/customers
GET    /api/v1/customers/{id}
GET    /api/v1/customers?page=0&size=20
POST   /api/v1/customers/{id}/meters
POST   /api/v1/meters/{id}/readings
GET    /api/v1/meters/{id}/readings?from=...&to=...
GET    /api/v1/customers/{id}/invoices
```

**Testes de contrato (importante):**

- [ ] Adicionar Schemathesis ou validador OpenAPI para garantir que requisições/respostas reais batem com a spec.
- [ ] Job no GitHub Actions que sobe a aplicação e bate todos os endpoints contra a spec.

**Conceitos a revisar:** content negotiation, `ResponseEntity` vs. retorno direto, idempotência em POST/PUT, design de erros REST, paginação em OpenAPI.

---

### Fase 3 — Spring Security com JWT (2-3 dias)

**Objetivo:** Autenticação e autorização modernas (sem `WebSecurityConfigurerAdapter`).

Tarefas:

- [ ] Adicionar `securitySchemes` na spec OpenAPI (Bearer JWT).
- [ ] Adicionar `security: [BearerAuth: []]` nos endpoints protegidos.
- [ ] Módulo `iam` com entidade `User` e `Role`.
- [ ] `SecurityFilterChain` configurado como bean.
- [ ] Endpoints `POST /api/v1/auth/login` e `POST /api/v1/auth/refresh` (definidos na spec primeiro).
- [ ] Filtro JWT customizado (extends `OncePerRequestFilter`).
- [ ] `@PreAuthorize` por role (ADMIN, OPERATOR, CLIENT).
- [ ] Autorização por ownership: cliente só vê suas próprias faturas.
- [ ] Testes com `@WithMockUser` e `SecurityMockMvcRequestPostProcessors.jwt()`.
- [ ] Senhas com BCrypt.

**Conceitos a revisar:** filter chain, `AuthenticationManager`, `UserDetailsService`, claims JWT, refresh tokens, CSRF (e por que desabilitar em APIs stateless), como descrever segurança em OpenAPI.

---

### Fase 4 — Spring Batch para Faturamento Mensal (3 dias)

**Objetivo:** Job batch real, ponto explicitamente citado na vaga.

Tarefas:

- [ ] Definir endpoint `POST /api/v1/admin/jobs/billing/run` na spec OpenAPI.
- [ ] Job `MonthlyBillingJob` no módulo `billing`.
- [ ] Steps:
  1. `readActiveMetersStep` — lê todos os medidores ativos.
  2. `calculateConsumptionStep` — calcula consumo do período.
  3. `generateInvoicesStep` — gera faturas em chunks.
  4. `publishEventsStep` — publica `InvoiceGenerated`.
- [ ] `ItemReader` paginado, `ItemProcessor`, `ItemWriter` com chunks de 100.
- [ ] Configuração de `JobRepository` no PostgreSQL.
- [ ] Endpoint protegido por ADMIN (validar via spec + Spring Security).
- [ ] Agendamento via `@Scheduled` (cron mensal) — opcional para o aprendizado.
- [ ] Testes com `JobLauncherTestUtils` e `JobRepositoryTestUtils`.
- [ ] Tratamento de skip/retry para itens com falha.

**Conceitos a revisar:** chunk-oriented processing, `JobParameters`, restart de jobs, idempotência em batch, `StepListener`.

---

### Fase 5 — Observabilidade e Qualidade (2 dias)

**Objetivo:** Tornar a aplicação production-grade.

Tarefas:

- [ ] Spring Boot Actuator (health, metrics, info, prometheus).
- [ ] Health check customizado para Postgres e para o último job batch.
- [ ] Logs estruturados em JSON (Logback + `logstash-logback-encoder`).
- [ ] Trace ID e Span ID nos logs (Micrometer Tracing).
- [ ] Métricas customizadas: `readings_processed_total`, `invoices_generated_total`.
- [ ] Cobertura de testes com JaCoCo (alvo: 80%+ no domínio).
- [ ] Análise estática: SpotBugs ou plugin Sonar local.
- [ ] Mutation testing com Pitest (opcional, mas excelente exercício).
- [ ] Lint da spec OpenAPI com Spectral (regras customizadas para padronização).

---

### Fase 6 — Docker, GitHub Actions e Bônus (2-3 dias)

**Objetivo:** Empacotar e automatizar.

Tarefas:

- [ ] `Dockerfile` multi-stage (build com Gradle + runtime com JRE slim).
- [ ] Otimização de cache: separar layer de dependências do layer de código.
- [ ] Alternativa moderna: testar `bootBuildImage` (Buildpacks).
- [ ] `docker-compose.yml` completo com app + Postgres + pgAdmin.
- [ ] `.github/workflows/ci.yml` com jobs:
  - `validate-spec`: roda `openApiValidate` e Spectral.
  - `build`: compila e gera código a partir da spec.
  - `test`: roda testes com Testcontainers.
  - `modulith-verify`: roda `ApplicationModules.verify()`.
  - `contract-test`: sobe app e valida contra a spec.
  - `package`: builda imagem Docker.
- [ ] Cache de Gradle no Actions via `gradle/actions/setup-gradle`.
- [ ] Publicar imagem no GitHub Container Registry (`ghcr.io`) em pushes na main.
- [ ] Branch protection + status checks obrigatórios na main.
- [ ] Dependabot configurado para Gradle e Actions.
- [ ] Templates de Issue e PR em `.github/`.
- [ ] Bônus: gerar cliente TypeScript da spec e publicar como package npm em GitHub Packages.

---

## 7. Cronograma Estimado

A 2h/dia em dias úteis:

| Fase | Duração | Acumulado |
|---|---|---|
| Fase 0 — Setup + OpenAPI | 2-3 dias | 3 dias |
| Fase 1 — Domínio TDD | 3-4 dias | 7 dias |
| Fase 1.5 — Modulith | 2 dias | 9 dias |
| Fase 2 — REST API spec-first | 4 dias | 13 dias |
| Fase 3 — Security | 2-3 dias | 16 dias |
| Fase 4 — Batch | 3 dias | 19 dias |
| Fase 5 — Observabilidade | 2 dias | 21 dias |
| Fase 6 — Docker/Actions | 2-3 dias | 24 dias |

**Total: 7 a 8 semanas** a 2h/dia. Reduz para 4-5 semanas com finais de semana incluídos.

A inclusão de OpenAPI spec-first adiciona ~1 dia em Fase 0 e ~1 dia em Fase 2, mas paga dividendos no resto do projeto: contratos claros, menos retrabalho, e habilidade altamente valorizada em DSIs.

---

## 8. Critérios de "Pronto" para o Projeto Inteiro

- [ ] Todos os endpoints existem primeiro em `api-spec/openapi.yaml`.
- [ ] `./gradlew openApiValidate` passa sem warnings.
- [ ] Spectral lint da spec passa sem erros.
- [ ] Todos os módulos passam em `ApplicationModules.verify()`.
- [ ] Cobertura JaCoCo ≥ 80% nas camadas de domínio.
- [ ] Testes de contrato verdes (implementação bate com a spec).
- [ ] Pipeline GitHub Actions verde em todos os jobs.
- [ ] Imagem Docker rodando em < 1GB.
- [ ] Job mensal de faturamento rodando end-to-end com dados de teste.
- [ ] Documentação Modulith gerada automaticamente.
- [ ] Swagger UI acessível e exibindo a spec corretamente.
- [ ] README com instruções de execução local em < 5 minutos.
- [ ] Eu consigo explicar **cada linha** de código que escrevi.

---

## 9. Recursos de Referência

**Documentação oficial:**
- Spring Boot — https://docs.spring.io/spring-boot/
- Spring Modulith — https://docs.spring.io/spring-modulith/reference/
- Spring Batch — https://docs.spring.io/spring-batch/reference/
- Spring Security — https://docs.spring.io/spring-security/reference/
- Gradle Kotlin DSL — https://docs.gradle.org/current/userguide/kotlin_dsl.html
- Testcontainers — https://java.testcontainers.org/
- OpenAPI 3.1 — https://spec.openapis.org/oas/v3.1.0
- OpenAPI Generator — https://openapi-generator.tech/docs/generators/spring
- GitHub Actions — https://docs.github.com/actions
- Spectral (lint OpenAPI) — https://docs.stoplight.io/docs/spectral

**Exemplos:**
- spring-modulith-examples no GitHub.
- Repos com `openapi-generator-gradle-plugin` + Spring Boot 3 (procurar no GitHub).

**Livros úteis para consulta:**
- *Spring in Action* (Craig Walls).
- *Domain-Driven Design Distilled* (Vaughn Vernon) — para a parte de bounded contexts.
- *Designing Web APIs* (Brenda Jin et al.) — para boas práticas OpenAPI.

---

## 10. Diário de Bordo

Manter um arquivo `JOURNAL.md` em paralelo, registrando ao final de cada sessão:

- O que fiz hoje.
- O que aprendi (em uma frase).
- O que travou e como resolvi.
- O que farei na próxima sessão.

Este diário é tão importante quanto o código — é o que consolida o aprendizado.

---

*Plano criado em maio de 2026. Para ser revisado a cada conclusão de fase.*
